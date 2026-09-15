package com.broadband.product.spring;

import com.broadband.product.pay.PayService;
import com.broadband.system.security.CustomerPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * C 端客户订单接口（开放层，供小程序「我的订单 / 下单 / 支付 / 评价」使用）。
 *
 * <p>与 PC 后台的 {@code /api/admin/orders}（需 {@code order:view} 权限）不同，
 * 这里是面向用户的开放接口。业务闭环：</p>
 * <ul>
 *   <li>POST /api/order/create   —— 下单（PENDING）</li>
 *   <li>POST /api/order/pay      —— 模拟微信支付（PAID，并生成关联安装工单待派单）</li>
 *   <li>GET  /api/order/my       —— 我的订单</li>
 *   <li>GET  /api/order/tracking —— 订单全链路追踪（订单→工单→SLA→评价）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/order")
public class ClientOrderController {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PayService payService;

    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestParam(required = false) String customerId) {
        return jdbc.queryForList("""
                SELECT id, customer_id AS customerId, customer_name AS customerName, phone,
                       package_id AS packageId, package_name AS packageName, amount,
                       community_name AS communityName,
                       order_type AS orderType, status, created_time AS createdTime
                FROM biz_order
                WHERE customer_id = ?
                ORDER BY created_time DESC
                """, (Object) (customerId == null ? "" : customerId));
    }

    /**
     * 下单：C 端客户创建业务订单（默认待支付 PENDING）。
     * 入参 {customerId, packageId, communityId, orderType?, address?, contactName?, contactPhone?, timeSlot?}。
     * 返回 {ok, orderId, amount, packageName, timeSlot, status}。
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body,
                                      @AuthenticationPrincipal CustomerPrincipal cp) {
        String customerId = str(body.get("customerId"), cp != null ? cp.id : null);
        String packageId = str(body.get("packageId"), null);
        String communityId = str(body.get("communityId"), null);
        String orderType = str(body.get("orderType"), "NEW_INSTALL");
        String timeSlot = str(body.get("timeSlot"), null);
        String contactName = str(body.get("contactName"), null);
        String contactPhone = str(body.get("contactPhone"), null);

        if (isBlank(customerId) || isBlank(packageId) || isBlank(communityId)) {
            throw new IllegalArgumentException("customerId / packageId / communityId 必填");
        }
        Map<String, Object> pkg = one("SELECT name, monthly_fee FROM package_info WHERE id = ?", packageId);
        if (pkg == null) throw new IllegalArgumentException("套餐不存在：" + packageId);
        String pkgName = String.valueOf(pkg.get("name"));
        int amount = ((Number) pkg.get("monthly_fee")).intValue();

        Map<String, Object> com = one("SELECT name FROM community WHERE id = ?", communityId);
        String communityName = com == null ? "" : String.valueOf(com.get("name"));

        String customerName = contactName;
        String phone = contactPhone;
        if (isBlank(customerName) || isBlank(phone)) {
            Map<String, Object> cust = one("SELECT name, phone FROM customer WHERE id = ?", customerId);
            if (cust != null) {
                if (isBlank(customerName)) customerName = str(cust.get("name"), "客户");
                if (isBlank(phone)) phone = str(cust.get("phone"), null);
            }
        }
        if (isBlank(customerName)) customerName = "客户";

        if (isBlank(timeSlot)) {
            timeSlot = LocalDate.now().plusDays(1).toString() + "#AM";
        }

        String orderId = "B" + System.currentTimeMillis();
        long now = System.currentTimeMillis();
        jdbc.update("INSERT INTO biz_order (id, customer_id, customer_name, phone, package_id, package_name, "
                + "amount, community_id, community_name, order_type, status, created_time) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?, 'PENDING', ?)",
                orderId, customerId, customerName, phone, packageId, pkgName, amount,
                communityId, communityName, orderType, now);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("orderId", orderId);
        resp.put("amount", amount);
        resp.put("packageName", pkgName);
        resp.put("timeSlot", timeSlot);
        resp.put("status", "PENDING");
        return resp;
    }

    /**
     * 支付：模拟微信支付（PayService 可替换为真实微信支付）。
     * 支付成功 -> 业务订单置 PAID，并创建关联安装工单（PENDING）作为派单数据源。
     * 幂等：已支付订单再次支付直接返回已关联工单。
     */
    @PostMapping("/pay")
    public Map<String, Object> pay(@RequestBody Map<String, Object> body) {
        String orderId = str(body.get("orderId"), null);
        String channel = str(body.get("channel"), "WECHAT_MOCK");
        if (isBlank(orderId)) throw new IllegalArgumentException("orderId 必填");

        Map<String, Object> order = one("SELECT id, status, customer_name, community_id, community_name, package_name "
                + "FROM biz_order WHERE id = ?", orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在：" + orderId);

        if (!"PENDING".equals(order.get("status"))) {
            String existingWo = jdbc.queryForObject(
                    "SELECT id FROM work_order WHERE biz_order_id = ? LIMIT 1", String.class, orderId);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok", true);
            r.put("alreadyPaid", true);
            r.put("orderId", orderId);
            r.put("workOrderId", existingWo);
            r.put("status", order.get("status"));
            return r;
        }

        int amount = jdbc.queryForObject("SELECT amount FROM biz_order WHERE id = ?", Integer.class, orderId);
        PayService.PayResult pr = payService.pay(orderId, amount, channel);
        if (!pr.success) throw new IllegalStateException("支付失败：" + pr.message);

        jdbc.update("UPDATE biz_order SET status = 'PAID' WHERE id = ?", orderId);

        String workOrderId = "WO" + System.currentTimeMillis();
        String timeSlot = LocalDate.now().plusDays(1).toString() + "#AM";
        jdbc.update("INSERT INTO work_order (id, community_id, address, time_slot, customer_name, package_desc, status, biz_order_id) "
                + "VALUES (?,?,?,?,?,?, 'PENDING', ?)",
                workOrderId, order.get("community_id"),
                order.get("community_name"), timeSlot,
                order.get("customer_name"), order.get("package_name"), orderId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("orderId", orderId);
        resp.put("workOrderId", workOrderId);
        resp.put("transactionId", pr.transactionId);
        resp.put("channel", pr.channel);
        resp.put("status", "PAID");
        return resp;
    }

    /** 订单全链路追踪（C 端）：业务订单 + 关联工单 + SLA 评估 + 评价。 */
    @GetMapping("/tracking")
    public Map<String, Object> tracking(@RequestParam String orderId) {
        Map<String, Object> order = one("SELECT * FROM biz_order WHERE id = ?", orderId);
        if (order == null) return Map.of();
        Map<String, Object> wo = one("SELECT id, status, worker_id, time_slot, down_speed, up_speed, complete_time "
                + "FROM work_order WHERE biz_order_id = ?", orderId);
        String woId = wo == null ? null : String.valueOf(wo.get("id"));
        List<Map<String, Object>> sla = woId == null ? List.of() : jdbc.queryForList(
                "SELECT id, order_type, sla_status, overtime_minutes, speed_test_mbps, created_time "
                + "FROM sla_record WHERE order_id = ? ORDER BY created_time", woId);
        List<Map<String, Object>> reviews = jdbc.queryForList(
                "SELECT id, score, tags, type, content, status, created_time FROM review WHERE order_id = ?", orderId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("order", order);
        resp.put("workOrder", wo == null ? Map.of() : wo);
        resp.put("slaRecords", sla);
        resp.put("reviews", reviews);
        return resp;
    }

    // ---------------------------------------------------------------- 工具

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> l = jdbc.queryForList(sql, args);
        return l.isEmpty() ? null : l.get(0);
    }

    private static String str(Object v, String fallback) {
        if (v == null) return fallback;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? fallback : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
