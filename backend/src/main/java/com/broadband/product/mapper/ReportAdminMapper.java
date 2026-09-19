package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 销售 / 财务报表数据访问。SQL 集中在 {@code resources/mapper/product/ReportAdminMapper.xml}。
 */
@Mapper
public interface ReportAdminMapper {

    /** 销售业绩报表（按销售 × 月份聚合）。 */
    List<Map<String, Object>> selectSalesReport();

    /** 销售业绩明细下钻（按销售 / 月份）。 */
    List<Map<String, Object>> selectSalesDetail(@Param("salesName") String salesName, @Param("month") String month);

    /** 财务月报（营收 / 退款 / 取消 / 应收 / 订单量）。 */
    List<Map<String, Object>> selectFinanceReport();

    /** 各月赔付金额（用于月报 enrichment）。 */
    List<Map<String, Object>> selectCompensationByMonth();

    /** 财务月度明细下钻：底层业务订单。 */
    List<Map<String, Object>> selectFinanceOrders(@Param("month") String month);

    /** 财务月度明细下钻：赔付工单。 */
    List<Map<String, Object>> selectFinanceCompensations(@Param("month") String month);
}
