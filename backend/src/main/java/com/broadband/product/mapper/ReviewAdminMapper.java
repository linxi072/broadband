package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 投诉与评价（PC 后台）查询 / 写映射，SQL 落在 ReviewAdminMapper.xml。 */
@Mapper
public interface ReviewAdminMapper {

    /** 评价 / 投诉列表（可按类型 / 状态过滤）。 */
    List<Map<String, Object>> reviews(@Param("type") String type, @Param("status") String status);

    /** 闭环投诉 / 评价。 */
    int closeReview(@Param("id") String id);
}
