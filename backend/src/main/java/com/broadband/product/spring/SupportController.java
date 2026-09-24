package com.broadband.product.spring;

import com.broadband.common.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * 在线客服 / 帮助中心（C 端小程序，V1.14 运营留存）。
 *
 * <ul>
 *   <li>GET  /api/support/faq     —— FAQ 列表（可按 category 过滤）</li>
 *   <li>POST /api/support/ticket  —— 提交工单式咨询（与报修/订单工单可互链）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired private JdbcTemplate jdbc;

    @GetMapping("/faq")
    public List<Map<String, Object>> faq(@RequestParam(required = false) String category) {
        String sql = "SELECT id, category, question, answer FROM support_faq";
        List<Object> args = new java.util.ArrayList<>();
        if (category != null && !category.isBlank()) {
            sql += " WHERE category = ?";
            args.add(category);
        }
        sql += " ORDER BY sort_order";
        return jdbc.queryForList(sql, args.toArray());
    }

    /** 提交咨询工单：{customerId?, type?, content, contact?}。 */
    @PostMapping("/ticket")
    public Map<String, Object> ticket(@RequestBody Map<String, Object> body) {
        String content = str(body.get("content"));
        if (isBlank(content)) throw new IllegalArgumentException("咨询内容必填");
        String customerId = str(body.get("customerId"));
        String type = str(body.get("type"), "CONSULT");
        String contact = str(body.get("contact"));

        String customerName = "匿名";
        if (customerId != null) {
            Map<String, Object> c = one("SELECT name FROM customer WHERE id = ?", customerId);
            if (c != null) customerName = String.valueOf(c.get("name"));
        }

        String id = Ids.next();
        jdbc.update("INSERT INTO support_ticket (id, customer_id, customer_name, type, content, contact, status, created_time) "
                        + "VALUES (?,?,?,?,?,?, 'PENDING', ?)",
                id, customerId, customerName, type, content, contact, System.currentTimeMillis());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("ticketId", id);
        resp.put("status", "PENDING");
        return resp;
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> l = jdbc.queryForList(sql, args);
        return l.isEmpty() ? null : l.get(0);
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
