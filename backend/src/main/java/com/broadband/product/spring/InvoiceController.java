package com.broadband.product.spring;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电子发票（占位）C 端接口。
 *
 * <ul>
 *   <li>POST /api/invoice/apply —— 申请开票（关联已完成订单，生成 PENDING 申请单）</li>
 *   <li>GET  /api/invoice/my     —— 按客户名查询自己的发票申请与状态</li>
 * </ul>
 *
 * <p>状态机：PENDING（申请） -> OPENED（已开具） / REJECTED（驳回），由后台
 * {@code /api/admin/invoices} 审批流转（见 AdminProductController）。</p>
 */
@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    @Autowired private JdbcTemplate jdbc;

    /**
     * 申请开票。入参 {orderId, title, taxNo?, customerName?}。
     * 仅「已完成 DONE」订单可开票；幂等：同订单已有 PENDING/OPENED 申请则直接返回。
     * 返回 {ok, invoiceId, orderId, amount, status}。
     */
    @PostMapping("/apply")
    public Map<String, Object> apply(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal CustomerPrincipal cp) {
        String orderId = str(body.get("orderId"), null);
        String title = str(body.get("title"), null);
        String taxNo = str(body.get("taxNo"), null);
        if (isBlank(orderId)) throw new IllegalArgumentException("orderId 必填");
        if (isBlank(title)) throw new IllegalArgumentException("发票抬头必填");

        Map<String, Object> order = one("SELECT id, status, customer_id, customer_name, amount "
                + "FROM biz_order WHERE id = ?", orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在：" + orderId);
        if (!"DONE".equals(order.get("status")))
            throw new IllegalStateException("仅「已完成」订单可开票，当前：" + order.get("status"));

        String existing = jdbc.queryForList(
                "SELECT id FROM invoice_apply WHERE order_id = ? AND status IN ('PENDING','OPENED') LIMIT 1",
                String.class, orderId).stream().findFirst().orElse(null);
        if (existing != null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok", true);
            r.put("alreadyApplied", true);
            r.put("invoiceId", existing);
            r.put("orderId", orderId);
            return r;
        }

        String invoiceId = "IN" + System.currentTimeMillis();
        int amount = ((Number) order.get("amount")).intValue();
        String cname = str(order.get("customer_name"), cp != null ? cp.name : "客户");
        jdbc.update("INSERT INTO invoice_apply (id, order_id, order_no, customer_id, customer_name, title, tax_no, amount, status, created_time) "
                + "VALUES (?,?,?,?,?,?,?,?, 'PENDING', ?)",
                invoiceId, orderId, orderId,
                str(order.get("customer_id"), cp != null ? cp.id : null), cname, title, taxNo, amount,
                System.currentTimeMillis());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("invoiceId", invoiceId);
        resp.put("orderId", orderId);
        resp.put("amount", amount);
        resp.put("status", "PENDING");
        return resp;
    }

    /** 我的发票申请（按客户名过滤）。 */
    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestParam(required = false) String customerName,
                                       @AuthenticationPrincipal CustomerPrincipal cp) {
        String name = str(customerName, cp != null ? cp.name : null);
        if (isBlank(name)) return List.of();
        return jdbc.queryForList("SELECT id, order_id AS orderId, title, tax_no AS taxNo, amount, status, "
                + "invoice_no AS invoiceNo, pdf_url AS pdfUrl, created_time AS createdTime, opened_time AS openedTime "
                + "FROM invoice_apply WHERE customer_name = ? ORDER BY created_time DESC", name);
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
