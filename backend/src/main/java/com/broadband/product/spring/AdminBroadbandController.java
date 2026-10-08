package com.broadband.product.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import com.broadband.system.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
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
 * 宽带暂停/恢复管理（v1.17 新模块 · PC 后台状态机 + 审计）。
 *
 * <p>复用既有 {@code customer} 表的 {@code status} 列（枚举 {@code ACTIVE/SUSPENDED/CLOSED}），
 * 补齐此前缺失的「宽带服务暂停/恢复」管理能力——此前该字段存在但无对应接口与页面。</p>
 *
 * <p>状态机（严格单向，拒绝非法跃迁）：</p>
 * <ul>
 *   <li>{@code ACTIVE（在用）} --pause--> {@code SUSPENDED（已暂停）}</li>
 *   <li>{@code SUSPENDED（已暂停）} --resume--> {@code ACTIVE（在用）}</li>
 *   <li>{@code CLOSED（已销户）} 为终态，不可暂停/恢复。</li>
 * </ul>
 *
 * <p>每次暂停/恢复均写入 {@code broadband_pause_log} 审计表（记录客户、类型、原因、操作人、时间），
 * 用于后台追溯与客服对账，杜绝「无痕改状态」带来的纠纷风险。</p>
 *
 * <p>权限 {@code broadband:manage}（M210/M211 菜单授权 ADMIN/OPERATOR）。</p>
 *
 * <p>注意：本文件所属后端模块在受限沙箱无法执行 Maven 构建，需在具备网络/本地仓库的机器上
 * {@code mvn -o compile} 验证编译通过后再合并。</p>
 */
@RestController
@RequestMapping("/api/admin/broadband")
public class AdminBroadbandController {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private NotificationService notifications;

    // ============================================================ 宽带客户台账

    /**
     * 宽带客户台账：关联套餐 / 小区，支持状态、关键字筛选与分页。
     * status 取值 ACTIVE / SUSPENDED / CLOSED。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('broadband:manage')")
    public Map<String, Object> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String householdId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            where.append(" AND c.status = ? ");
            args.add(status);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (c.name LIKE ? OR c.phone LIKE ?) ");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
        }
        // 多住宅隔离：按住宅维度过滤客户台账（US-3.1）
        if (householdId != null && !householdId.isBlank()) {
            where.append(" AND c.household_id = ? ");
            args.add(householdId);
        }

        int total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer c" + where,
                Integer.class, args.toArray());

        String sql = """
                SELECT c.id, c.name AS customerName, c.phone, c.level,
                       c.package_id AS packageId, p.name AS packageName,
                       c.community_id AS communityId, com.name AS communityName,
                       c.address, c.status, c.created_time AS createdTime
                FROM customer c
                LEFT JOIN package_info p ON p.id = c.package_id
                LEFT JOIN community com ON com.id = c.community_id
                """ + where + """
                ORDER BY c.created_time DESC
                LIMIT ? OFFSET ?
                """;

        List<Object> pageArgs = new ArrayList<>(args);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        pageArgs.add(safeSize);
        pageArgs.add((safePage - 1) * safeSize);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, pageArgs.toArray());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", rows);
        resp.put("total", total);
        resp.put("page", safePage);
        resp.put("size", safeSize);
        return resp;
    }

    // ============================================================ 概览统计

    /**
     * 宽带服务概览：客户总数 / 在用 / 已暂停 / 已销户。
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('broadband:manage')")
    public Map<String, Object> stats() {
        return jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END), 0) AS active,
                       COALESCE(SUM(CASE WHEN status = 'SUSPENDED' THEN 1 ELSE 0 END), 0) AS suspended,
                       COALESCE(SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END), 0) AS closed
                FROM customer
                """);
    }

    // ============================================================ 暂停/恢复审计日志

    /**
     * 暂停/恢复记录：可按客户、类型筛选，按时间倒序。
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('broadband:manage')")
    public Map<String, Object> logs(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();

        if (customerId != null && !customerId.isBlank()) {
            where.append(" AND l.customer_id = ? ");
            args.add(customerId);
        }
        if (type != null && !type.isBlank()) {
            where.append(" AND l.type = ? ");
            args.add(type);
        }

        int total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM broadband_pause_log l" + where,
                Integer.class, args.toArray());

        String sql = """
                SELECT l.id, l.customer_id AS customerId, c.name AS customerName, c.phone,
                       l.type, l.reason, l.operator, l.created_time AS createdTime
                FROM broadband_pause_log l
                LEFT JOIN customer c ON c.id = l.customer_id
                """ + where + """
                ORDER BY l.created_time DESC
                LIMIT ? OFFSET ?
                """;

        List<Object> pageArgs = new ArrayList<>(args);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        pageArgs.add(safeSize);
        pageArgs.add((safePage - 1) * safeSize);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, pageArgs.toArray());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", rows);
        resp.put("total", total);
        resp.put("page", safePage);
        resp.put("size", safeSize);
        return resp;
    }

    // ============================================================ 状态机：暂停 / 恢复

    /**
     * 暂停：仅 ACTIVE → SUSPENDED 合法，写入 PAUSE 审计。
     * 入参 {customerId, reason}。
     */
    @PostMapping("/pause")
    @PreAuthorize("hasAuthority('broadband:manage')")
    public Map<String, Object> pause(@RequestBody Map<String, Object> body) {
        String customerId = str(body, "customerId");
        String reason = str(body, "reason");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId 必填");
        }
        String cur = jdbc.queryForObject("SELECT status FROM customer WHERE id = ?", String.class, customerId);
        if (cur == null) {
            throw new IllegalArgumentException("客户不存在：" + customerId);
        }
        if (!"ACTIVE".equals(cur)) {
            throw new IllegalStateException("仅「在用(ACTIVE)」客户可暂停，当前状态：" + cur);
        }
        jdbc.update("UPDATE customer SET status = 'SUSPENDED' WHERE id = ?", customerId);
        insertLog(customerId, "PAUSE", reason);
        emitNotification("BROADBAND_PAUSE", "宽带已暂停",
                "客户 " + customerId + " 的宽带服务已暂停", customerId);
        return ok(customerId, "SUSPENDED");
    }

    /**
     * 恢复：仅 SUSPENDED → ACTIVE 合法，写入 RESUME 审计。
     * 入参 {customerId, reason}。
     */
    @PostMapping("/resume")
    @PreAuthorize("hasAuthority('broadband:manage')")
    public Map<String, Object> resume(@RequestBody Map<String, Object> body) {
        String customerId = str(body, "customerId");
        String reason = str(body, "reason");
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId 必填");
        }
        String cur = jdbc.queryForObject("SELECT status FROM customer WHERE id = ?", String.class, customerId);
        if (cur == null) {
            throw new IllegalArgumentException("客户不存在：" + customerId);
        }
        if (!"SUSPENDED".equals(cur)) {
            throw new IllegalStateException("仅「已暂停(SUSPENDED)」客户可恢复，当前状态：" + cur);
        }
        jdbc.update("UPDATE customer SET status = 'ACTIVE' WHERE id = ?", customerId);
        insertLog(customerId, "RESUME", reason);
        emitNotification("BROADBAND_RESUME", "宽带已恢复",
                "客户 " + customerId + " 的宽带服务已恢复", customerId);
        return ok(customerId, "ACTIVE");
    }

    // ============================================================ 内部辅助

    private void insertLog(String customerId, String type, String reason) {
        String id = "BPL" + System.nanoTime();
        jdbc.update(
                "INSERT INTO broadband_pause_log (id, customer_id, type, reason, operator, created_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id, customerId, type, reason == null ? "" : reason, currentOperator(), System.currentTimeMillis());
    }

    /** 操作人取自当前登录主体；RBAC 未注入时回退 ADMIN（不影响状态机主流程） */
    private String currentOperator() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) {
            // 保留回退
        }
        return "ADMIN";
    }

    private static String str(Map<String, Object> body, String k) {
        Object v = body.get(k);
        return v == null ? null : String.valueOf(v);
    }

    /** 通知为旁路能力：失败不影响状态机主流程（暂停/恢复已落库）。 */
    private void emitNotification(String eventType, String title, String content, String customerId) {
        try {
            notifications.notify(eventType, title, content, "customer", customerId, "ALL");
        } catch (Exception ignored) {
            // 通知子系统异常不应阻断核心业务
        }
    }

    private static Map<String, Object> ok(String customerId, String status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("customerId", customerId);
        m.put("status", status);
        return m;
    }
}
