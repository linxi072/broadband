package com.broadband.product.service;

import com.broadband.product.mapper.PromotionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 优惠活动专区（C 端，V1.14 运营留存）：仅暴露已上线（ONLINE）活动。数据访问通过 {@link PromotionMapper}（SQL 在 XML）。
 */
@Service
public class PromotionService {

    @Autowired private PromotionMapper promotionMapper;

    /** 活动列表，可按 type 过滤。 */
    public List<Map<String, Object>> list(String type) {
        return promotionMapper.list(type == null ? null : type);
    }

    /** 活动详情；不存在返回空 Map。 */
    public Map<String, Object> detail(String id) {
        Map<String, Object> row = promotionMapper.detail(id);
        return row == null ? Map.of() : row;
    }
}
