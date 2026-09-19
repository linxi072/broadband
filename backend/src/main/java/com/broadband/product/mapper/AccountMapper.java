package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 账户与账单中心数据访问。SQL 集中在 {@code resources/mapper/product/AccountMapper.xml}。
 */
@Mapper
public interface AccountMapper {

    /** 客户基础信息。 */
    Map<String, Object> selectCustomer(@Param("id") String id);

    /** 积分余额（COALESCE 兜底 0）。 */
    Map<String, Object> selectPoints(@Param("customerId") String customerId);

    /** 本月消费（指定状态、时间区间）。 */
    Integer selectMonthConsume(@Param("customerId") String customerId,
                               @Param("start") long start, @Param("end") long end);

    /** 最近一条 ACTIVE 合约到期日。 */
    Map<String, Object> selectContractEnd(@Param("customerId") String customerId);

    /** 账单明细（可按账期 yyyy-MM 过滤）。 */
    List<Map<String, Object>> selectBills(@Param("customerId") String customerId, @Param("period") String period);
}
