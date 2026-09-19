package com.broadband.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 平台级运营聚合（数据看板 / 订单 / 流量）查询映射，SQL 落在 PlatformDashboardMapper.xml。 */
@Mapper
public interface PlatformDashboardMapper {

    long countBizOrderSince(@Param("ms") long ms);
    long sumBizOrderRevenueSince(@Param("ms") long ms);
    long countPendingInstall();
    long countSlaSince(@Param("ms") long ms);
    long countSlaMetSince(@Param("ms") long ms);
    long countCompSince(@Param("ms") long ms);
    long sumCompSince(@Param("ms") long ms);
    long countBizOrderInRange(@Param("from") long from, @Param("to") long to);
    long countActiveCustomers();
    long sumTrafficPool(@Param("month") String month);
    long countOverCustomers(@Param("month") String month);
    long countAlarming(@Param("month") String month);
    Long avgResponseMinutes();
    List<Map<String, Object>> recentOrders(@Param("limit") int limit);
    List<Map<String, Object>> orders(@Param("status") String status, @Param("keyword") String keyword,
                                     @Param("deptIds") List<String> deptIds);
    List<Map<String, Object>> trafficTop(@Param("month") String month);
}
