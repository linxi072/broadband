package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 套餐营销看板（PC 后台读侧聚合）映射，SQL 落在 MarketingAdminMapper.xml。 */
@Mapper
public interface MarketingAdminMapper {

    /** 汇总指标（单值行）。 */
    Map<String, Object> summary();

    /** 已支付订单数（业务订单 → 已支付漏斗阶段）。 */
    Long paidOrders();

    /** 套餐销量排行。 */
    List<Map<String, Object>> packageRanking();

    /** 业务类型分布。 */
    List<Map<String, Object>> orderTypeDist();

    /** 升级单状态分布。 */
    List<Map<String, Object>> upgradeByStatus();

    /** 客户分层分布。 */
    List<Map<String, Object>> customerLevelDist();

    /** 近 6 月营收趋势。 */
    List<Map<String, Object>> revenueTrend(@Param("threshold") long threshold);

    /** 漏斗下钻：业务订单（全部）。 */
    List<Map<String, Object>> orderDetailAll();

    /** 漏斗下钻：业务订单（已支付）。 */
    List<Map<String, Object>> orderDetailPaid();

    /** 漏斗下钻：业务订单（已完成）。 */
    List<Map<String, Object>> orderDetailDone();

    /** 漏斗下钻：升级申请（全部）。 */
    List<Map<String, Object>> upgradeDetailAll();

    /** 漏斗下钻：升级申请（已生效）。 */
    List<Map<String, Object>> upgradeDetailEffective();
}
