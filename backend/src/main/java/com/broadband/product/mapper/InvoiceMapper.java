package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 电子发票数据访问。SQL 集中在 {@code resources/mapper/product/InvoiceMapper.xml}。
 */
@Mapper
public interface InvoiceMapper {

    /** 开票前的订单校验查询。 */
    Map<String, Object> selectOrder(@Param("id") String id);

    /** 发票申请详情（含状态与金额）。 */
    Map<String, Object> selectInvoice(@Param("id") String id);

    /** 查询同订单是否已存在 PENDING/OPENED 申请（幂等校验）。 */
    String selectExistingId(@Param("orderId") String orderId);

    /** 我的发票申请（按客户名）。 */
    List<Map<String, Object>> selectMy(@Param("customerName") String name);

    /** 发票申请列表（可按状态过滤）。 */
    List<Map<String, Object>> selectList(@Param("status") String status);

    /** 创建发票申请。 */
    int insertApply(Map<String, Object> m);

    /** 开具发票（PENDING -> OPENED）。 */
    int updateOpen(@Param("id") String id, @Param("invoiceNo") String invoiceNo,
                   @Param("pdfUrl") String pdfUrl, @Param("operator") String operator,
                   @Param("openedTime") Long openedTime);

    /** 驳回发票（PENDING -> REJECTED）。 */
    int updateReject(@Param("id") String id, @Param("remark") String remark,
                     @Param("operator") String operator, @Param("openedTime") Long openedTime);
}
