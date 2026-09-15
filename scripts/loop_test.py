#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
宽带业务闭环联调脚本（M11）。
驱动完整链路：下单 → 支付(模拟微信) → 运营派单 → 师傅开工 → 师傅完工(自动SLA) → 评价。

三个场景覆盖 SLA 两类评估：
  A 正常新装  : down=650  -> 时限达标 + 速率达标，无赔付
  B 速率不达标: down=200  -> 时限达标 + 速率不达标(现金赔付)
  C 超时新装  : 受理时间回拨3天 + down=900 -> 时限超时(慢必赔) + 速率达标

运行前需后端已在 :8082 启动（DB broadband/broadband）。
"""
import json
import sys
import urllib.request
import urllib.error
import urllib.parse

BASE = "http://localhost:8082"
SLOT = "2026-09-16#AM"   # 与种子数据不同的时段，避免容量干扰

passed = 0
failed = 0


def call(method, path, token=None, body=None):
    url = BASE + path
    data = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            raw = r.read().decode("utf-8")
            return r.status, (json.loads(raw) if raw else {})
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}
    except Exception as e:  # noqa
        return 0, {"error": str(e)}


def check(label, cond, detail=""):
    global passed, failed
    if cond:
        passed += 1
        print(f"  [PASS] {label}")
    else:
        failed += 1
        print(f"  [FAIL] {label}  {detail}")


def login_customer(phone="13800000000"):
    s, j = call("POST", "/api/auth/miniapp-login", body={"phone": phone, "code": "1234"})
    assert s == 200 and j.get("token"), f"customer login failed: {s} {j}"
    return j["token"], j.get("customer", {}).get("name", "演示客户")


def login_worker(phone):
    s, j = call("POST", "/api/auth/worker-login", body={"phone": phone, "code": "1234"})
    assert s == 200 and j.get("token"), f"worker login failed({phone}): {s} {j}"
    return j["token"]


def login_admin():
    s, j = call("POST", "/api/auth/login", body={"username": "admin", "password": "admin123"})
    assert s == 200 and j.get("token"), f"admin login failed: {s} {j}"
    return j["token"]


def backdate_order(order_id):
    """把业务订单受理时间回拨 3 天，并把工单预约时段设为已过期，模拟「超时未完成」（仅测试用，直连 DB）。"""
    import subprocess
    sql = (
        f"UPDATE biz_order SET created_time = UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 3 DAY))*1000 "
        f"WHERE id = '{order_id}'; "
        f"UPDATE work_order SET time_slot = '2026-09-12#AM' WHERE biz_order_id = '{order_id}'"
    )
    cmd = ["/usr/local/mysql/bin/mysql", "-ubroadband", "-pbroadband", "broadband", "-e", sql]
    subprocess.run(cmd, check=True, capture_output=True)


def main():
    print("=== 宽带业务闭环联调 (M11) ===")
    ctok, cname = login_customer()
    atok = login_admin()
    print(f"  customer={cname}  admin_token_ok={bool(atok)}")

    cases = [
        ("A 正常新装",  650, None),     # down, backdate?
        ("B 速率不达标", 200, False),
        ("C 超时新装",  900, True),
    ]

    orders = []
    print("\n[1] 下单 + 支付")
    for name, down, _ in cases:
        s, j = call("POST", "/api/order/create", token=ctok, body={
            "customerId": "demo", "packageId": "pkg500",
            "communityId": "com_ns01", "orderType": "NEW_INSTALL",
            "timeSlot": SLOT, "contactName": cname})
        check(f"{name}: 下单 200", s == 200 and j.get("ok"), f"{s} {j}")
        oid = j.get("orderId")
        s2, j2 = call("POST", "/api/order/pay", token=ctok, body={"orderId": oid})
        check(f"{name}: 支付 200 + 生成工单", s2 == 200 and j2.get("workOrderId"), f"{s2} {j2}")
        orders.append({"name": name, "down": down, "orderId": oid,
                       "workOrderId": j2.get("workOrderId"), "status": j2.get("status")})

    print("\n[2] 运营派单 (一次性派全部 PENDING)")
    s, j = call("POST", "/api/dispatch/run", token=atok)
    check("派单接口 200", s == 200, f"{s} {j}")

    print("\n[3] 逐单：读取指派师傅 → 开工 → 完工(自动SLA) → 评价")
    for o in orders:
        # 读取工单指派的师傅
        s, j = call("GET", "/api/worker/work-orders", token=atok)
        wo = next((x for x in j if x.get("id") == o["workOrderId"]), None)
        check(f"{o['name']}: 派单后已指派师傅", wo is not None and wo.get("workerId"),
              f"wo={wo}")
        worker_id = wo.get("workerId") if wo else None
        # 回拨受理时间（场景 C）
        if o["name"].startswith("C"):
            backdate_order(o["orderId"])
        # 用被指派的师傅登录
        phone = {"w01": "13700000001", "w02": "13700000002", "w03": "13700000003"}.get(worker_id)
        wtok = login_worker(phone) if phone else None
        check(f"{o['name']}: 师傅令牌({worker_id})", wtok is not None, f"phone={phone}")
        # 开工
        s, j = call("POST", f"/api/worker/work-orders/{o['workOrderId']}/start", token=wtok)
        check(f"{o['name']}: 开工->INSTALLING", s == 200 and j.get("status") == "INSTALLING", f"{s} {j}")
        # 完工（触发 SLA）
        s, j = call("POST", f"/api/worker/work-orders/{o['workOrderId']}/complete", token=wtok,
                    body={"down": o["down"], "up": 30, "sign": "客户签名", "services": "1,2,3,4,5,6"})
        check(f"{o['name']}: 完工->DONE", s == 200 and j.get("status") == "DONE", f"{s} {j}")

        # 追踪校验
        s, t = call("GET", f"/api/order/tracking?orderId={o['orderId']}", token=ctok)
        bo = t.get("order", {})
        wo2 = t.get("workOrder", {})
        slas = t.get("slaRecords", [])
        check(f"{o['name']}: 业务订单 DONE", bo.get("status") == "DONE", f"{bo}")
        check(f"{o['name']}: 工单 DONE", wo2.get("status") == "DONE", f"{wo2}")
        check(f"{o['name']}: 生成 SLA 记录(时限+速率=2)", len(slas) == 2, f"slas={slas}")
        o["slas"] = slas

    print("\n[4] SLA 结果断言")
    # A: 两条都 MET
    a = next(o for o in orders if o["name"].startswith("A"))
    a_status = {x.get("order_type") or "": x.get("sla_status") for x in a["slas"]}
    check("A 时限达标", "MET" in a_status.values(), f"{a_status}")
    check("A 速率达标", "MET" in a_status.values(), f"{a_status}")
    # B: 速率 OVERTIME（有赔付）
    b = next(o for o in orders if o["name"].startswith("B"))
    b_any_over = any(x.get("sla_status") == "OVERTIME" for x in b["slas"])
    check("B 速率不达标触发赔付", b_any_over, f"{b['slas']}")
    # C: 时限 OVERTIME（慢必赔）
    c = next(o for o in orders if o["name"].startswith("C"))
    c_any_over = any(x.get("sla_status") == "OVERTIME" for x in c["slas"])
    check("C 超时触发慢必赔", c_any_over, f"{c['slas']}")

    print("\n[5] 赔付工单核对")
    s, comps = call("GET", "/api/sla/compensations", token=atok)
    comp_orders = {c.get("orderId") for c in (comps or [])}
    b_wo = next(o for o in orders if o["name"].startswith("B"))["workOrderId"]
    c_wo = next(o for o in orders if o["name"].startswith("C"))["workOrderId"]
    check("B 工单产生赔付", b_wo in comp_orders, f"comps={comps}")
    check("C 工单产生赔付", c_wo in comp_orders, f"comps={comps}")

    print("\n[6] 客户评价")
    a_oid = next(o for o in orders if o["name"].startswith("A"))["orderId"]
    s, j = call("POST", "/api/review/create", token=ctok, body={
        "orderId": a_oid, "customerName": cname, "workerName": "张伟",
        "score": 5, "tags": "准时,专业", "type": "REVIEW", "content": "装机很满意"})
    check("评价提交 200", s == 200 and j.get("ok"), f"{s} {j}")
    s, j = call("GET", "/api/review/my?customerName=" + urllib.parse.quote(cname), token=ctok)
    check("我的评价可见", s == 200 and any(r.get("orderId") == a_oid for r in (j or [])), f"{s} {j}")

    print(f"\n=== 结果: PASS={passed}  FAIL={failed} ===")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
