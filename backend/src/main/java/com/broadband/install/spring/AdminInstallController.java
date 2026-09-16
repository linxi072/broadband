package com.broadband.install.spring;

import com.broadband.system.spring.AuthController;
import com.broadband.system.spring.OperLogService;
import com.broadband.system.service.SysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 安装工单与师傅容量管理（PC 后台）。
 *
 * <ul>
 *   <li>GET  /api/admin/work-orders        —— 工单池（联表解析小区名 / 师傅名）</li>
 *   <li>POST /api/admin/work-orders/reset  —— 重置为待派单（重复演示派单前使用）</li>
 *   <li>GET  /api/admin/workers            —— 师傅列表（技能等级映射为 初/中/高级）</li>
 *   <li>POST /api/admin/worker-capacity    —— 按师傅 × 时段批量设置容量</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminInstallController {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private OperLogService operLog;
    @Autowired private SysDepartmentService departmentService;

    // ==================================================================== 工单池

    @GetMapping("/work-orders")
    @PreAuthorize("hasAuthority('workorder:view')")
    public List<Map<String, Object>> workOrders(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT w.id,
                       w.customer_name AS customer,
                       w.package_desc  AS pkgDesc,
                       w.type          AS type,
                       c.name          AS community,
                       w.address, w.time_slot AS timeSlot,
                       COALESCE(k.name, '—') AS worker,
                       w.status, w.cluster_id AS clusterId,
                       w.adjacent_route AS adjacent
                FROM work_order w
                LEFT JOIN community c ON c.id = w.community_id
                LEFT JOIN worker    k ON k.id = w.worker_id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();

        // 数据权限：运营人员仅看本部门及下级部门工单；管理员（deptId 为空）看全部
        List<String> scope = dataScopeDeptIds();
        if (scope != null) {
            sql.append(" AND w.dept_id IN (").append(placeholders(scope)).append(")");
            args.addAll(scope);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND w.status = ?");
            args.add(status);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (w.id LIKE ? OR c.name LIKE ? OR w.customer_name LIKE ?)");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY w.time_slot, w.id");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    /** 当前登录用户的数据权限部门集合；null 表示不限定（看全部）。 */
    private List<String> dataScopeDeptIds() {
        var me = AuthController.current();
        if (me == null || me.user.deptId == null || me.user.deptId.isEmpty()) return null;
        return departmentService.visibleDeptIds(me.user.deptId);
    }

    private static String placeholders(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    /** 单条工单详情（师傅端工单详情页用）。 */
    @GetMapping("/work-orders/{id}")
    @PreAuthorize("hasAuthority('workorder:view')")
    public Map<String, Object> workOrder(@PathVariable String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT w.id, w.customer_name AS customer, w.package_desc AS pkgDesc,
                       c.name AS community, w.address, w.time_slot AS timeSlot,
                       COALESCE(k.name, '—') AS worker, w.status, w.cluster_id AS clusterId,
                       w.adjacent_route AS adjacent
                FROM work_order w
                LEFT JOIN community c ON c.id = w.community_id
                LEFT JOIN worker k ON k.id = w.worker_id
                WHERE w.id = ?
                """, id);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    /**
     * 重置为待派单：清空派单回填字段。
     * 幂等演示用 —— 派单算法只处理 PENDING 工单，重复演示前先重置。
     */
    @PostMapping("/work-orders/reset")
    @PreAuthorize("hasAuthority('dispatch:run')")
    public Map<String, Object> reset() {
        int n = jdbc.update("""
                UPDATE work_order
                SET status = 'PENDING', worker_id = NULL, cluster_id = NULL, adjacent_route = NULL
                WHERE status IN ('ASSIGNED', 'INSTALLING')
                """);
        log("重置工单为待派单", "/api/admin/work-orders/reset", "POST");
        return Map.of("ok", true, "reset", n);
    }

    // ==================================================================== 师傅

    @GetMapping("/workers")
    @PreAuthorize("hasAuthority('capacity:config')")
    public List<Map<String, Object>> workers() {
        return jdbc.queryForList("""
                SELECT id, name, region, skill_level AS skillLevel,
                       CASE skill_level WHEN 3 THEN '高级' WHEN 2 THEN '中级' ELSE '初级' END AS level
                FROM worker ORDER BY id
                """);
    }

    /**
     * 师傅容量批量设置。
     * body: { workerId, day: 'yyyy-MM-dd', slots: [{ slotKey: 'AM', adjacentCap: 4, nonAdjacentCap: 2, enabled: true }] }
     *
     * <p>time_slot 记为 {@code day#slotKey}，与派单算法的时段标识一致；
     * enabled=false 表示删除配置、回落到全局默认容量（相邻 3-4 / 非相邻 1-2）。</p>
     */
    @PostMapping("/worker-capacity")
    @PreAuthorize("hasAuthority('capacity:config')")
    public Map<String, Object> saveCapacity(@RequestBody Map<String, Object> req) {
        String workerId = str(req.get("workerId"));
        String day = str(req.get("day"));
        if (workerId == null || day == null) throw new IllegalArgumentException("缺少 workerId 或 day");

        Object rawSlots = req.get("slots");
        if (!(rawSlots instanceof List<?> slots)) throw new IllegalArgumentException("缺少 slots");

        int saved = 0;
        int cleared = 0;
        for (Object o : slots) {
            if (!(o instanceof Map<?, ?> slot)) continue;
            String slotKey = str(slot.get("slotKey"));
            if (slotKey == null) continue;
            String timeSlot = day + "#" + slotKey;
            boolean enabled = !Boolean.FALSE.equals(slot.get("enabled"));

            if (!enabled) {
                cleared += jdbc.update("DELETE FROM worker_capacity WHERE worker_id = ? AND time_slot = ?",
                        workerId, timeSlot);
                continue;
            }
            int adj = intOf(slot.get("adjacentCap"), 4);
            int non = intOf(slot.get("nonAdjacentCap"), 2);
            jdbc.update("""
                    INSERT INTO worker_capacity (worker_id, time_slot, adjacent_cap, non_adjacent_cap)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE adjacent_cap = VALUES(adjacent_cap),
                                            non_adjacent_cap = VALUES(non_adjacent_cap)
                    """, workerId, timeSlot, adj, non);
            saved++;
        }

        log("设置师傅容量 " + workerId + " " + day + "（" + saved + " 段）",
                "/api/admin/worker-capacity", "POST");
        return Map.of("ok", true, "saved", saved, "cleared", cleared);
    }

    // ==================================================================== 工具

    private void log(String action, String target, String method) {
        var me = AuthController.current();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static int intOf(Object v, int def) {
        if (v == null) return def;
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 供其他 admin 控制器复用的空 Map（保持序列化键顺序）。 */
    static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
