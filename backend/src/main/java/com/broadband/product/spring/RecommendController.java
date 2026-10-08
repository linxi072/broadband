package com.broadband.product.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 高频入口智能推荐（v1.18 新模块 · US-3.2）。
 *
 * <p>基于 {@code user_behavior_event} 行为底座，按入口触达频次聚合出「高频入口 Top 榜」，
 * 供运营在 PC 后台查看哪些功能最受客户关注，用于首页快捷入口编排与精准营销位投放。</p>
 *
 * <p>权限 {@code recommend:view}（M220/M221 菜单授权 ADMIN/OPERATOR）。</p>
 *
 * <p>注意：本文件所属后端模块在受限沙箱无法执行 Maven 构建，需在具备网络/本地仓库的机器上
 * {@code mvn -o compile} 验证编译通过后再合并。</p>
 */
@RestController
@RequestMapping("/api/admin/recommend")
public class RecommendController {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 高频入口 Top 榜：按入口聚合触达次数与去重用户数，倒序返回。
     * scope=global 含全部角色；scope=customer 仅统计 C 端客户行为。
     */
    @GetMapping("/shortcuts")
    @PreAuthorize("hasAuthority('recommend:view')")
    public Map<String, Object> shortcuts(
            @RequestParam(defaultValue = "global") String scope,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "30") int days) {

        long since = System.currentTimeMillis() - (long) days * 24 * 3600 * 1000L;

        StringBuilder where = new StringBuilder(" WHERE created_time >= ? ");
        List<Object> args = new ArrayList<>();
        args.add(since);
        if ("customer".equalsIgnoreCase(scope)) {
            where.append(" AND user_type = 'CUSTOMER' ");
        }
        if (eventType != null && !eventType.isBlank()) {
            where.append(" AND event_type = ? ");
            args.add(eventType);
        }

        int safeLimit = Math.min(Math.max(1, limit), 50);
        String sql = """
                SELECT entry, entry_title AS entryTitle,
                       COUNT(*) AS hit,
                       COUNT(DISTINCT user_id) AS users
                FROM user_behavior_event
                """ + where + """
                GROUP BY entry, entry_title
                ORDER BY hit DESC
                LIMIT ?
                """;
        List<Object> qArgs = new ArrayList<>(args);
        qArgs.add(safeLimit);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, qArgs.toArray());

        // 附加排名
        int rank = 1;
        for (Map<String, Object> r : rows) {
            r.put("rank", rank++);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("scope", scope);
        resp.put("days", days);
        resp.put("list", rows);
        resp.put("total", rows.size());
        return resp;
    }

    /**
     * 行为趋势：按天聚合事件量，用于观察埋点采集量与活跃走势。
     */
    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('recommend:view')")
    public Map<String, Object> trend(@RequestParam(defaultValue = "7") int days) {
        long since = System.currentTimeMillis() - (long) days * 24 * 3600 * 1000L;
        String sql = """
                SELECT DATE(FROM_UNIXTIME(created_time / 1000)) AS day,
                       COUNT(*) AS cnt,
                       COUNT(DISTINCT user_id) AS users
                FROM user_behavior_event
                WHERE created_time >= ?
                GROUP BY DATE(FROM_UNIXTIME(created_time / 1000))
                ORDER BY day
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, since);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("days", days);
        resp.put("list", rows);
        return resp;
    }
}
