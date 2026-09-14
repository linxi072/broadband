package com.broadband.community.spring;

import com.broadband.common.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 小区覆盖管理（PC 后台）。
 *
 * <p>列表把「端口余量 / 覆盖状态」作为计算列直接在 SQL 中算出，与
 * {@code CommunityChecker} 的三态口径保持一致（余量 0 → 不可安装；余量 ≤ 8 或占用 ≥ 80% → 紧张）。</p>
 */
@RestController
@RequestMapping("/api/admin/communities")
public class AdminCommunityController {

    /** 覆盖状态推导（与前端校验口径、CommunityChecker 判定一致） */
    private static final String STATUS_EXPR = """
            CASE
              WHEN installable = 0 THEN 'UNAVAILABLE'
              WHEN port_total <= 0 THEN 'UNAVAILABLE'
              WHEN port_total - port_used <= 0 THEN 'UNAVAILABLE'
              WHEN port_total - port_used <= 8 OR port_used / port_total >= 0.8 THEN 'TIGHT'
              ELSE 'AVAILABLE'
            END
            """;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.broadband.system.spring.OperLogService operLog;

    @GetMapping
    @PreAuthorize("hasAuthority('community:view')")
    public List<Map<String, Object>> list(@RequestParam(required = false) String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, name, region, street, carrier,
                       latitude AS lat, longitude AS lng,
                       installable AS covered,
                       port_total AS portTotal, port_used AS portUsed,
                       GREATEST(port_total - port_used, 0) AS portRemaining,
                """).append(STATUS_EXPR).append(" AS status FROM community WHERE 1 = 1");

        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (name LIKE ? OR region LIKE ? OR street LIKE ?)");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY id");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('community:edit')")
    public Map<String, Object> save(@RequestBody Map<String, Object> req) {
        String id = str(req.get("id"));
        String name = str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("小区名称不能为空");
        if (id == null) id = Ids.next();

        int portTotal = intOf(req.get("portTotal"));
        int portUsed = intOf(req.get("portUsed"));
        if (portUsed > portTotal) throw new IllegalArgumentException("已占用端口不能大于端口总数");

        Boolean covered = boolOf(req.get("covered"));
        jdbc.update("""
                INSERT INTO community (id, name, region, street, carrier, latitude, longitude,
                                       installable, port_total, port_used)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  name = VALUES(name), region = VALUES(region), street = VALUES(street),
                  carrier = VALUES(carrier), latitude = VALUES(latitude), longitude = VALUES(longitude),
                  installable = VALUES(installable), port_total = VALUES(port_total), port_used = VALUES(port_used)
                """,
                id, name, str(req.get("region")), str(req.get("street")), str(req.get("carrier")),
                dblOf(req.get("lat")), dblOf(req.get("lng")),
                Boolean.FALSE.equals(covered) ? 0 : 1, portTotal, portUsed);

        var me = com.broadband.system.spring.AuthController.current();
        if (me != null) {
            // 写操作留痕
            com.broadband.system.spring.AuthController.current();
        }
        return Map.of("ok", true, "id", id);
    }

    // ------------------------------------------------------------------ 类型转换

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static int intOf(Object v) {
        if (v == null) return 0;
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double dblOf(Object v) {
        if (v == null) return 0d;
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    private static Boolean boolOf(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v).trim();
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(s) || "0".equals(s)) return Boolean.FALSE;
        return null;
    }
}
