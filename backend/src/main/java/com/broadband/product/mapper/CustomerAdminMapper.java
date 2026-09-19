package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 客户列表与客户 360 数据访问。SQL 集中在 {@code resources/mapper/product/CustomerAdminMapper.xml}。
 * 分群口径（CTE + CASE）与「数据智能」模块一致，避免两处口径分歧。
 */
@Mapper
public interface CustomerAdminMapper {

    /** 客户列表（含套餐 / 合约 / 小区 / 标签计算列）。 */
    List<Map<String, Object>> selectCustomers(@Param("keyword") String keyword);

    /** 客户档案（客户 360 之 profile）。 */
    Map<String, Object> selectProfile(@Param("id") String id);

    /** 业务订单（客户 360 之 orders）。 */
    List<Map<String, Object>> selectOrders(@Param("id") String id);

    /** 合约（客户 360 之 contracts）。 */
    List<Map<String, Object>> selectContracts(@Param("id") String id);

    /** 流量（客户 360 之 traffic，取最近周期）。 */
    Map<String, Object> selectTraffic(@Param("id") String id);

    /** 投诉与评价（客户 360 之 reviews，按客户名）。 */
    List<Map<String, Object>> selectReviews(@Param("name") String name);

    /** 安装工单（客户 360 之 workOrders，按业务订单客户或客户名）。 */
    List<Map<String, Object>> selectWorkOrders(@Param("id") String id, @Param("name") String name);

    /** 升级申请单（客户 360 之 upgradeOrders）。 */
    List<Map<String, Object>> selectUpgradeOrders(@Param("id") String id);

    /** 汇总指标（客户 360 之 summary）。 */
    Map<String, Object> selectSummary(@Param("id") String id, @Param("name") String name);

    /** 单客户分群判定（CTE + CASE，与数据智能模块一致）。 */
    Map<String, Object> selectCustomerSegment(@Param("id") String id);
}
