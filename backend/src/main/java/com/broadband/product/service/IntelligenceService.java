package com.broadband.product.service;

import com.broadband.common.Ids;
import com.broadband.common.RedisCacheService;
import com.broadband.common.Values;
import com.broadband.product.mapper.IntelligenceMapper;
import com.broadband.system.service.CurrentUser;
import com.broadband.system.service.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据智能（V1.15 迭代二 · 数据深化）：客户分群 / 流失预警 / 营销自动化。
 *
 * <p>分群口径全部来自 {@code IntelligenceMapper.xml} 的共享 CTE 片段，与「客户 360」生命周期阶段同源；
 * 风险分与归因复用 {@link CustomerAdminService} 的分群派生方法，避免两处各算一套。</p>
 */
@Service
public class IntelligenceService {

    /** 分群固定展示顺序。 */
    private static final List<String> SEGMENT_ORDER =
            Arrays.asList("LEAD", "GROWING", "STABLE", "HIGH_VALUE", "RENEW", "CHURN_RISK");

    /** 营销触达去重窗口（毫秒）：30 天内已触达过的客户不再重复动作。 */
    private static final long DEDUP_WINDOW_MS = 30L * 86_400_000L;

    @Autowired private IntelligenceMapper intelligenceMapper;
    @Autowired private OperLogService operLog;
    @Autowired private RedisCacheService cache;

    // ============================================================ 客户分群

    /** 客户分群：6 类分群计数（无成员的置 0，顺序固定）。结果缓存 2 分钟（分群口径变化低频）。 */
    public Map<String, Object> segments() {
        return cache.get("intel:segments", Duration.ofSeconds(120), this::computeSegments);
    }

    /** 分群聚合计算（缓存未命中时执行）。 */
    private Map<String, Object> computeSegments() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = intelligenceMapper.segmentCounts();
        Map<String, String> labels = Map.of(
                "LEAD", "潜在客户", "RENEW", "临期待续约", "CHURN_RISK", "流失预警",
                "HIGH_VALUE", "高价值活跃", "GROWING", "成长期", "STABLE", "稳定期");
        List<Map<String, Object>> list = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> r : rows) {
            String seg = Values.str(r.get("segment"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("segment", seg);
            m.put("segmentLabel", labels.getOrDefault(seg, seg));
            long cnt = ((Number) r.get("cnt")).longValue();
            m.put("count", cnt);
            list.add(m);
            total += cnt;
        }
        for (String seg : new String[]{"LEAD", "RENEW", "CHURN_RISK", "HIGH_VALUE", "GROWING", "STABLE"}) {
            if (list.stream().noneMatch(m -> seg.equals(m.get("segment")))) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("segment", seg);
                m.put("segmentLabel", labels.get(seg));
                m.put("count", 0L);
                list.add(m);
            }
        }
        list.sort((a, b) -> Integer.compare(
                SEGMENT_ORDER.indexOf(a.get("segment")),
                SEGMENT_ORDER.indexOf(b.get("segment"))));
        result.put("total", total);
        result.put("segments", list);
        return result;
    }

    // ============================================================ 流失预警

    /** 流失预警：导出高危客户（流失风险 + 临期待续约）含风险分与归因，按风险分倒序。 */
    public Map<String, Object> churn(int limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = intelligenceMapper.churnList(limit);
        Map<String, String> levelLabels = Map.of("VIP", "五星", "GOLD", "四星", "SILVER", "三星", "NORMAL", "普通");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String seg = Values.str(r.get("segment"));
            long lastActive = r.get("lastActive") == null ? 0 : ((Number) r.get("lastActive")).longValue();
            long daysSince = lastActive == 0 ? 9999 : (System.currentTimeMillis() - lastActive) / 86_400_000L;
            long contractDaysLeft = r.get("contractDaysLeft") == null ? 9999
                    : ((Number) r.get("contractDaysLeft")).longValue();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id"));
            m.put("name", r.get("name"));
            m.put("level", r.get("level"));
            m.put("levelLabel", levelLabels.getOrDefault(Values.str(r.get("level")), Values.str(r.get("level"))));
            m.put("segment", seg);
            m.put("segmentLabel", CustomerAdminService.segLabel(seg));
            m.put("daysSince", daysSince);
            m.put("contractDaysLeft", contractDaysLeft);
            m.put("lastActiveText", lastActive == 0 ? "无记录" : fmt(lastActive));
            m.put("riskScore", CustomerAdminService.segRisk(seg, daysSince, contractDaysLeft));
            m.put("reasons", CustomerAdminService.segReasons(seg, daysSince, contractDaysLeft));
            list.add(m);
        }
        result.put("total", list.size());
        result.put("list", list);
        return result;
    }

    // ============================================================ 分群客户下钻

    /** 分群客户明细下钻（US-2.2）：与聚合口径完全一致。 */
    public Map<String, Object> segmentCustomers(String segment, int limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = intelligenceMapper.segmentCustomers(segment, limit);
        Map<String, String> levelLabels = Map.of("VIP", "五星", "GOLD", "四星", "SILVER", "三星", "NORMAL", "普通");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            long lastActive = r.get("lastActive") == null ? 0 : ((Number) r.get("lastActive")).longValue();
            long contractDaysLeft = r.get("contractDaysLeft") == null ? 9999
                    : ((Number) r.get("contractDaysLeft")).longValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id"));
            m.put("name", r.get("name"));
            m.put("level", r.get("level"));
            m.put("levelLabel", levelLabels.getOrDefault(Values.str(r.get("level")), Values.str(r.get("level"))));
            m.put("status", r.get("status"));
            m.put("orderCnt", r.get("orderCnt"));
            m.put("contractDaysLeft", contractDaysLeft);
            m.put("lastActiveText", lastActive == 0 ? "无记录" : fmt(lastActive));
            m.put("segment", segment);
            m.put("segmentLabel", CustomerAdminService.segLabel(segment));
            list.add(m);
        }
        result.put("segment", segment);
        result.put("segmentLabel", CustomerAdminService.segLabel(segment));
        result.put("count", list.size());
        result.put("rows", list);
        return result;
    }

    // ============================================================ 营销自动化规则

    /** 营销自动化规则列表（含执行次数统计）。 */
    public List<Map<String, Object>> campaigns() {
        List<Map<String, Object>> rows = intelligenceMapper.campaigns(System.currentTimeMillis() - DEDUP_WINDOW_MS);
        Map<String, String> trigLabels = Map.of(
                "CHURN_RISK", "流失预警客户", "RENEW", "临期待续约客户",
                "HIGH_VALUE", "高价值活跃客户", "GROWING", "成长期客户");
        for (Map<String, Object> r : rows) {
            r.put("triggerLabel", trigLabels.getOrDefault(Values.str(r.get("triggerType")), Values.str(r.get("triggerType"))));
            r.put("actionLabel", "GRANT_COUPON".equals(Values.str(r.get("actionType"))) ? "自动发券" : "推送活动");
        }
        return rows;
    }

    /**
     * 营销自动化一键触发：遍历启用规则，对命中分群客户执行动作（发券 / 推送），30 天内已执行过的去重。
     * @param dryRun true 时只统计不落库
     */
    public Map<String, Object> autoTrigger(boolean dryRun) {
        List<Map<String, Object>> campaigns = intelligenceMapper.enabledCampaigns();

        long now = System.currentTimeMillis();
        long dupWindow = now - DEDUP_WINDOW_MS;
        int matched = 0, granted = 0, pushed = 0, skipped = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Map<String, Object> cp : campaigns) {
            String campaignId = Values.str(cp.get("id"));
            String trigger = Values.str(cp.get("triggerType"));
            String action = Values.str(cp.get("actionType"));
            String target = Values.str(cp.get("targetItem"));
            List<String> custIds = intelligenceMapper.targetCustomers(toSegment(trigger))
                    .stream().map(m -> Values.str(m.get("id"))).toList();
            int cMatched = 0, cActed = 0, cSkip = 0;
            for (String cid : custIds) {
                matched++; cMatched++;
                Long dup = intelligenceMapper.countExecDup(campaignId, cid, dupWindow);
                if (dup != null && dup > 0) {
                    skipped++; cSkip++;
                    if (!dryRun) intelligenceMapper.insertExec(Ids.next(), campaignId, cid, action, target, "DUP", "30 天内已触达", now);
                    continue;
                }
                if (dryRun) { cActed++; continue; }
                if ("GRANT_COUPON".equals(action)) {
                    Map<String, Object> item = intelligenceMapper.mallItem(target);
                    if (item == null) {
                        cSkip++; skipped++;
                        intelligenceMapper.insertExec(Ids.next(), campaignId, cid, action, target, "SKIP", "目标商品不存在", now);
                        continue;
                    }
                    String couponId = Ids.next();
                    String code = "C" + now + (int) (Math.random() * 9000 + 1000);
                    intelligenceMapper.insertCoupon(couponId, cid, target, code,
                            Values.str(item.get("coupon_type")), Values.str(item.get("coupon_value")),
                            now, now + DEDUP_WINDOW_MS);
                    intelligenceMapper.insertExec(Ids.next(), campaignId, cid, action, target, "SUCCESS", "发放券 " + code, now);
                    granted++; cActed++;
                } else { // SEND_PROMO
                    intelligenceMapper.insertExec(Ids.next(), campaignId, cid, action, target, "SUCCESS", "推送活动 " + target, now);
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

    /** 触发类型 → 分群枚举（与分群口径一致）。 */
    private static String toSegment(String trigger) {
        return switch (trigger) {
            case "RENEW" -> "RENEW";
            case "HIGH_VALUE" -> "HIGH_VALUE";
            case "GROWING" -> "GROWING";
            default -> "CHURN_RISK";
        };
    }

    private static String fmt(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(ms));
    }

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
