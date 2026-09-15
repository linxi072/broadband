package com.broadband.product.spring;

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
 * C 端评价 / 投诉接口（开放层，供小程序「订单评价」使用）。
 *
 * <ul>
 *   <li>POST /api/review/create —— 提交评价 / 投诉（关联 order_id）</li>
 *   <li>GET  /api/review/my     —— 按客户名查询自己的评价（小程序侧以登录客户名过滤）</li>
 * </ul>
 *
 * <p>后台管理端评价列表见 AdminProductController 的 {@code /api/admin/reviews}（需 review:view 权限）。</p>
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired private JdbcTemplate jdbc;

    /**
     * 提交评价 / 投诉。
     * 入参 {orderId, customerName?, workerName?, score, tags?, type?, content?}。
     * 返回 {ok, id, status}。
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        String orderId = str(body.get("orderId"), null);
        String customerName = str(body.get("customerName"), "匿名客户");
        String workerName = str(body.get("workerName"), null);
        Integer score = toInt(body.get("score"));
        String tags = str(body.get("tags"), null);
        String type = str(body.get("type"), "REVIEW");
        String content = str(body.get("content"), null);

        if (isBlank(orderId)) throw new IllegalArgumentException("orderId 必填");
        if (score == null || score < 0 || score > 5) throw new IllegalArgumentException("评分需在 0-5 之间");

        String id = "RV" + System.currentTimeMillis();
        jdbc.update("INSERT INTO review (id, order_id, customer_name, worker_name, score, tags, type, content, status, created_time) "
                + "VALUES (?,?,?,?,?,?,?,?, 'PENDING', ?)",
                id, orderId, customerName, workerName, score, tags, type, content, System.currentTimeMillis());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("status", "PENDING");
        return resp;
    }

    /** 我的评价（按客户名过滤）。 */
    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestParam(required = false) String customerName) {
        if (isBlank(customerName)) return List.of();
        return jdbc.queryForList("SELECT id, order_id AS orderId, customer_name AS customer, worker_name AS worker, "
                + "score, tags, type, content, status, created_time AS createdTime "
                + "FROM review WHERE customer_name = ? ORDER BY created_time DESC", customerName);
    }

    // ---------------------------------------------------------------- 工具

    private static String str(Object v, String fallback) {
        if (v == null) return fallback;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? fallback : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
