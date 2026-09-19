package com.broadband.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 装维 SLA 履约看板聚合查询映射，SQL 落在 SlaDashboardMapper.xml。 */
@Mapper
public interface SlaDashboardMapper {

    long countSlaTotal();
    long countSlaMet();
    long countSlaOvertime();
    long countPendingComp();
    long sumCompTotal();
    long countSlaStatusInRange(@Param("status") String status, @Param("from") long from, @Param("to") long to);
    long sumCompInRange(@Param("from") long from, @Param("to") long to);
    long countCompInRange(@Param("from") long from, @Param("to") long to);
    Long avgResponseMinutes();
    List<Map<String, Object>> byType();
    List<Map<String, Object>> compByType();
    List<Map<String, Object>> recentCompensations();
    List<Map<String, Object>> heatmapRows();
    List<Map<String, Object>> overtimeDetail();
}
