package com.broadband.product.spring;

import com.broadband.common.Ids;
import com.broadband.system.security.CustomerPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 故障报修（C 端小程序）。V1.13 全流程：客户提交报修 → 生成报修工单(REPAIR) → 派单 → 师傅上门 → 完工 → SLA 评估。
 *
 * <p>与「新装」不同，报修无需支付，提交即受理（biz_order 直接置 PAID 表示已受理），
 * 并同步生成 type=REPAIR 的安装工单进入派单池（被现有派单算法按小区相邻性统一调度）。</p>
 *
 * <ul>
 *   <li>POST /api/repair/create   —— 提交报修（生成 biz_order(REPAIR) + work_order(REPAIR)）</li>
 *   <li>GET  /api/repair/my       —— 我的报修列表（含进度）</li>
 *   <li>GET  /api/repair/{id}     —— 报修详情（工单 + 师傅 + SLA + 时间线）</li>
 *   <li>POST /api/repair/{id}/cancel —— 撤销报修（仅 PENDING 未派单可撤）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/repair")
public class RepairController {

    @Autowired private JdbcTemplate jdbc;

    private static final String REPAIR_ORDER_SELECT = """
            SELECT w.id,
                   w.type, w.fault_category AS faultCategory, w.fault_desc AS faultDesc,
                   w.contact_phone AS contactPhone,
                   w.customer_name AS customer, w.package_desc AS pkgDesc,
                   c.name AS community, w.address,
                   w.time_slot AS timeSlot, w.status,
                   COALESCE(k.name, '—') AS worker,
                   b.id AS orderId, b.status AS orderStatus, w.biz_order_id AS bizOrderId
            FROM work_order w
            LEFT JOIN community c ON c.id = w.community_id
            LEFT JOIN worker    k ON k.id = w.worker_id
            LEFT JOIN biz_order b ON b.id = w.biz_order_id
            """;

    /**
     * 提交报修。入参 {customerId, communityId?, faultCategory, faultDesc, contactPhone?, timeSlot?}。
     * 返回 {ok, repairId, orderId, status}。
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal CustomerPrincipal cp) {
        String customerId = resolveCustomerId(cp, str(body.get("customerId")));
        String faultCategory = str(body.get("faultCategory"));
        String faultDesc = str(body.get("faultDesc"));
        if (isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        if (isBlank(faultCategory)) throw new IllegalArgumentException("faultCategory 必填（故障类型）");
        if (isBlank(faultDesc)) throw new IllegalArgumentException("faultDesc 必填（故障描述）");

        Map<String, Object> cust = one("SELECT name, phone, community_id FROM customer WHERE id = ?", customerId);
        if (cust == null) throw new IllegalArgumentException("客户不存在：" + customerId);

        String communityId = str(body.get("communityId"));
        if (isBlank(communityId)) communityId = str(cust.get("community_id"));
        if (isBlank(communityId)) throw new IllegalArgumentException("无法确定报修小区（客户未绑定小区且未传 communityId）");

        Map<String, Object> com = one("SELECT name, dept_id FROM community WHERE id = ?", communityId);
        if (com == null) throw new IllegalArgumentException("小区不存在：" + communityId);
        String communityName = String.valueOf(com.get("name"));
        String deptId = str(com.get("dept_id"));

        String customerName = str(cust.get("name"), "客户");
        String contactPhone = str(body.get("contactPhone"), str(cust.get("phone")));

        // 故障类型字典标签（用于工单描述，便于师傅一眼看懂）
        String categoryLabel = faultCategory;
        Map<String, Object> dict = one(
                "SELECT dict_label FROM sys_dict_data WHERE dict_type='fault_category' AND dict_value=? LIMIT 1",
                faultCategory);
        if (dict != null) categoryLabel = String.valueOf(dict.get("dict_label"));

        // 预约时段：优先取客户端指定，否则默认次日 AM
        String timeSlot = str(body.get("timeSlot"));
        if (isBlank(timeSlot)) timeSlot = LocalDate.now().plusDays(1).toString() + "#AM";

        String orderId = "B" + System.currentTimeMillis();
        long now = System.currentTimeMillis();
        jdbc.update("INSERT INTO biz_order (id, customer_id, customer_name, phone, package_id, package_name, "
                + "amount, community_id, community_name, order_type, status, dept_id, created_time) "
                + "VALUES (?,?,?,?,?,?, 0, ?,?, 'REPAIR', 'PAID', ?, ?)",
                orderId, customerId, customerName, contactPhone, null, "故障报修",
                communityId, communityName, deptId, now);

        String workOrderId = "WO" + System.currentTimeMillis();
        jdbc.update("INSERT INTO work_order (id, community_id, address, time_slot, customer_name, package_desc, "
                + "status, type, fault_category, fault_desc, contact_phone, biz_order_id, dept_id) "
                + "VALUES (?,?,?,?,?,?, 'PENDING', 'REPAIR', ?,?,?,?, ?)",
                workOrderId, communityId, communityName, timeSlot, customerName,
                "故障报修-" + categoryLabel, faultCategory, faultDesc, contactPhone, orderId, deptId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("repairId", workOrderId);
        resp.put("orderId", orderId);
        resp.put("status", "PENDING");
        resp.put("categoryLabel", categoryLabel);
        return resp;
    }

    /** 我的报修列表（按客户查其 REPAIR 业务订单关联的工单）。 */
    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestParam String customerId) {
        if (isBlank(customerId)) return List.of();
        List<Map<String, Object>> rows = jdbc.queryForList(
                REPAIR_ORDER_SELECT + " WHERE b.customer_id = ? AND b.order_type = 'REPAIR' ORDER BY w.time_slot DESC, w.id DESC",
                customerId);
        return rows.stream().map(this::enrich).toList();
    }

    /** 报修详情（含 SLA 评估与进度时间线）。 */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        List<Map<String, Object>> rows = jdbc.queryForList(REPAIR_ORDER_SELECT + " WHERE w.id = ?", id);
        if (rows.isEmpty()) return Map.of();
        Map<String, Object> wo = enrich(rows.get(0));

        List<Map<String, Object>> sla = jdbc.queryForList(
                "SELECT id, order_type, sla_status, overtime_minutes, speed_test_mbps, created_time "
                + "FROM sla_record WHERE order_id = ? ORDER BY created_time", id);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("workOrder", wo);
        resp.put("slaRecords", sla);
        resp.put("timeline", buildTimeline(wo));
        return resp;
    }

    /** 撤销报修：仅 PENDING（未派单）可撤，同步置 biz_order 为 CANCELLED。 */
    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable String id) {
        Map<String, Object> wo = one("SELECT id, status, biz_order_id FROM work_order WHERE id = ?", id);
        if (wo == null) throw new IllegalArgumentException("报修工单不存在：" + id);
        if (!"PENDING".equals(wo.get("status"))) {
            throw new IllegalStateException("仅「待派单」状态可撤销，当前：" + wo.get("status"));
        }
        jdbc.update("UPDATE work_order SET status = 'CANCELLED' WHERE id = ?", id);
        String bizId = str(wo.get("biz_order_id"));
        if (bizId != null) jdbc.update("UPDATE biz_order SET status = 'CANCELLED' WHERE id = ?", bizId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("status", "CANCELLED");
        return resp;
    }

    // ---------------------------------------------------------------- 辅助

    private Map<String, Object> enrich(Map<String, Object> r) {
        Map<String, Object> m = new LinkedHashMap<>(r);
        m.put("statusText", statusText(String.valueOf(r.get("status"))));
        m.put("progress", progress(String.valueOf(r.get("status"))));
        m.put("timeSlotText", prettySlot(str(r.get("timeSlot"))));
        m.put("faultCategoryText", faultLabel(str(r.get("faultCategory"))));
        return m;
    }

    /** 故障类型值 -> 字典标签（如 NETWORK_DOWN -> 网络中断）。 */
    private String faultLabel(String value) {
        if (value == null) return "";
        Map<String, Object> d = one(
                "SELECT dict_label FROM sys_dict_data WHERE dict_type='fault_category' AND dict_value=? LIMIT 1",
                value);
        return d == null ? value : String.valueOf(d.get("dict_label"));
    }

    /** 进度步骤（用于小程序进度条）：0 待派单 / 1 已派单 / 2 维修中 / 3 已完成。 */
    private int progress(String status) {
        return switch (status) {
            case "PENDING" -> 0;
            case "ASSIGNED" -> 1;
            case "INSTALLING" -> 2;
            case "DONE" -> 3;
            case "CANCELLED" -> -1;
            default -> 0;
        };
    }

    private List<Map<String, Object>> buildTimeline(Map<String, Object> wo) {
        String status = String.valueOf(wo.get("status"));
        List<Map<String, Object>> steps = new java.util.ArrayList<>();
        steps.add(step("已提交报修", true, "客户提交故障报修申请"));
        steps.add(step("已派单", "PENDING".equals(status) ? false : true,
                "ASSIGNED".equals(status) || "INSTALLING".equals(status) || "DONE".equals(status)
                        ? "师傅已接单：" + wo.get("worker") : "等待调度派单"));
        steps.add(step("维修中", "INSTALLING".equals(status) || "DONE".equals(status),
                "INSTALLING".equals(status) ? "师傅正在上门维修" : ("DONE".equals(status) ? "维修已完成" : "尚未开始")));
        steps.add(step("已完成", "DONE".equals(status), "DONE".equals(status) ? "服务已完成，欢迎评价" : "待完成"));
        return steps;
    }

    private Map<String, Object> step(String title, boolean done, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("done", done);
        m.put("desc", desc);
        return m;
    }

    private static String statusText(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "PENDING" -> "待派单";
            case "ASSIGNED" -> "已派单";
            case "INSTALLING" -> "维修中";
            case "DONE" -> "已完成";
            case "CANCELLED" -> "已撤销";
            default -> status;
        };
    }

    private static String prettySlot(String slot) {
        if (slot == null) return "—";
        String[] p = slot.split("#");
        if (p.length < 2) return slot;
        String half = switch (p[1]) {
            case "AM" -> "上午";
            case "PM" -> "下午";
            default -> p[1];
        };
        return p[0] + " " + half;
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> l = jdbc.queryForList(sql, args);
        return l.isEmpty() ? null : l.get(0);
    }

    /**
     * 解析操作主体身份：小程序 token 落地（protect-client-api=true）后，优先采用认证主体，
     * 杜绝客户端伪造 customerId 的越权（IDOR）；开发态开放层未认证时回退请求参数。
     */
    private static String resolveCustomerId(CustomerPrincipal cp, String fallback) {
        if (cp != null && cp.id != null && !cp.id.isBlank()) return cp.id;
        return fallback;
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String str(Object v, String fallback) {
        String s = str(v);
        return s == null ? fallback : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
