package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * C 端客户订单数据访问。SQL 集中在 {@code resources/mapper/product/OrderMapper.xml}。
 */
@Mapper
public interface OrderMapper {

    /** 我的订单。 */
    List<Map<String, Object>> selectMy(@Param("customerId") String customerId);

    /** 套餐查询（下单金额取 monthly_fee）。 */
    Map<String, Object> selectPackage(@Param("packageId") String packageId);

    /** 小区查询（取名称与部门）。 */
    Map<String, Object> selectCommunity(@Param("communityId") String communityId);

    /** 客户查询（取姓名与电话）。 */
    Map<String, Object> selectCustomer(@Param("customerId") String customerId);

    /** 创建业务订单。 */
    int insertOrder(Map<String, Object> m);

    /** 订单状态查询（支付 / 退款前置校验）。 */
    Map<String, Object> selectOrderStatus(@Param("orderId") String orderId);

    /** 关联工单查询（支付幂等判定）。 */
    String selectWorkOrderId(@Param("orderId") String orderId);

    /** 订单金额查询（支付扣款）。 */
    Integer selectAmount(@Param("orderId") String orderId);

    /** 支付成功：订单置 PAID。 */
    int updatePaid(@Param("orderId") String orderId);

    /** 创建关联安装工单。 */
    int insertWorkOrder(Map<String, Object> m);

    /** 退款前置：订单完整信息。 */
    Map<String, Object> selectOrderForRefund(@Param("orderId") String orderId);

    /** 退款幂等：已存在 PENDING/REFUNDED 退款单。 */
    String selectExistingRefund(@Param("orderId") String orderId);

    /** 创建退款单。 */
    int insertRefund(Map<String, Object> m);

    /** 订单全链路：工单信息。 */
    Map<String, Object> selectWorkOrderByBiz(@Param("orderId") String orderId);

    /** 订单全链路：SLA 评估。 */
    List<Map<String, Object>> selectSla(@Param("orderId") String orderId);

    /** 订单全链路：评价。 */
    List<Map<String, Object>> selectReviews(@Param("orderId") String orderId);
}
