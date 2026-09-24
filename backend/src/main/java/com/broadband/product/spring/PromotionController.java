package com.broadband.product.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 优惠活动专区（C 端小程序，V1.14 运营留存）。
 *
 * <ul>
 *   <li>GET /api/promotions      —— 活动列表（仅上线的）</li>
 *   <li>GET /api/promotions/{id} —— 活动详情</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Autowired private JdbcTemplate jdbc;

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String type) {
        String sql = "SELECT id, title, subtitle, cover, type, target, start_date AS startDate, "
                + "end_date AS endDate, rule_json AS ruleJson, status FROM promotion WHERE status='ONLINE'";
        List<Object> args = new java.util.ArrayList<>();
        if (type != null && !type.isBlank()) {
            sql += " AND type = ?";
            args.add(type);
        }
        sql += " ORDER BY start_date DESC";
        return jdbc.queryForList(sql, args.toArray());
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, title, subtitle, cover, type, target, start_date AS startDate, "
                        + "end_date AS endDate, rule_json AS ruleJson, status FROM promotion WHERE id = ?", id);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }
}
