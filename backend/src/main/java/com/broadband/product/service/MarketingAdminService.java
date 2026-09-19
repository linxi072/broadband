package com.broadband.product.service;

import com.broadband.common.RedisCacheService;
import com.broadband.product.mapper.MarketingAdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 套餐营销看板（PC 后台读侧聚合）。数据访问通过 {@link MarketingAdminMapper}（SQL 在 XML）。
 *
 * <p>输出：汇总指标 + 转化漏斗 + 套餐销量排行 + 业务类型分布 + 升级单状态分布 +
 * 客户分层分布 + 近 6 月营收趋势，以及漏斗阶段下钻明细。</p>
 */
@Service
public class MarketingAdminService {

    @Autowired private MarketingAdminMapper marketingAdminMapper;
    @Autowired private RedisCacheService cache;

    /**
     * 套餐营销看板：汇总指标 + 套餐销量排行 + 业务类型分布 + 升级单状态分布 + 客户分层分布 + 近 6 月营收趋势（短 TTL 缓存）。
     */
    public Map<String, Object> productMarketing() {
        return cache.get("mkt:board", Duration.ofSeconds(60), this::computeProductMarketing);
    }

    /** 营销看板聚合计算（缓存未命中时执行）。 */
    private Map<String, Object> computeProductMarketing() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ---- 汇总指标 ----
        Map<String, Object> summary = marketingAdminMapper.summary();
        if (summary == null) summary = new LinkedHashMap<>();
        long upgradeCount = ((Number) summary.getOrDefault("upgradeCount", 0)).longValue();
        long effectiveUpgrades = ((Number) summary.getOrDefault("effectiveUpgrades", 0)).longValue();
        double upgradeRate = upgradeCount == 0 ? 0 : Math.round(effectiveUpgrades * 1000.0 / upgradeCount) / 10.0;
        summary.put("upgradeRate", upgradeRate);
        result.put("summary", summary);

        // ---- 转化漏斗：业务订单 → 已支付 → 已完成 → 升级申请 → 升级生效 ----
        long totalOrders = ((Number) summary.getOrDefault("totalOrders", 0)).longValue();
        long doneOrders = ((Number) summary.getOrDefault("doneOrders", 0)).longValue();
        Long paidObj = marketingAdminMapper.paidOrders();
        long paidOrders = paidObj == null ? 0 : paidObj;
        List<Map<String, Object>> funnel = new ArrayList<>();
        addStage(funnel, "业务订单", totalOrders);
        addStage(funnel, "已支付", paidOrders);
        addStage(funnel, "已完成", doneOrders);
        addStage(funnel, "升级申请", upgradeCount);
        addStage(funnel, "升级生效", effectiveUpgrades);
        for (int i = 0; i < funnel.size(); i++) {
            long v = ((Number) funnel.get(i).get("value")).longValue();
            long top = ((Number) funnel.get(0).get("value")).longValue();
            long prev = i == 0 ? v : ((Number) funnel.get(i - 1).get("value")).longValue();
            funnel.get(i).put("conversionFromTop", top == 0 ? 0 : Math.round(v * 1000.0 / top) / 10.0);
            funnel.get(i).put("conversionFromPrev", prev == 0 ? 0 : Math.round(v * 1000.0 / prev) / 10.0);
        }
        result.put("funnel", funnel);

        // ---- 套餐销量排行 ----
        long totalRevenue = ((Number) summary.getOrDefault("totalRevenue", 0)).longValue();
        List<Map<String, Object>> ranking = marketingAdminMapper.packageRanking();
        for (Map<String, Object> r : ranking) {
            long rev = ((Number) r.getOrDefault("revenue", 0)).longValue();
            r.put("ratio", totalRevenue == 0 ? 0 : Math.round(rev * 1000.0 / totalRevenue) / 10.0);
        }
        result.put("packageRanking", ranking);

        // ---- 业务类型分布 ----
        Map<String, String> typeLabels = Map.of(
                "NEW_INSTALL", "新装宽带", "MOVE", "宽带移机", "RENEW", "续费",
                "SPEED_UP", "宽带提速", "REPAIR", "故障报修", "ADDON", "加购");
        List<Map<String, Object>> typeDist = marketingAdminMapper.orderTypeDist();
        for (Map<String, Object> t : typeDist) {
            String type = String.valueOf(t.get("type"));
            t.put("typeLabel", typeLabels.getOrDefault(type, type));
        }
        result.put("orderTypeDist", typeDist);

        // ---- 升级单状态分布 ----
        Map<String, String> upLabels = Map.of("SUBMITTED", "待审核", "EFFECTIVE", "已生效", "REJECTED", "已驳回");
        List<Map<String, Object>> upgradeDist = marketingAdminMapper.upgradeByStatus();
        for (Map<String, Object> u : upgradeDist) {
            String s = String.valueOf(u.get("status"));
            u.put("statusLabel", upLabels.getOrDefault(s, s));
        }
        result.put("upgradeByStatus", upgradeDist);

        // ---- 客户分层分布 ----
        Map<String, String> levelLabels = Map.of("VIP", "五星", "GOLD", "四星", "SILVER", "三星", "NORMAL", "普通");
        List<Map<String, Object>> levelDist = marketingAdminMapper.customerLevelDist();
        for (Map<String, Object> l : levelDist) {
            String lv = String.valueOf(l.get("level"));
            l.put("levelLabel", levelLabels.getOrDefault(lv, lv));
        }
        result.put("customerLevelDist", levelDist);

        // ---- 近 6 月营收趋势 ----
        long threshold = LocalDate.now().minusMonths(5).withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        result.put("revenueTrend", marketingAdminMapper.revenueTrend(threshold));

        return result;
    }

    /**
     * 转化漏斗下钻：返回指定漏斗阶段的订单 / 升级单明细。
     * @param stage 业务订单 / 已支付 / 已完成 / 升级申请 / 升级生效
     */
    public List<Map<String, Object>> funnelDetail(String stage) {
        return switch (stage) {
            case "业务订单" -> marketingAdminMapper.orderDetailAll();
            case "已支付" -> marketingAdminMapper.orderDetailPaid();
            case "已完成" -> marketingAdminMapper.orderDetailDone();
            case "升级申请" -> marketingAdminMapper.upgradeDetailAll();
            case "升级生效" -> marketingAdminMapper.upgradeDetailEffective();
            default -> List.of();
        };
    }

    // ==================================================================== 工具

    private static void addStage(List<Map<String, Object>> funnel, String stage, long value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stage", stage);
        m.put("value", value);
        funnel.add(m);
    }
}
