package com.broadband.install.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 安装工单与师傅容量（PC 后台）数据访问。SQL 集中在 {@code resources/mapper/install/AdminInstallMapper.xml}。
 */
@Mapper
public interface AdminInstallMapper {

    /** 工单池（联表解析小区名/师傅名），按部门集合做行级数据隔离；status/keyword 为空时不过滤。 */
    List<Map<String, Object>> selectWorkOrders(@Param("status") String status,
                                               @Param("keyword") String keyword,
                                               @Param("scope") List<String> scope);

    /** 单条工单详情。 */
    Map<String, Object> selectWorkOrder(@Param("id") String id);

    /** 师傅列表（技能等级映射为 初/中/高级）。 */
    List<Map<String, Object>> selectWorkers();

    /** 删除某师傅某时段的容量配置。 */
    int deleteCapacity(@Param("workerId") String workerId, @Param("timeSlot") String timeSlot);

    /** 新增/更新师傅时段容量。 */
    int upsertCapacity(@Param("workerId") String workerId, @Param("timeSlot") String timeSlot,
                       @Param("adj") int adj, @Param("non") int non);
}
