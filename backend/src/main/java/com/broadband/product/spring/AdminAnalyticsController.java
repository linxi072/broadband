package com.broadband.product.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据分析深化（T-04 三页深化）：客户 360 / 营销漏斗 / SLA 超时与赔付 / 赔付趋势。
 *
 * <p>权限 {@code analytics:view}（菜单 M190/M191/M192/M193）。全部基于 customer / biz_order / review /
 * points_account / sla_record / work_order / community / compensation 真实聚合，与前端演示数据字段一一对齐。</p>
 */
@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasAuthority('analytics:view')")
public class AdminAnalyticsController {

    @Autowired private JdbcTemplate jdbc;

    /** 客户 360：基础信息 + 生命周期分群/风险 + 订单/评价/积分聚合。 */
    @GetMapping("/customer-360")
    public Map<String, Object> customer360(@RequestParam(required = false) String customerId) {
        if (customerId == null || customerId.isBlank()) {
            try {
                customerId = jdbc.queryForObject(
                        "select customer_id from biz_order group by customer_id order by count(*) desc limit 1", String.class);
            } catch (Exception ignored) {
                try {
                    customerId = jdbc.queryForObject("select id from customer order by created_time desc limit 1", String.class);
                } catch (Exception e) {
                    customerId = null;
                }
            }
        }
        if (customerId == null) return Map.of();
        Map<String, Object> cust = jdbc.queryForMap(
                "select id, name, phone, level, status, community_id, created_time from customer where id=?", customerId);
        String name = String.valueOf(cust.get("name"));

        long orderCount = countOrSum("select count(*) from biz_order where customer_id=?", customerId);
        long totalSpent = countOrSum(
                "select coalesce(sum(amount),0) from biz_order where customer_id=? and status in ('PAID','INSTALLING','DONE')", customerId);
        long pointsBalance = countOrSum("select coalesce(balance,0) from points_account where customer_id=?", customerId);
        long complaintOpen = countOrSum(
                "select count(*) from review where type='COMPLAINT' and customer_name=? and status<>'CLOSED'", name);
        Long lastOrder = jdbc.queryForObject("select max(created_time) from biz_order where customer_id=?", Long.class, customerId);
        long daysSince = lastOrder == null ? 9999 : (System.currentTimeMillis() - lastOrder) / 86_400_000L;

        String segment;
        String segmentLabel;
        long riskScore;
        List<String> riskReasons = new ArrayList<>();
        if (orderCount >= 4 && totalSpent >= 400) {
            segment = "HIGH_VALUE"; segmentLabel = "高价值"; riskScore = 22;
            riskReasons.add("高价值客户，需重点维系");
        } else if (daysSince > 120) {
            segment = "CHURN_RISK"; segmentLabel = "流失风险"; riskScore = 73;
            riskReasons.add("超过 120 天未下单，流失高风险");
        } else if (daysSince > 60) {
            segment = "AT_RISK"; segmentLabel = "预警"; riskScore = 41;
            riskReasons.add("60 天以上未互动，存在流失预警");
        } else {
            segment = "ACTIVE"; segmentLabel = "活跃"; riskScore = 30;
            riskReasons.add("活跃客户，保持常规运营");
        }

        List<Map<String, Object>> recentOrders = jdbc.queryForList(
                "select id, order_type as orderType, package_name as packageName, amount, status, created_time as createdTime "
                        + "from biz_order where customer_id=? order by created_time desc limit 5",
                customerId);
        List<Map<String, Object>> recentReviews = jdbc.queryForList(
                "select score, type, content, status, created_time as createdTime from review where customer_name=? order by created_time desc limit 3",
                name);

        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("id", cust.get("id"));
        basic.put("name", cust.get("name"));
        basic.put("phone", cust.get("phone"));
        basic.put("level", cust.get("level"));
        basic.put("communityId", cust.get("community_id"));
        basic.put("status", cust.get("status"));
        basic.put("createdTime", cust.get("created_time"));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("basic", basic);
        r.put("segment", segment);
        r.put("segmentLabel", segmentLabel);
        r.put("riskScore", riskScore);
        r.put("riskReasons", riskReasons);
        r.put("orderCount", orderCount);
        r.put("totalSpent", totalSpent);
        r.put("pointsBalance", pointsBalance);
        r.put("complaintOpen", complaintOpen);
        r.put("daysSinceLastOrder", daysSince);
        r.put("recentOrders", recentOrders);
        r.put("recentReviews", recentReviews);
        r.put("activeRefunds", List.of());
        return r;
    }

    /** 营销漏斗：注册 → 活跃 → 创建订单 → 支付成功 → 复购，按注册客户数折算转化率。 */
    @GetMapping("/funnel")
    public Map<String, Object> funnel() {
        long now = System.currentTimeMillis();
        long registered = countOrSum("select count(*) from customer");
        long active = countOrSum("select count(distinct customer_id) from biz_order where created_time >= ?", now - 90L * 86_400_000L);
        long created = countOrSum("select count(distinct customer_id) from biz_order");
        long paid = countOrSum("select count(distinct customer_id) from biz_order where status in ('PAID','INSTALLING','DONE')");
        long repurchase = countOrSum(
                "select count(*) from (select customer_id from biz_order group by customer_id having count(*) > 1) t");

        List<Map<String, Object>> stages = new ArrayList<>();
        addStage(stages, "注册客户", registered, registered);
        addStage(stages, "活跃参与", active, registered);
        addStage(stages, "创建订单", created, registered);
        addStage(stages, "支付成功", paid, registered);
        addStage(stages, "复购客户", repurchase, registered);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("registered", registered);
        r.put("stages", stages);
        return r;
    }

    /** SLA 超时热力：总超时数 + 按小区分布 + 近 5 周趋势。 */
    @GetMapping("/sla-heatmap")
    public Map<String, Object> slaHeatmap(@RequestParam(defaultValue = "30") int range) {
        long since = System.currentTimeMillis() - (long) range * 86_400_000L;
        long totalOvertime = countOrSum(
                "select count(*) from sla_record where sla_status='OVERTIME' and created_time >= ?", since);
        List<Map<String, Object>> byCommunity = jdbc.queryForList(
                "select coalesce(wo.community_id,'-') as communityId, coalesce(c.name,'-') as communityName, count(*) as overtimeCount "
                        + "from sla_record sr left join work_order wo on wo.id = sr.order_id "
                        + "left join community c on c.id = wo.community_id "
                        + "where sr.sla_status='OVERTIME' and sr.created_time >= ? "
                        + "group by wo.community_id, c.name order by overtimeCount desc", since);
        List<Map<String, Object>> recs = jdbc.queryForList(
                "select created_time from sla_record where sla_status='OVERTIME' and created_time >= ?", since);
        List<Map<String, Object>> trend = weeklyTrend(recs, "overtimeCount");

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("range", range);
        r.put("totalOvertime", totalOvertime);
        r.put("byCommunity", byCommunity);
        r.put("trend", trend);
        return r;
    }

    /** 赔付趋势：区间内赔付总额/笔数 + 按月趋势。 */
    @GetMapping("/payout-trend")
    public Map<String, Object> payoutTrend(@RequestParam(defaultValue = "90") int range) {
        long since = System.currentTimeMillis() - (long) range * 86_400_000L;
        double totalAmount = sumDouble("select coalesce(sum(comp_amount),0) from compensation where created_time >= ?", since);
        long totalCount = countOrSum("select count(*) from compensation where created_time >= ?", since);
        List<Map<String, Object>> recs = jdbc.queryForList(
                "select created_time, comp_amount from compensation where created_time >= ?", since);

        java.util.LinkedHashMap<String, double[]> bucket = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : recs) {
            Object t = row.get("created_time");
            if (t == null) continue;
            long ms = ((Number) t).longValue();
            LocalDate d = LocalDate.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
            String month = d.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            double[] arr = bucket.computeIfAbsent(month, k -> new double[]{0, 0});
            arr[0] += ((Number) row.get("comp_amount")).doubleValue();
            arr[1] += 1;
        }
        List<String> months = new ArrayList<>(bucket.keySet());
        months.sort(Comparator.naturalOrder());
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String m : months) {
            double[] arr = bucket.get(m);
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("weekStart", m + "-01");
            mm.put("amount", Math.round(arr[0]));
            mm.put("count", (int) arr[1]);
            trend.add(mm);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("range", range);
        r.put("totalAmount", Math.round(totalAmount));
        r.put("totalCount", totalCount);
        r.put("trend", trend);
        return r;
    }

    // ============================================================ 内部

    private void addStage(List<Map<String, Object>> stages, String stage, long count, long total) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stage", stage);
        m.put("count", count);
        m.put("conversion", total == 0 ? 0 : Math.round(count * 1000.0 / total) / 10.0);
        stages.add(m);
    }

    private List<Map<String, Object>> weeklyTrend(List<Map<String, Object>> recs, String countKey) {
        Map<String, Integer> bucket = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : recs) {
            Object t = row.get("created_time");
            if (t == null) continue;
            long ms = ((Number) t).longValue();
            LocalDate d = LocalDate.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
            String wk = d.with(DayOfWeek.MONDAY).toString();
            bucket.put(wk, bucket.getOrDefault(wk, 0) + 1);
        }
        List<String> weeks = new ArrayList<>(bucket.keySet());
        weeks.sort(Comparator.naturalOrder());
        List<String> last5 = weeks.subList(Math.max(0, weeks.size() - 5), weeks.size());
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String wk : last5) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("weekStart", wk);
            m.put(countKey, bucket.get(wk));
            trend.add(m);
        }
        return trend;
    }

    private long countOrSum(String sql, Object... args) {
        try {
            Number n = jdbc.queryForObject(sql, Number.class, args);
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private double sumDouble(String sql, Object... args) {
        try {
            Number n = jdbc.queryForObject(sql, Number.class, args);
            return n == null ? 0 : n.doubleValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
