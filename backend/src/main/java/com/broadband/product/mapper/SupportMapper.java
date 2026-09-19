package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 在线客服 / 帮助中心数据访问。SQL 集中在 {@code resources/mapper/product/SupportMapper.xml}。
 */
@Mapper
public interface SupportMapper {

    /** FAQ 列表，可按 category 过滤。 */
    List<Map<String, Object>> selectFaq(@Param("category") String category);

    /** 提交咨询工单。 */
    int insertTicket(Map<String, Object> m);

    /** 按客户 ID 取姓名（咨询工单署名）。 */
    String selectCustomerName(@Param("id") String id);
}
