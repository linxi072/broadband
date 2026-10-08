package com.broadband.product.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.broadband.system.service.NotificationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 合约与续约管理（v1.16 新模块 · PC 后台读侧聚合）。
 *
 * <p>复用既有 {@code customer_contract} 表（原用于「套餐升级剩余月数 / 补差折算」与账户到期查询），
 * 补齐此前缺失的「合约台账 + 到期预警 + 续约办理」管理能力——此前该表有数据但无独立接口与页面。</p>
 *
 * <p>聚合口径说明（全部真实表查询，无臆造数据）：</p>
 * <ul>
 *   <li>剩余天数 {@code daysLeft} = end_date − CURDATE()，负数表示已过期；end_date 为空则为 NULL。</li>
 *   <li>「临期」= 生效中（ACTIVE）且 daysLeft ≤ 指定天数（默认 30 天）。</li>
 *   <li>续约为按自然月顺延 end_date，不新建合约记录，以保持与升级补差口径一致。</li>
 * </ul>
 *
 * <p>权限 {@code contract:view}（M200 菜单授权 ADMIN/OPERATOR）。</p>
 *
 * <p>注意：本文件所属后端模块在受限沙箱无法执行 Maven 构建，需在具备网络/本地仓库的机器上
 * {@code mvn -o compile} 验证编译通过后再合并。</p>
 */
@RestController
@RequestMapping("/api/admin/contract")
public class AdminContractController {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired(required = false)
    private NotificationService notifications;

    // ============================================================ 合约台账

    /**
     * 合约台账：关联客户 / 套餐 / 小区，支持状态、临期天数、关键字筛选与分页。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('contract:view')")
    public Map<String, Object> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer expiringDays,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            where.append(" AND ct.status = ? ");
            args.add(status);
        }
        if (expiringDays != null && expiringDays > 0) {
            where.append(" AND ct.status = 'ACTIVE' ")
                    .append(" AND STR_TO_DATE(ct.end_date, '%Y-%m-%d') <= DATE_ADD(CURDATE(), INTERVAL ? DAY) ");
            args.add(expiringDays);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (c.name LIKE ? OR c.phone LIKE ?) ");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
        }

        int total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_contract ct LEFT JOIN customer c ON c.id = ct.customer_id"
                        + where,
                Integer.class, args.toArray());

        String sql = """
                SELECT ct.id, ct.customer_id AS customerId, c.name AS customerName, c.phone,
                       ct.package_id AS packageId, p.name AS packageName,
                       ct.monthly_fee AS monthlyFee,
                       ct.start_date AS startDate, ct.end_date AS endDate, ct.status,
                       com.name AS communityName,
                       DATEDIFF(STR_TO_DATE(ct.end_date, '%Y-%m-%d'), CURDATE()) AS daysLeft
                FROM customer_contract ct
                LEFT JOIN customer c ON c.id = ct.customer_id
                LEFT JOIN package_info p ON p.id = ct.package_id
                LEFT JOIN community com ON com.id = c.community_id
                """ + where + """
                ORDER BY ct.end_date ASC
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

    // ============================================================ 合约统计

    /**
     * 合约概览：总数 / 生效 / 临期(30 天) / 已过期 / 已终止 / 生效合约月费合计。
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('contract:view')")
    public Map<String, Object> stats() {
        return jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END), 0) AS active,
                       COALESCE(SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END), 0) AS expired,
                       COALESCE(SUM(CASE WHEN status = 'TERMINATED' THEN 1 ELSE 0 END), 0) AS terminated,
                       COALESCE(SUM(CASE WHEN status = 'ACTIVE'
                            AND STR_TO_DATE(end_date, '%Y-%m-%d') <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
                            THEN 1 ELSE 0 END), 0) AS expiring,
                       COALESCE(SUM(CASE WHEN status = 'ACTIVE' THEN monthly_fee ELSE 0 END), 0) AS monthlyFeeSum
                FROM customer_contract
                """);
    }

    // ============================================================ 续约办理

    /**
     * 续约：按自然月顺延到期日并置回生效态。
     * 入参 {@code {contractId, months}}，months 取值 1~120（默认 12）。
     */
    @PostMapping("/renew")
    @PreAuthorize("hasAuthority('contract:view')")
    public Map<String, Object> renew(@RequestBody Map<String, Object> body) {
        String contractId = body.get("contractId") == null ? null : String.valueOf(body.get("contractId"));
        int months = body.get("months") == null ? 12 : ((Number) body.get("months")).intValue();

        if (contractId == null || contractId.isBlank()) {
            throw new IllegalArgumentException("contractId 必填");
        }
        if (months <= 0 || months > 120) {
            throw new IllegalArgumentException("续约月份须在 1~120 之间");
        }

        boolean exists = !jdbc.queryForList(
                "SELECT id FROM customer_contract WHERE id = ?", contractId).isEmpty();
        if (!exists) {
            throw new IllegalArgumentException("合约不存在：" + contractId);
        }

        jdbc.update("UPDATE customer_contract "
                        + "SET end_date = DATE_FORMAT("
                        + "      DATE_ADD(STR_TO_DATE(end_date, '%Y-%m-%d'), INTERVAL ? MONTH), '%Y-%m-%d'), "
                        + "    status = 'ACTIVE' "
                        + "WHERE id = ?",
                months, contractId);

        Map<String, Object> after = jdbc.queryForList(
                        "SELECT id, end_date AS endDate, status FROM customer_contract WHERE id = ?", contractId)
                .stream().findFirst().orElse(null);

        if (notifications != null) {
            try {
                String custName = jdbc.queryForObject(
                        "SELECT c.name FROM customer_contract ct LEFT JOIN customer c ON c.id = ct.customer_id WHERE ct.id = ?",
                        String.class, contractId);
                notifications.notifyContractRenewed(contractId, custName,
                        after == null ? null : String.valueOf(after.get("endDate")), months);
            } catch (Exception ignored) {
                // 通知失败不影响续约主流程
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("contractId", contractId);
        resp.put("months", months);
        resp.put("endDate", after == null ? null : after.get("endDate"));
        return resp;
    }

    // ============================================================ 合约到期预警推送

    /**
     * 扫描临期（默认 30 天）生效中合约并发送「合约即将到期」站内通知。
     * 触发点：运营人员可手动触发（按钮）或后续由定时任务调用；同一合约 24h 内仅推送一次（去重）。
     */
    @PostMapping("/notify-expiring")
    @PreAuthorize("hasAuthority('contract:view')")
    public Map<String, Object> notifyExpiring(@RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT ct.id, c.name AS customerName, p.name AS packageName, ct.end_date AS endDate, "
                        + "DATEDIFF(STR_TO_DATE(ct.end_date, '%Y-%m-%d'), CURDATE()) AS daysLeft "
                        + "FROM customer_contract ct "
                        + "LEFT JOIN customer c ON c.id = ct.customer_id "
                        + "LEFT JOIN package_info p ON p.id = ct.package_id "
                        + "WHERE ct.status = 'ACTIVE' "
                        + "AND STR_TO_DATE(ct.end_date, '%Y-%m-%d') <= DATE_ADD(CURDATE(), INTERVAL ? DAY) "
                        + "ORDER BY ct.end_date ASC",
                days);
        int scanned = rows.size();
        int notified = 0;
        if (notifications != null) {
            for (Map<String, Object> r : rows) {
                try {
                    String id = notifications.notifyContractExpiring(
                            String.valueOf(r.get("id")),
                            String.valueOf(r.get("customerName")),
                            String.valueOf(r.get("packageName")),
                            String.valueOf(r.get("endDate")),
                            ((Number) r.get("daysLeft")).intValue());
                    if (id != null) notified++;
                } catch (Exception ignored) {
                    // 单条失败不影响批次
                }
            }
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("scanned", scanned);
        resp.put("notified", notified);
        return resp;
    }
}
