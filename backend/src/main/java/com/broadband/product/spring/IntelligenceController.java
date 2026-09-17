package com.broadband.product.spring;

import com.broadband.common.Ids;
import com.broadband.system.spring.AuthController;
import com.broadband.system.spring.OperLogService;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据智能（V1.15 迭代二 · 数据深化）：客户分群 / 流失预警 / 营销自动化。
 *
 * <p>全部为真实表聚合，遵循 {@link AdminProductController} 的 JdbcTemplate + 私有 helper 风格，
 * 不引入 MyBatis-Plus 实体。端点受 {@code intelligence:view} 权限保护（菜单 M150 已同源授权）。</p>
 *
 * <ul>
 *   <li>客户分群：按「订单量 / 合约到期 / 近 180 天活跃度 / 客户等级」派生 6 类分群并汇总计数。</li>
 *   <li>流失预警：导出高危客户（流失风险 + 临期待续约）含风险分与归因，按风险分倒序。</li>
 *   <li>营销自动化：规则引擎（mkt_campaign）按触发分群对客户自动发券 / 推送活动，全程留痕去重。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private OperLogService operLog;

    // ============================================================ 客户分群

    @GetMapping("/segments")
    @PreAuthorize("hasAuthority('intelligence:view')")
    public Map<String, Object> segments() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                WITH cb AS (
                  SELECT c.id, c.level,
                    (SELECT COUNT(*) FROM biz_order b WHERE b.customer_id = c.id) AS order_cnt,
                    GREATEST(
                      COALESCE((SELECT MAX(created_time) FROM biz_order b WHERE b.customer_id = c.id),0),
                      COALESCE((SELECT MAX(created_time) FROM points_record p WHERE p.customer_id = c.id),0),
                      COALESCE((SELECT MAX(created_time) FROM support_ticket t WHERE t.customer_id = c.id),0)
                    ) AS last_active,
                    (SELECT MIN(DATEDIFF(ct.end_date, CURDATE())) FROM customer_contract ct
                       WHERE ct.customer_id = c.id AND ct.status = 'ACTIVE') AS contract_days_left
                  FROM customer c
                )
                SELECT
                  CASE
                    WHEN contract_days_left IS NOT NULL AND contract_days_left <= 90 THEN 'RENEW'
                    WHEN last_active = 0 AND order_cnt = 0 THEN 'LEAD'
                    WHEN last_active = 0 OR DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) > 180 THEN 'CHURN_RISK'
                    WHEN level IN ('VIP','GOLD') AND DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) <= 30 THEN 'HIGH_VALUE'
                    WHEN DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) <= 30 THEN 'GROWING'
                    ELSE 'STABLE'
                  END AS segment,
                  COUNT(*) AS cnt
                FROM cb GROUP BY segment
                """);
        Map<String, String> labels = Map.of(
                "LEAD", "潜在客户", "RENEW", "临期待续约", "CHURN_RISK", "流失预警",
                "HIGH_VALUE", "高价值活跃", "GROWING", "成长期", "STABLE", "稳定期");
        List<Map<String, Object>> list = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> r : rows) {
            String seg = str(r.get("segment"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("segment", seg);
            m.put("segmentLabel", labels.getOrDefault(seg, seg));
            long cnt = ((Number) r.get("cnt")).longValue();
            m.put("count", cnt);
            list.add(m);
            total += cnt;
        }
        // 保证 6 类分群都出现（无成员的置 0），顺序稳定
        for (String seg : new String[]{"LEAD", "RENEW", "CHURN_RISK", "HIGH_VALUE", "GROWING", "STABLE"}) {
            if (list.stream().noneMatch(m -> seg.equals(m.get("segment")))) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("segment", seg);
                m.put("segmentLabel", labels.get(seg));
                m.put("count", 0L);
                list.add(m);
            }
        }
        // 按固定顺序排
        list.sort((a, b) -> Integer.compare(
                Arrays.asList("LEAD", "GROWING", "STABLE", "HIGH_VALUE", "RENEW", "CHURN_RISK")
                        .indexOf(a.get("segment")),
                Arrays.asList("LEAD", "GROWING", "STABLE", "HIGH_VALUE", "RENEW", "CHURN_RISK")
                        .indexOf(b.get("segment"))));
        result.put("total", total);
        result.put("segments", list);
        return result;
    }

    // ============================================================ 流失预警

    @GetMapping("/churn")
    @PreAuthorize("hasAuthority('intelligence:view')")
    public Map<String, Object> churn(@RequestParam(required = false, defaultValue = "50") Integer limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                WITH cb AS (
                  SELECT c.id, c.name, c.level, c.status,
                    (SELECT COUNT(*) FROM biz_order b WHERE b.customer_id = c.id) AS order_cnt,
                    GREATEST(
                      COALESCE((SELECT MAX(created_time) FROM biz_order b WHERE b.customer_id = c.id),0),
                      COALESCE((SELECT MAX(created_time) FROM points_record p WHERE p.customer_id = c.id),0),
                      COALESCE((SELECT MAX(created_time) FROM support_ticket t WHERE t.customer_id = c.id),0)
                    ) AS last_active,
                    (SELECT MIN(DATEDIFF(ct.end_date, CURDATE())) FROM customer_contract ct
                       WHERE ct.customer_id = c.id AND ct.status = 'ACTIVE') AS contract_days_left
                  FROM customer c
                )
                SELECT id, name, level, status, order_cnt AS orderCnt, last_active AS lastActive,
                       contract_days_left AS contractDaysLeft,
                       CASE
                         WHEN order_cnt = 0 THEN 'LEAD'
                         WHEN contract_days_left IS NOT NULL AND contract_days_left <= 90 THEN 'RENEW'
                         WHEN last_active = 0 OR DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) > 180 THEN 'CHURN_RISK'
                         WHEN level IN ('VIP','GOLD') AND DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) <= 30 THEN 'HIGH_VALUE'
                         WHEN DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) <= 30 THEN 'GROWING'
                         ELSE 'STABLE'
                       END AS segment
                FROM cb
                WHERE (contract_days_left IS NOT NULL AND contract_days_left <= 90)
                   OR (last_active = 0 OR DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) > 120)
                ORDER BY
                  CASE WHEN segment='CHURN_RISK' THEN 0 WHEN segment='RENEW' THEN 1 ELSE 2 END,
                  contract_days_left ASC, last_active ASC
                LIMIT ?
                """, limit);

        Map<String, String> levelLabels = Map.of("VIP", "五星", "GOLD", "四星", "SILVER", "三星", "NORMAL", "普通");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String seg = str(r.get("segment"));
            long lastActive = r.get("lastActive") == null ? 0 : ((Number) r.get("lastActive")).longValue();
            long daysSince = lastActive == 0 ? 9999
                    : (System.currentTimeMillis() - lastActive) / 86_400_000L;
            long contractDaysLeft = r.get("contractDaysLeft") == null ? 9999
                    : ((Number) r.get("contractDaysLeft")).longValue();
            int risk = computeRisk(seg, daysSince, contractDaysLeft);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id"));
            m.put("name", r.get("name"));
            m.put("level", r.get("level"));
            m.put("levelLabel", levelLabels.getOrDefault(str(r.get("level")), str(r.get("level"))));
            m.put("segment", seg);
            m.put("segmentLabel", segmentLabel(seg));
            m.put("daysSince", daysSince);
            m.put("contractDaysLeft", contractDaysLeft);
            m.put("lastActiveText", lastActive == 0 ? "无记录" : fmt(lastActive));
            m.put("riskScore", risk);
            m.put("reasons", reasons(seg, daysSince, contractDaysLeft));
            list.add(m);
        }
        result.put("total", list.size());
        result.put("list", list);
        return result;
    }

    // ============================================================ 营销自动化规则

    @GetMapping("/campaigns")
    @PreAuthorize("hasAuthority('intelligence:view')")
    public List<Map<String, Object>> campaigns() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, name, trigger_type AS triggerType, action_type AS actionType,
                       target_item AS targetItem, status, description,
                       (SELECT COUNT(*) FROM mkt_campaign_exec e WHERE e.campaign_id = m.id) AS execTotal,
                       (SELECT COUNT(*) FROM mkt_campaign_exec e WHERE e.campaign_id = m.id
                          AND e.created_time >= ?) AS execRecent
                FROM mkt_campaign m ORDER BY m.id
                """, System.currentTimeMillis() - 30L * 86_400_000L);
        Map<String, String> trigLabels = Map.of(
                "CHURN_RISK", "流失预警客户", "RENEW", "临期待续约客户",
                "HIGH_VALUE", "高价值活跃客户", "GROWING", "成长期客户");
        for (Map<String, Object> r : rows) {
            r.put("triggerLabel", trigLabels.getOrDefault(str(r.get("triggerType")), str(r.get("triggerType"))));
            r.put("actionLabel", "GRANT_COUPON".equals(str(r.get("actionType"))) ? "自动发券" : "推送活动");
        }
        return rows;
    }

    /**
     * 营销自动化一键触发：遍历启用规则，对命中分群客户执行动作（发券 / 推送），30 天内已执行过的去重。
     */
    @PostMapping("/auto-trigger")
    @PreAuthorize("hasAuthority('intelligence:view')")
    public Map<String, Object> autoTrigger(@RequestBody(required = false) Map<String, Object> body) {
        Boolean dryRun = body != null && Boolean.TRUE.equals(body.get("dryRun"));
        List<Map<String, Object>> campaigns = jdbc.queryForList(
                "SELECT id, name, trigger_type AS triggerType, action_type AS actionType, target_item AS targetItem, status FROM mkt_campaign WHERE status='ENABLED'");

        long now = System.currentTimeMillis();
        long dupWindow = now - 30L * 86_400_000L;
        int matched = 0, granted = 0, pushed = 0, skipped = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Map<String, Object> cp : campaigns) {
            String campaignId = str(cp.get("id"));
            String trigger = str(cp.get("triggerType"));
            String action = str(cp.get("actionType"));
            String target = str(cp.get("targetItem"));
            List<String> custIds = targetCustomers(trigger);
            int cMatched = 0, cActed = 0, cSkip = 0;
            for (String cid : custIds) {
                matched++; cMatched++;
                Integer dup = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM mkt_campaign_exec WHERE campaign_id=? AND customer_id=? AND created_time>=?",
                        Integer.class, campaignId, cid, dupWindow);
                if (dup != null && dup > 0) {
                    skipped++; cSkip++;
                    if (!dryRun) logExec(campaignId, cid, action, target, "DUP", "30 天内已触达");
                    continue;
                }
                if (dryRun) { cActed++; continue; }
                if ("GRANT_COUPON".equals(action)) {
                    Map<String, Object> item = one(
                            "SELECT coupon_type, coupon_value FROM points_mall_item WHERE id=?", target);
                    if (item == null) { cSkip++; skipped++; logExec(campaignId, cid, action, target, "SKIP", "目标商品不存在"); continue; }
                    String couponId = Ids.next();
                    String code = "C" + now + (int) (Math.random() * 9000 + 1000);
                    jdbc.update("""
                            INSERT INTO points_coupon (id, customer_id, item_id, coupon_code, coupon_type, coupon_value, status, created_time, expire_time)
                            VALUES (?, ?, ?, ?, ?, ?, 'UNUSED', ?, ?)
                            """, couponId, cid, target, code, str(item.get("coupon_type")), str(item.get("coupon_value")),
                            now, now + 30L * 86_400_000L);
                    logExec(campaignId, cid, action, target, "SUCCESS", "发放券 " + code);
                    granted++; cActed++;
                } else { // SEND_PROMO
                    logExec(campaignId, cid, action, target, "SUCCESS", "推送活动 " + target);
                    pushed++; cActed++;
                }
            }
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("campaignId", campaignId);
            d.put("name", cp.get("name"));
            d.put("trigger", trigger);
            d.put("matched", cMatched);
            d.put("acted", cActed);
            d.put("skipped", cSkip);
            details.add(d);
        }

        log("营销自动化触发（dryRun=" + dryRun + "）", "/api/intelligence/auto-trigger", "POST");
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("dryRun", dryRun);
        resp.put("matched", matched);
        resp.put("granted", granted);
        resp.put("pushed", pushed);
        resp.put("skipped", skipped);
        resp.put("details", details);
        return resp;
    }

    // ============================================================ 工具

    /** 命中某触发分群的全部客户 id（复用分群口径）。 */
    private List<String> targetCustomers(String trigger) {
        String seg = switch (trigger) {
            case "CHURN_RISK" -> "CHURN_RISK";
            case "RENEW" -> "RENEW";
            case "HIGH_VALUE" -> "HIGH_VALUE";
            case "GROWING" -> "GROWING";
            default -> "CHURN_RISK";
        };
        return jdbc.queryForList("""
                WITH cb AS (
                  SELECT c.id, c.level,
                    (SELECT COUNT(*) FROM biz_order b WHERE b.customer_id = c.id) AS order_cnt,
                    GREATEST(
                      COALESCE((SELECT MAX(created_time) FROM biz_order b WHERE b.customer_id = c.id),0),
                      COALESCE((SELECT MAX(created_time) FROM points_record p WHERE p.customer_id = c.id),0),
                      COALESCE((SELECT MAX(created_time) FROM support_ticket t WHERE t.customer_id = c.id),0)
                    ) AS last_active,
                    (SELECT MIN(DATEDIFF(ct.end_date, CURDATE())) FROM customer_contract ct
                       WHERE ct.customer_id = c.id AND ct.status = 'ACTIVE') AS contract_days_left
                  FROM customer c
                )
                SELECT id FROM cb
                WHERE CASE
                    WHEN contract_days_left IS NOT NULL AND contract_days_left <= 90 THEN 'RENEW'
                    WHEN last_active = 0 AND order_cnt = 0 THEN 'LEAD'
                    WHEN last_active = 0 OR DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) > 180 THEN 'CHURN_RISK'
                    WHEN level IN ('VIP','GOLD') AND DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) <= 30 THEN 'HIGH_VALUE'
                    WHEN DATEDIFF(CURDATE(), FROM_UNIXTIME(last_active/1000)) <= 30 THEN 'GROWING'
                    ELSE 'STABLE' END = ?
                """, seg).stream().map(m -> str(m.get("id"))).toList();
    }

    private void logExec(String campaignId, String customerId, String action, String target,
                         String status, String remark) {
        jdbc.update("""
                INSERT INTO mkt_campaign_exec (id, campaign_id, customer_id, action_type, target_item, status, remark, created_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, Ids.next(), campaignId, customerId, action, target, status, remark, System.currentTimeMillis());
    }

    private static int computeRisk(String seg, long daysSince, long contractDaysLeft) {
        if ("CHURN_RISK".equals(seg)) {
            return (int) Math.min(99, 70 + Math.max(0, daysSince - 180));
        }
        if ("RENEW".equals(seg)) {
            return (int) Math.min(95, 50 + Math.max(0, 90 - contractDaysLeft));
        }
        if (daysSince > 120) return (int) Math.min(69, 40 + (daysSince - 120) / 10);
        return 20;
    }

    private static List<String> reasons(String seg, long daysSince, long contractDaysLeft) {
        List<String> r = new ArrayList<>();
        if ("CHURN_RISK".equals(seg)) {
            r.add(daysSince >= 9999 ? "无业务互动记录" : "近 " + daysSince + " 天无互动，存在流失风险");
        } else if ("RENEW".equals(seg)) {
            r.add(contractDaysLeft >= 9999 ? "合约临期" : "合约将于 " + contractDaysLeft + " 天后到期");
        } else if (daysSince > 120) {
            r.add("近 " + daysSince + " 天互动较少");
        }
        return r;
    }

    private static String segmentLabel(String seg) {
        return Map.of("LEAD", "潜在客户", "RENEW", "临期待续约", "CHURN_RISK", "流失预警",
                "HIGH_VALUE", "高价值活跃", "GROWING", "成长期", "STABLE", "稳定期")
                .getOrDefault(seg, seg);
    }

    private static String fmt(long ms) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(ms));
    }

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

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> l = jdbc.queryForList(sql, args);
        return l.isEmpty() ? null : l.get(0);
    }
}
