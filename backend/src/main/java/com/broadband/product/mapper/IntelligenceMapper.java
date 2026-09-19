package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 数据智能（客户分群 / 流失预警 / 营销自动化）聚合查询映射。
 * SQL 全部落在 {@code IntelligenceMapper.xml}，分群口径与 CustomerAdminMapper 同源（共享 CTE 片段）。
 */
@Mapper
public interface IntelligenceMapper {

    /** 六类分群计数（与聚合口径一致）。 */
    List<Map<String, Object>> segmentCounts();

    /** 流失预警客户（CHURN_RISK / RENEW）。 */
    List<Map<String, Object>> churnList(@Param("limit") int limit);

    /** 分群客户明细下钻。 */
    List<Map<String, Object>> segmentCustomers(@Param("segment") String segment, @Param("limit") int limit);

    /** 营销自动化规则列表（含执行次数统计）。 */
    List<Map<String, Object>> campaigns(@Param("since") long since);

    /** 已启用的营销规则。 */
    List<Map<String, Object>> enabledCampaigns();

    /** 某客户在某规则下的去重计数（30 天内）。 */
    Long countExecDup(@Param("campaignId") String campaignId,
                      @Param("customerId") String customerId,
                      @Param("since") long since);

    /** 营销目标商品（券类型 / 面值）。 */
    Map<String, Object> mallItem(@Param("id") String id);

    /** 发放券。 */
    int insertCoupon(@Param("id") String id,
                     @Param("customerId") String customerId,
                     @Param("itemId") String itemId,
                     @Param("code") String code,
                     @Param("couponType") String couponType,
                     @Param("couponValue") String couponValue,
                     @Param("createdTime") long createdTime,
                     @Param("expireTime") long expireTime);

    /** 营销执行留痕。 */
    int insertExec(@Param("id") String id,
                   @Param("campaignId") String campaignId,
                   @Param("customerId") String customerId,
                   @Param("actionType") String actionType,
                   @Param("targetItem") String targetItem,
                   @Param("status") String status,
                   @Param("remark") String remark,
                   @Param("createdTime") long createdTime);

    /** 命中某分群的全部客户 id。 */
    List<Map<String, Object>> targetCustomers(@Param("segment") String segment);
}
