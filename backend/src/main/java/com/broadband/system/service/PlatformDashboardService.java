package com.broadband.system.service;

import com.broadband.common.RedisCacheService;
import com.broadband.system.mapper.PlatformDashboardMapper;
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
 * 平台级运营聚合：数据看板 / 业务订单列表 / 流量总览。数据访问通过 {@link PlatformDashboardMapper}（SQL 在 XML）。
 *
 * <p>放在 system 模块：这些指标天然是「跨业务域聚合」，不属于任何一个业务模块；
 * 用 Mapper 直接做读侧聚合，避免让业务模块互相依赖（保持各模块单向可拆）。</p>
 */
@Service
public class PlatformDashboardService {

    @Autowired private PlatformDashboardMapper platformDashboardMapper;
    @Autowired private SysDepartmentService departmentService;
    @Autowired private RedisCacheService cache;

    // ==================================================================== 数据看板

    /** 看板聚合：订单 / 营收 / 待装 / 履约 + SLA 指标 + 7 日订单趋势（短 TTL 缓存，DB 直查退化解）。 */
    public Map<String, Object> dashboardStats() {
        return cache.get("dash:stats", Duration.ofSeconds(60), this::computeDashboardStats);
    }

    /** 看板聚合计算（缓存未命中时执行）。 */
    private Map<String, Object> computeDashboardStats() {
        long monthStart = monthStartMillis();
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("monthOrders", z(platformDashboardMapper.countBizOrderSince(monthStart)));
        out.put("revenue", z(platformDashboardMapper.sumBizOrderRevenueSince(monthStart)));
        out.put("pendingInstall", platformDashboardMapper.countPendingInstall());

        long slaTotal = z(platformDashboardMapper.countSlaSince(monthStart));
        long slaMet = z(platformDashboardMapper.countSlaMetSince(monthStart));
        double slaRate = slaTotal == 0 ? 100.0 : round1(slaMet * 100.0 / slaTotal);
        out.put("slaRate", slaRate);
        out.put("fulfillmentRate", slaRate);

        out.put("slowPay", z(platformDashboardMapper.countCompSince(monthStart)));
        out.put("avgResponse", z(platformDashboardMapper.avgResponseMinutes()));
        out.put("monthComp", z(platformDashboardMapper.sumCompSince(monthStart)));

        // 近 7 日订单量与日期标签（按自然日分桶，避免依赖具体数据库的日期函数）
        List<Integer> trend = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long from = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long to = d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            trend.add((int) z(platformDashboardMapper.countBizOrderInRange(from, to)));
            labels.add(String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth()));
        }
        out.put("orderTrend", trend);
        out.put("orderTrendLabels", labels);
        return out;
    }

    /** 看板「最近订单」。 */
    public List<Map<String, Object>> recentOrders(int limit) {
        return platformDashboardMapper.recentOrders(Math.max(1, Math.min(limit, 50)));
    }

    /**
     * 订单列表（同时服务「订单管理」页与看板明细，属同一读侧聚合）。
     * 运营人员按部门做行级数据隔离，管理员（deptId 为空）看全部。
     */
    public List<Map<String, Object>> orders(String status, String keyword) {
        return platformDashboardMapper.orders(status, keyword, dataScopeDeptIds());
    }

    // ==================================================================== 流量总览

    public Map<String, Object> trafficOverview() {
        String month = LocalDate.now().toString().substring(0, 7);
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("activeCustomers", platformDashboardMapper.countActiveCustomers());
        long poolTotal = platformDashboardMapper.sumTrafficPool(month);
        out.put("monthPool", poolTotal >= 1000 ? String.format("%.1f TB", poolTotal / 1024.0) : poolTotal + " GB");
        out.put("overCustomers", platformDashboardMapper.countOverCustomers(month));
        out.put("alarming", platformDashboardMapper.countAlarming(month));

        List<Map<String, Object>> top = platformDashboardMapper.trafficTop(month);
        for (Map<String, Object> row : top) {
            long used = num(row.get("used"));
            long total = num(row.get("total"));
            row.put("status", total > 0 && total - used <= 5 ? "预警" : "正常");
        }
        out.put("top", top);
        return out;
    }

    // ==================================================================== 工具

    /** 当前登录用户的数据权限部门集合；null 表示不限定（看全部）。 */
    private List<String> dataScopeDeptIds() {
        var me = CurrentUser.get();
        if (me == null || me.user.deptId == null || me.user.deptId.isEmpty()) return null;
        return departmentService.visibleDeptIds(me.user.deptId);
    }

    private long monthStartMillis() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
    }

    private static long z(Long v) {
        return v == null ? 0 : v;
    }

    private static long num(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
