package com.broadband.product.controller;

import com.broadband.product.service.ReviewService;
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
 * C 端评价 / 投诉接口（开放层，供小程序「订单评价」使用）。
 *
 * <ul>
 *   <li>POST /api/review/create —— 提交评价 / 投诉（关联 order_id）</li>
 *   <li>GET  /api/review/my     —— 按客户名查询自己的评价</li>
 *   <li>POST /api/review/submit/{reviewId} —— 回填系统推送的「待评价」</li>
 * </ul>
 *
 * <p>后台管理端评价列表见 AdminProductController 的 {@code /api/admin/reviews}（需 review:view 权限）。</p>
 *
 * <p>本类只做参数绑定与 HTTP 响应，业务逻辑见 {@link ReviewService}。</p>
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired private ReviewService reviewService;

    /**
     * 提交评价 / 投诉。
     * 入参 {orderId, customerName?, workerName?, score, tags?, type?, content?}。
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return reviewService.create(body);
    }

    /** 我的评价（按客户名过滤）。 */
    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestParam(required = false) String customerName) {
        return reviewService.my(customerName);
    }

    /**
     * 回填系统推送的「待评价」（TO_EVALUATE）：客户对完工服务评分，闭环评价。
     * 入参 {score, tags?, workerName?, content?}。
     */
    @PostMapping("/submit/{reviewId}")
    public Map<String, Object> submit(@PathVariable String reviewId,
                                      @RequestBody Map<String, Object> body) {
        return reviewService.submit(reviewId, body);
    }
}
