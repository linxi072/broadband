package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * C 端评价 / 投诉数据访问。SQL 集中在 {@code resources/mapper/product/ReviewMapper.xml}。
 */
@Mapper
public interface ReviewMapper {

    /** 创建评价 / 投诉。 */
    int insertReview(Map<String, Object> m);

    /** 我的评价（按客户名）。 */
    List<Map<String, Object>> selectMy(@Param("customerName") String customerName);

    /** 回填「待评价」（TO_EVALUATE -> PENDING）。 */
    int updateSubmit(@Param("id") String id, @Param("score") Integer score, @Param("tags") String tags,
                     @Param("content") String content, @Param("workerName") String workerName,
                     @Param("customerName") String customerName, @Param("createdTime") Long createdTime);

    /** 评价状态查询（提交 / 驳回前校验）。 */
    Map<String, Object> selectReviewStatus(@Param("id") String id);
}
