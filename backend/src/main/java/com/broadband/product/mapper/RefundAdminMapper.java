package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 退款 / 对账状态机（PC 后台）查询 / 写映射，SQL 落在 RefundAdminMapper.xml。 */
@Mapper
public interface RefundAdminMapper {

    /** 退款单列表，可选按状态过滤。 */
    List<Map<String, Object>> refunds(@Param("status") String status);

    /** 退款单详情（审批前校验）。 */
    Map<String, Object> refundById(@Param("id") String id);

    /** 审批通过：置 REFUNDED 并生成流水号。 */
    int approveRefund(@Param("id") String id, @Param("refundNo") String refundNo,
                      @Param("operator") String operator, @Param("handledTime") long handledTime);

    /** 审批通过：关联业务订单置 REFUND（营收扣减）。 */
    int markOrderRefunded(@Param("orderId") String orderId);

    /** 驳回退款。 */
    int rejectRefund(@Param("id") String id, @Param("operator") String operator,
                     @Param("handledTime") long handledTime);
}
