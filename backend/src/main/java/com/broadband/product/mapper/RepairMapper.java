package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 故障报修数据访问。SQL 集中在 {@code resources/mapper/product/RepairMapper.xml}。
 */
@Mapper
public interface RepairMapper {

    /** 我的报修列表（按客户查其 REPAIR 业务订单关联的工单）。 */
    List<Map<String, Object>> selectMy(@Param("customerId") String customerId);

    /** 报修详情（含社区 / 师傅 / 业务订单信息）。 */
    List<Map<String, Object>> selectById(@Param("id") String id);

    /** SLA 评估记录（按工单 / 业务订单）。 */
    List<Map<String, Object>> selectSla(@Param("id") String id);

    /** 报修提交前的客户档案查询。 */
    Map<String, Object> selectCustomer(@Param("id") String id);

    /** 报修提交前的小区查询（取名称与部门）。 */
    Map<String, Object> selectCommunity(@Param("id") String id);

    /** 撤销前的工单状态查询。 */
    Map<String, Object> selectWorkOrderStatus(@Param("id") String id);

    /** 故障类型字典标签（用于工单描述）。 */
    String selectFaultLabel(@Param("value") String value);

    /** 创建报修业务订单。 */
    int insertBizOrder(Map<String, Object> m);

    /** 创建报修安装工单。 */
    int insertWorkOrder(Map<String, Object> m);

    /** 更新工单状态。 */
    int updateWorkOrderStatus(@Param("id") String id, @Param("status") String status);

    /** 更新业务订单状态。 */
    int updateBizOrderStatus(@Param("id") String id, @Param("status") String status);
}
