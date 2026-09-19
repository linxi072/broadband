package com.broadband.product.controller;

import com.broadband.product.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 优惠活动专区（C 端小程序，V1.14 运营留存）。
 *
 * <ul>
 *   <li>GET /api/promotions      —— 活动列表（仅上线的）</li>
 *   <li>GET /api/promotions/{id} —— 活动详情</li>
 * </ul>
 *
 * <p>本类只做参数绑定与 HTTP 响应，业务逻辑见 {@link PromotionService}。</p>
 */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Autowired private PromotionService promotionService;

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String type) {
        return promotionService.list(type);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        return promotionService.detail(id);
    }
}
