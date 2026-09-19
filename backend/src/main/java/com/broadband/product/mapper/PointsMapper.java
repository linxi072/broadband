package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 积分成长体系数据访问。SQL 集中在 {@code resources/mapper/product/PointsMapper.xml}。
 */
@Mapper
public interface PointsMapper {

    /** 积分账户（余额 / 累计 / 连续签到）。 */
    Map<String, Object> selectAccount(@Param("customerId") String customerId);

    /** 签到更新（余额 + 累计 + 日期 + 连续天数）。 */
    int updateSign(@Param("customerId") String customerId, @Param("earned") int earned,
                   @Param("today") String today, @Param("streak") int streak);

    /** 兑换扣减（余额 - 累计支出）。 */
    int updateRedeem(@Param("customerId") String customerId, @Param("cost") int cost);

    /** 商品库存 -1。 */
    int updateStock(@Param("itemId") String itemId);

    /** 写入积分流水（type + 可选关联单 refId）。 */
    int insertRecord(@Param("id") String id, @Param("customerId") String customerId,
                     @Param("type") String type, @Param("amount") int amount,
                     @Param("remark") String remark, @Param("refId") String refId,
                     @Param("createdTime") long createdTime);

    /** 生成优惠券。 */
    int insertCoupon(@Param("id") String id, @Param("customerId") String customerId,
                     @Param("itemId") String itemId, @Param("couponCode") String couponCode,
                     @Param("couponType") Object couponType, @Param("couponValue") Object couponValue,
                     @Param("createdTime") long createdTime);

    /** 积分任务列表（仅启用）。 */
    List<Map<String, Object>> selectTasks();

    /** 积分商城商品列表（仅上架）。 */
    List<Map<String, Object>> selectMall();

    /** 兑换商品详情。 */
    Map<String, Object> selectItem(@Param("itemId") String itemId);

    /** 账户计数（自动开户判定）。 */
    Integer countAccount(@Param("customerId") String customerId);

    /** 自动开户。 */
    int insertAccount(@Param("customerId") String customerId, @Param("createdTime") long createdTime);
}
