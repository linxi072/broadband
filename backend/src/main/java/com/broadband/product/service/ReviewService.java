package com.broadband.product.service;

import com.broadband.common.Values;
import com.broadband.product.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * C 端评价 / 投诉业务：提交评价、查询我的评价、回填系统推送的「待评价」。
 *
 * <p>后台管理端评价列表见 {@link ReviewAdminService}（需 review:view 权限）。数据访问经 {@link ReviewMapper}。</p>
 */
@Service
public class ReviewService {

    @Autowired private ReviewMapper reviewMapper;

    /** 提交评价 / 投诉；返回 {ok, id, status}。 */
    public Map<String, Object> create(Map<String, Object> body) {
        String orderId = Values.str(body.get("orderId"));
        String customerName = Values.str(body.get("customerName"), "匿名客户");
        String workerName = Values.str(body.get("workerName"));
        Integer score = Values.toInt(body.get("score"));
        String tags = Values.str(body.get("tags"));
        String type = Values.str(body.get("type"), "REVIEW");
        String content = Values.str(body.get("content"));

        if (Values.isBlank(orderId)) throw new IllegalArgumentException("orderId 必填");
        if (score == null || score < 0 || score > 5) throw new IllegalArgumentException("评分需在 0-5 之间");

        String id = "RV" + System.currentTimeMillis();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("orderId", orderId);
        m.put("customerName", customerName);
        m.put("workerName", workerName);
        m.put("score", score);
        m.put("tags", tags);
        m.put("type", type);
        m.put("content", content);
        m.put("status", "PENDING");
        m.put("createdTime", System.currentTimeMillis());
        reviewMapper.insertReview(m);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("status", "PENDING");
        return resp;
    }

    /** 我的评价（按客户名过滤）。 */
    public List<Map<String, Object>> my(String customerName) {
        if (Values.isBlank(customerName)) return List.of();
        return reviewMapper.selectMy(customerName);
    }

    /**
     * 回填系统推送的「待评价」（TO_EVALUATE）：客户对完工服务评分，闭环评价。
     * @throws IllegalArgumentException 评价不存在或评分越界
     * @throws IllegalStateException    评价已提交 / 不可编辑
     */
    public Map<String, Object> submit(String reviewId, Map<String, Object> body) {
        Map<String, Object> rev = reviewMapper.selectReviewStatus(reviewId);
        if (rev == null) throw new IllegalArgumentException("评价不存在：" + reviewId);
        if (!"TO_EVALUATE".equals(rev.get("status")))
            throw new IllegalStateException("该评价已提交或不可编辑，当前：" + rev.get("status"));
        Integer score = Values.toInt(body.get("score"));
        if (score == null || score < 0 || score > 5) throw new IllegalArgumentException("评分需在 0-5 之间");
        String tags = Values.str(body.get("tags"));
        String content = Values.str(body.get("content"));
        String workerName = Values.str(body.get("workerName"));
        String customerName = Values.str(body.get("customerName"));
        reviewMapper.updateSubmit(reviewId, score, tags, content, workerName, customerName, System.currentTimeMillis());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", reviewId);
        resp.put("status", "PENDING");
        return resp;
    }
}
