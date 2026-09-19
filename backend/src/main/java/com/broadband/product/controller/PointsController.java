package com.broadband.product.controller;

import com.broadband.product.service.PointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 积分成长体系（C 端小程序，V1.14 运营留存）。
 *
 * <ul>
 *   <li>GET  /api/points/balance —— 积分余额 + 连续签到天数 + 今日是否已签</li>
 *   <li>POST /api/points/sign    —— 每日签到（幂等：同日重复签到不重复加积分）</li>
 *   <li>GET  /api/points/tasks   —— 积分任务列表</li>
 *   <li>GET  /api/points/mall    —— 积分商城商品列表</li>
 *   <li>POST /api/points/redeem  —— 兑换商品（生成优惠券、扣减积分）</li>
 * </ul>
 *
 * <p>本类只做参数绑定与 HTTP 响应，业务逻辑见 {@link PointsService}。</p>
 */
@RestController
@RequestMapping("/api/points")
public class PointsController {

    @Autowired private PointsService pointsService;

    /** 积分余额概览。入参 customerId。 */
    @GetMapping("/balance")
    public Map<String, Object> balance(@RequestParam String customerId) {
        return pointsService.balance(customerId);
    }

    /** 每日签到（幂等）。 */
    @PostMapping("/sign")
    public Map<String, Object> sign(@RequestParam String customerId) {
        return pointsService.sign(customerId);
    }

    /** 积分任务列表（任务为全局配置，customerId 仅用于前端上下文透传）。 */
    @GetMapping("/tasks")
    public List<Map<String, Object>> tasks(@RequestParam(required = false) String customerId) {
        return pointsService.tasks();
    }

    /** 积分商城商品列表。 */
    @GetMapping("/mall")
    public List<Map<String, Object>> mall() {
        return pointsService.mall();
    }

    /** 兑换商品：扣积分 + 生成优惠券。 */
    @PostMapping("/redeem")
    public Map<String, Object> redeem(@RequestParam String customerId, @RequestParam String itemId) {
        return pointsService.redeem(customerId, itemId);
    }
}
