package com.broadband.install.controller;

import com.broadband.install.service.AdminInstallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 安装工单与师傅容量管理（PC 后台）REST 接口。
 *
 * <ul>
 *   <li>GET  /api/admin/work-orders        —— 工单池（联表解析小区名 / 师傅名）</li>
 *   <li>GET  /api/admin/workers            —— 师傅列表（技能等级映射为 初/中/高级）</li>
 *   <li>POST /api/admin/worker-capacity    —— 按师傅 × 时段批量设置容量</li>
 * </ul>
 *
 * <p>本类只做权限校验、参数绑定与 HTTP 响应，业务逻辑见 {@link AdminInstallService}。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminInstallController {

    @Autowired private AdminInstallService adminInstallService;

    // ==================================================================== 工单池

    @GetMapping("/work-orders")
    @PreAuthorize("hasAuthority('workorder:view')")
    public List<Map<String, Object>> workOrders(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword) {
        return adminInstallService.workOrders(status, keyword);
    }

    /** 单条工单详情（师傅端工单详情页用）。 */
    @GetMapping("/work-orders/{id}")
    @PreAuthorize("hasAuthority('workorder:view')")
    public Map<String, Object> workOrder(@PathVariable String id) {
        return adminInstallService.workOrder(id);
    }

    // ==================================================================== 师傅

    @GetMapping("/workers")
    @PreAuthorize("hasAuthority('capacity:config')")
    public List<Map<String, Object>> workers() {
        return adminInstallService.workers();
    }

    /**
     * 师傅容量批量设置。
     * body: { workerId, day: 'yyyy-MM-dd', slots: [{ slotKey: 'AM', adjacentCap: 4, nonAdjacentCap: 2, enabled: true }] }
     */
    @PostMapping("/worker-capacity")
    @PreAuthorize("hasAuthority('capacity:config')")
    public Map<String, Object> saveCapacity(@RequestBody Map<String, Object> req) {
        return adminInstallService.saveCapacity(req);
    }
}
