package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 优惠活动专区（C 端）查询映射，SQL 落在 PromotionMapper.xml。 */
@Mapper
public interface PromotionMapper {

    /** 活动列表，可按 type 过滤（仅上线活动）。 */
    List<Map<String, Object>> list(@Param("type") String type);

    /** 活动详情。 */
    Map<String, Object> detail(@Param("id") String id);
}
