package com.broadband.product.controller;

import com.broadband.product.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 故障报修（C 端小程序）。V1.13 全流程：客户提交报修 → 生成报修工单(REPAIR) → 派单 → 师傅上门 → 完工 → SLA 评估。
 *
 * <ul>
 *   <li>POST /api/repair/create   —— 提交报修（生成 biz_order(REPAIR) + work_order(REPAIR)）</li>
 *   <li>GET  /api/repair/my       —— 我的报修列表（含进度）</li>
 *   <li>GET  /api/repair/{id}     —— 报修详情（工单 + 师傅 + SLA + 时间线）</li>
 *   <li>POST /api/repair/{id}/cancel —— 撤销报修（仅 PENDING 未派单可撤）</li>
 * </ul>
 *
 * <p>本类只做参数绑定与 HTTP 响应，业务逻辑见 {@link RepairService}。</p>
 */
@RestController
@RequestMapping("/api/repair")
public class RepairController {

    @Autowired private RepairService repairService;

    /**
     * 提交报修。入参 {customerId, communityId?, faultCategory, faultDesc, contactPhone?, timeSlot?}。
     * 返回 {ok, repairId, orderId, status}。
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return repairService.create(body);
    }

    /** 我的报修列表（按客户查其 REPAIR 业务订单关联的工单）。 */
    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestParam String customerId) {
        return repairService.my(customerId);
    }

    /** 报修详情（含 SLA 评估与进度时间线）。 */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        return repairService.detail(id);
    }

    /** 撤销报修：仅 PENDING（未派单）可撤，同步置 biz_order 为 CANCELLED。 */
    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable String id) {
        return repairService.cancel(id);
    }
}
