package com.broadband.install.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 师傅端聚合查询数据访问（与 {@link WorkerMapper} 实体 CRUD 分离）。
 * SQL 集中在 {@code resources/mapper/install/WorkerQueryMapper.xml}。
 * 所有查询均按 {@code workerId} 做数据隔离（师傅端传入登录师傅，后台传 null 表示不限）。
 */
@Mapper
public interface WorkerQueryMapper {

    /** 工单列表（含小区/师傅联表与计算列）。workerId/status 为空时不过滤。 */
    List<Map<String, Object>> selectOrders(@Param("workerId") String workerId, @Param("status") String status);

    /** 工单详情；workerId 非空时追加归属过滤（越权查不到）。 */
    Map<String, Object> selectOrderById(@Param("id") String id, @Param("workerId") String workerId);

    /** 待上门数：时段 LIKE prefix 且状态 ASSIGNED/INSTALLING。 */
    int countPending(@Param("prefix") String prefix, @Param("workerId") String workerId);

    /** 当日已完成数：状态 DONE 且完工日期 = day。 */
    int countDoneOnDate(@Param("day") String day, @Param("workerId") String workerId);

    /** 累计工单数（可按师傅过滤）。 */
    int countTotal(@Param("workerId") String workerId);

    /** 师傅时段容量配置（worker_capacity）。 */
    List<Map<String, Object>> selectCapacity(@Param("workerId") String workerId, @Param("prefix") String prefix);

    /** 各时段已派工单计数（GROUP BY time_slot）。 */
    List<Map<String, Object>> selectUsedBySlot(@Param("prefix") String prefix, @Param("workerId") String workerId);

    /** 未来排班：各时段工单计数（time_slot >= from）。 */
    List<Map<String, Object>> selectSchedule(@Param("from") String from, @Param("workerId") String workerId);

    /** 完工：置 DONE 并落测速/签名/服务项/完工时间。 */
    int updateComplete(@Param("id") String id, @Param("down") Integer down, @Param("up") Integer up,
                       @Param("sign") String sign, @Param("services") String services);

    /** 读取工单对应的业务订单 id。 */
    String selectBizOrderId(@Param("id") String id);

    /** 更新业务订单状态。 */
    int updateBizOrderStatus(@Param("id") String id, @Param("status") String status);

    /** 查询是否已存在该业务订单的待评价记录。 */
    String selectReviewId(@Param("bizId") String bizId);

    /** 生成待评价记录。 */
    int insertReview(@Param("reviewId") String reviewId, @Param("bizId") String bizId,
                     @Param("custName") String custName, @Param("workerName") String workerName,
                     @Param("createdTime") long createdTime);

    /** 读取工单完工时间。 */
    String selectCompleteTime(@Param("id") String id);

    /** 开始施工：ASSIGNED -> INSTALLING。 */
    int updateStart(@Param("id") String id);

    /** 完工时取工单基础信息供 SLA 评估。 */
    Map<String, Object> selectWorkOrderForSla(@Param("id") String id);

    /** 取业务订单类型与创建时间。 */
    Map<String, Object> selectBizOrder(@Param("id") String id);

    /** 取工单下行测速速率。 */
    Integer selectDownSpeed(@Param("id") String id);
}
