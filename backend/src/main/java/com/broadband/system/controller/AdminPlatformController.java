package com.broadband.system.controller;

import com.broadband.system.service.MonitorService;
import com.broadband.system.service.PlatformDashboardService;
import com.broadband.system.service.SlaDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 平台级运营聚合接口（数据看板 / 流量总览 / 性能监控 / SLA 履约看板）。
 *
 * <p>放在 system 模块：这些指标天然是「跨业务域聚合」，不属于任何一个业务模块。
 * 本类只做权限校验、参数绑定与 HTTP 响应，聚合逻辑分散到：</p>
 * <ul>
 *   <li>{@link PlatformDashboardService} —— 看板 / 订单 / 流量总览</li>
 *   <li>{@link MonitorService} —— 应用 + JVM + 连接池监控</li>
 *   <li>{@link SlaDashboardService} —— SLA 履约看板与超时热力下钻</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminPlatformController {

    @Autowired private PlatformDashboardService platformDashboardService;
    @Autowired private MonitorService monitorService;
    @Autowired private SlaDashboardService slaDashboardService;

    // ==================================================================== 数据看板

    /** 看板聚合：订单/营收/待装/履约 + SLA 指标 + 7 日订单趋势。 */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Map<String, Object> dashboardStats() {
        return platformDashboardService.dashboardStats();
    }

    /** 看板「最近订单」。 */
    @GetMapping("/dashboard/recent-orders")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public List<Map<String, Object>> recentOrders(@RequestParam(defaultValue = "6") int limit) {
        return platformDashboardService.recentOrders(limit);
    }

    /** 订单列表（同时服务「订单管理」页与看板明细）。 */
    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('order:view')")
    public List<Map<String, Object>> orders(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword) {
        return platformDashboardService.orders(status, keyword);
    }

    // ==================================================================== 流量总览

    @GetMapping("/traffic/overview")
    @PreAuthorize("hasAuthority('traffic:view')")
    public Map<String, Object> trafficOverview() {
        return platformDashboardService.trafficOverview();
    }

    // ==================================================================== 性能监控

    @GetMapping("/monitor/overview")
    @PreAuthorize("hasAuthority('monitor:view')")
    public Map<String, Object> monitorOverview() {
        return monitorService.monitorOverview();
    }

    // ==================================================================== 装维 SLA 履约看板

    /** SLA 履约看板：汇总指标 + 分类型达标率 + 近 14 日超时分布 + 近 6 月赔付趋势 + 最近赔付。 */
    @GetMapping("/sla/dashboard")
    @PreAuthorize("hasAuthority('sla:view')")
    public Map<String, Object> slaDashboard() {
        return slaDashboardService.slaDashboard();
    }

    /**
     * SLA 超时热力下钻：返回指定「星期 × 时段」单元的超时工单明细。
     * 前端点击热力图单元格时调用（dayOfWeek 1..7 周一为 1；hour 0..23）。
     */
    @GetMapping("/sla/overtime-detail")
    @PreAuthorize("hasAuthority('sla:view')")
    public List<Map<String, Object>> slaOvertimeDetail(@RequestParam int dayOfWeek, @RequestParam int hour) {
        return slaDashboardService.slaOvertimeDetail(dayOfWeek, hour);
    }
}
