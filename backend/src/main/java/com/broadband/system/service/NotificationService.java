package com.broadband.system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 消息通知中心核心服务（v1.23 新模块 · PC 后台站内信）。
 *
 * <p>为各业务模块（订单/支付/合约/宽带暂停恢复/SLA 等）提供统一的站内通知能力：
 * 业务事件调用 {@link #notify} 写入通知，PC 后台通过 {@code NotificationController} 查阅/已读。</p>
 *
 * <p><b>可测试性</b>：去重键生成 {@link #buildDedupKey}、角色扇出 {@link #resolveTargetRoles}、
 * 模板渲染 {@link #render} 均为<b>纯函数（无 DB / 无 Spring 依赖）</b>，可由
 * {@code NotificationServiceTest} 在不启动 Spring 上下文的情况下直接单测；
 * DB 读写方法以 Mockito 注入的 {@link JdbcTemplate} 验证 SQL 与参数。</p>
 *
 * <p>注意：本文件所属后端模块在受限沙箱无法执行 Maven 构建，需在具备本地仓库的机器上
 * {@code mvn -o compile} / {@code mvn -o test} 验证通过后再合并。</p>
 */
@Service
public class NotificationService {

    /** 去重窗口：同一 (eventType, refType, refId) 在该时间窗内只推送一次。 */
    static final long DEDUP_WINDOW_MS = 24L * 3600 * 1000;

    @Autowired
    private JdbcTemplate jdbc;

    // ============================================================ 纯逻辑（可单测，无 DB 依赖）

    /**
     * 去重键：同一业务对象（eventType + refType + refId）在窗口内只推送一次。
     * 任一维度为空时以 {@code *} 占位，保证键格式稳定、可被 SQL 精确匹配。
     */
    public static String buildDedupKey(String eventType, String refType, String refId) {
        return (eventType == null ? "*" : eventType)
                + "|" + (refType == null ? "*" : refType)
                + "|" + (refId == null ? "*" : refId);
    }

    /**
     * 目标角色扇出：{@code ALL} / 空 / null → [ADMIN, OPERATOR]；否则保持单角色（转大写）。
     * 用于管理端主动推送时按角色生成独立通知行。
     */
    public static List<String> resolveTargetRoles(String targetRole) {
        if (targetRole == null || targetRole.isBlank() || "ALL".equalsIgnoreCase(targetRole)) {
            List<String> all = new ArrayList<>();
            all.add("ADMIN");
            all.add("OPERATOR");
            return all;
        }
        List<String> one = new ArrayList<>();
        one.add(targetRole.toUpperCase());
        return one;
    }

    /**
     * 模板渲染：将 {@code {key}} 占位符替换为 params 中对应值；缺失的占位符原样保留。
     * 纯字符串操作，便于针对「多语言/缺参」场景单测。
     */
    public static String render(String template, Map<String, Object> params) {
        if (template == null) {
            return "";
        }
        if (params == null || params.isEmpty()) {
            return template;
        }
        String out = template;
        for (Map.Entry<String, Object> e : params.entrySet()) {
            out = out.replace("{" + e.getKey() + "}",
                    e.getValue() == null ? "" : String.valueOf(e.getValue()));
        }
        return out;
    }

    // ============================================================ 业务写入

    /**
     * 产生一条通知（供业务模块调用）。
     *
     * <p>入参：eventType（事件类型）、title、content、refType（关联对象类型）、
     * refId（关联对象ID）、targetRole（目标角色）。</p>
     *
     * <p><b>去重</b>：若 {@link #buildDedupKey} 在 {@link #DEDUP_WINDOW_MS} 窗口内已存在相同键，
     * 视为重复推送，返回 {@code null} 且不写库。</p>
     *
     * @return 通知ID（去重跳过时返回 {@code null}）
     */
    public String notify(String eventType, String title, String content,
                         String refType, String refId, String targetRole) {
        String dedupKey = buildDedupKey(eventType, refType, refId);
        Long recent = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE dedup_key = ? AND created_time >= ?",
                Long.class, dedupKey, System.currentTimeMillis() - DEDUP_WINDOW_MS);
        if (recent != null && recent > 0) {
            return null; // 去重窗口内已推送，跳过
        }
        String id = "N" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
        jdbc.update(
                "INSERT INTO notification (id, event_type, title, content, ref_type, ref_id, "
                        + "target_role, dedup_key, is_read, created_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)",
                id, eventType, title, content, refType, refId,
                targetRole == null ? "ALL" : targetRole.toUpperCase(),
                dedupKey, System.currentTimeMillis());
        return id;
    }

    /**
     * 管理端主动推送（如合约到期批量提醒）：按 {@link #resolveTargetRoles} 扇出为各角色独立通知行。
     * 为避免去重互相抵消，refId 按角色后缀区分，使每角色行拥有独立 dedup_key。
     *
     * @return 各角色通知ID列表（去重跳过时为 {@code null} 元素）
     */
    public List<String> create(String title, String content, String eventType,
                               String refType, String refId, String targetRole) {
        List<String> ids = new ArrayList<>();
        for (String role : resolveTargetRoles(targetRole)) {
            ids.add(notify(eventType == null ? "ADMIN_PUSH" : eventType,
                    title, content, refType, refId + ":" + role, role));
        }
        return ids;
    }

    // ============================================================ 查阅 / 已读

    public Map<String, Object> list(int page, int size, boolean unreadOnly, String role) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (unreadOnly) {
            where.append(" AND is_read = 0 ");
        }
        if (isRoleScoped(role)) {
            where.append(" AND (target_role = ? OR target_role = 'ALL') ");
            args.add(role.toUpperCase());
        }
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM notification" + where,
                Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        pageArgs.add(safeSize);
        pageArgs.add((safePage - 1) * safeSize);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, event_type AS eventType, title, content, ref_type AS refType, ref_id AS refId, "
                        + "target_role AS targetRole, is_read AS isRead, created_time AS createdTime, read_time AS readTime "
                        + "FROM notification" + where + " ORDER BY created_time DESC LIMIT ? OFFSET ?",
                pageArgs.toArray());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", rows);
        resp.put("total", total);
        resp.put("page", safePage);
        resp.put("size", safeSize);
        resp.put("unread", unreadCount(role));
        return resp;
    }

    public int unreadCount(String role) {
        if (isRoleScoped(role)) {
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM notification WHERE is_read = 0 AND (target_role = ? OR target_role = 'ALL')",
                    Integer.class, role.toUpperCase());
        }
        return jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE is_read = 0", Integer.class);
    }

    public int markRead(List<String> ids, String role) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder(
                "UPDATE notification SET is_read = 1, read_time = ? WHERE is_read = 0 AND id IN (");
        List<Object> args = new ArrayList<>();
        args.add(System.currentTimeMillis());
        for (int i = 0; i < ids.size(); i++) {
            sql.append("?");
            if (i < ids.size() - 1) {
                sql.append(",");
            }
            args.add(ids.get(i));
        }
        sql.append(")");
        if (isRoleScoped(role)) {
            sql.append(" AND (target_role = ? OR target_role = 'ALL')");
            args.add(role.toUpperCase());
        }
        return jdbc.update(sql.toString(), args.toArray());
    }

    public int markAllRead(String role) {
        if (isRoleScoped(role)) {
            return jdbc.update(
                    "UPDATE notification SET is_read = 1, read_time = ? "
                            + "WHERE is_read = 0 AND (target_role = ? OR target_role = 'ALL')",
                    System.currentTimeMillis(), role.toUpperCase());
        }
        return jdbc.update("UPDATE notification SET is_read = 1, read_time = ? WHERE is_read = 0",
                System.currentTimeMillis());
    }

    /** 角色是否为「限定范围」：非空且非 ALL 时按角色过滤；否则展示全部。 */
    private static boolean isRoleScoped(String role) {
        return role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role);
    }
}
