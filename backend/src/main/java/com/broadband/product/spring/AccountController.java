package com.broadband.product.spring;

import com.broadband.system.security.CustomerPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 账户与账单中心（C 端小程序，V1.14 运营留存）。
 *
 * <ul>
 *   <li>GET /api/account/summary —— 账户概览：积分余额 / 本月消费 / 合约到期 / 客户分层</li>
 *   <li>GET /api/account/bills   —— 账单明细：按账期聚合业务订单（含退款/发票状态）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired private JdbcTemplate jdbc;

    /** 账户概览。身份优先取认证主体，未认证（开发态开放层）回退请求参数。 */
    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(required = false) String customerId,
                                       @AuthenticationPrincipal CustomerPrincipal cp) {
        customerId = resolveCustomerId(cp, customerId);
        if (isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        Map<String, Object> cust = one(
                "SELECT id, name, level, phone FROM customer WHERE id = ?", customerId);
        if (cust == null) throw new IllegalArgumentException("客户不存在：" + customerId);

        // 积分余额
        Map<String, Object> pa = one(
                "SELECT COALESCE(balance,0) AS balance FROM points_account WHERE customer_id = ?", customerId);
        int points = pa == null ? 0 : ((Number) pa.get("balance")).intValue();

        // 本月消费：业务订单（PAID/INSTALLING/DONE）当日历月内
        long[] m = thisMonthRange();
        Integer monthConsume = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM biz_order "
                        + "WHERE customer_id = ? AND status IN ('PAID','INSTALLING','DONE') "
                        + "AND created_time >= ? AND created_time < ?",
                Integer.class, customerId, m[0], m[1]);

        // 合约到期日（取最近一条 ACTIVE 合约）
        Map<String, Object> contract = one(
                "SELECT end_date FROM customer_contract WHERE customer_id = ? AND status='ACTIVE' "
                        + "ORDER BY end_date DESC LIMIT 1", customerId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("customerId", customerId);
        resp.put("name", cust.get("name"));
        resp.put("level", cust.get("level"));
        resp.put("points", points);
        resp.put("monthConsume", monthConsume == null ? 0 : monthConsume);
        resp.put("contractEnd", contract == null ? null : contract.get("end_date"));
        return resp;
    }

    /** 账单明细。身份优先取认证主体，未认证（开发态开放层）回退请求参数。 */
    @GetMapping("/bills")
    public List<Map<String, Object>> bills(@RequestParam(required = false) String customerId,
                                           @RequestParam(required = false) String period,
                                           @AuthenticationPrincipal CustomerPrincipal cp) {
        customerId = resolveCustomerId(cp, customerId);
        if (isBlank(customerId)) return List.of();
        String sql = "SELECT id, package_name AS packageName, amount, order_type AS orderType, "
                + "status, created_time AS createdTime FROM biz_order WHERE customer_id = ?";
        List<Object> args = new java.util.ArrayList<>();
        args.add(customerId);
        if (!isBlank(period)) {
            sql += " AND DATE_FORMAT(FROM_UNIXTIME(created_time/1000), '%Y-%m') = ?";
            args.add(period);
        }
        sql += " ORDER BY created_time DESC";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
        for (Map<String, Object> r : rows) {
            r.put("statusText", orderStatusText(String.valueOf(r.get("status"))));
        }
        return rows;
    }

    // ---------------------------------------------------------------- 辅助

    private long[] thisMonthRange() {
        ZoneId z = ZoneId.systemDefault();
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        long start = first.atStartOfDay(z).toInstant().toEpochMilli();
        long end = first.plusMonths(1).atStartOfDay(z).toInstant().toEpochMilli();
        return new long[]{start, end};
    }

    private static String orderStatusText(String s) {
        if (s == null) return "未知";
        return switch (s) {
            case "PENDING" -> "待支付";
            case "PAID" -> "已支付";
            case "INSTALLING" -> "安装中";
            case "DONE" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "REFUND" -> "已退款";
            default -> s;
        };
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> l = jdbc.queryForList(sql, args);
        return l.isEmpty() ? null : l.get(0);
    }

    /**
     * 解析操作主体身份：小程序 token 落地（protect-client-api=true）后，优先采用认证主体，
     * 杜绝客户端伪造 customerId 的越权（IDOR）；开发态开放层未认证时回退请求参数。
     */
    private static String resolveCustomerId(CustomerPrincipal cp, String fallback) {
        if (cp != null && cp.id != null && !cp.id.isBlank()) return cp.id;
        return fallback;
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
