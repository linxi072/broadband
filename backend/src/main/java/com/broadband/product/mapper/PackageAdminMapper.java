package com.broadband.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 套餐与套餐升级单（PC 后台）查询 / 写映射，SQL 落在 PackageAdminMapper.xml。 */
@Mapper
public interface PackageAdminMapper {

    /** 套餐列表（含主图 / 轮播数 / 参数组数等统计列）。 */
    List<Map<String, Object>> packages();

    /** 套餐升级申请单列表。 */
    List<Map<String, Object>> upgradeOrders();

    /** 套餐主表 upsert。 */
    int upsertPackage(@Param("id") String id, @Param("name") String name, @Param("category") String category,
                      @Param("monthlyFee") Integer monthlyFee, @Param("originalFee") Integer originalFee,
                      @Param("status") String status, @Param("deposit") Integer deposit,
                      @Param("deviceRent") Integer deviceRent, @Param("penalty") String penalty,
                      @Param("slaInfo") String slaInfo);

    /** 删除套餐全部轮播/主图。 */
    int deleteImages(@Param("packageId") String packageId);

    /** 插入单条图片。 */
    int insertImage(@Param("id") String id, @Param("packageId") String packageId,
                    @Param("type") String type, @Param("url") String url, @Param("sortOrder") int sortOrder);

    /** 删除套餐全部参数选项。 */
    int deleteParamOptions(@Param("packageId") String packageId);

    /** 删除套餐全部参数组。 */
    int deleteParams(@Param("packageId") String packageId);

    /** 插入参数组。 */
    int insertParam(@Param("id") String id, @Param("packageId") String packageId, @Param("groupKey") String groupKey,
                    @Param("name") String name, @Param("type") String type, @Param("required") int required,
                    @Param("sortOrder") int sortOrder);

    /** 插入参数选项（保留历史加价）。 */
    int insertParamOption(@Param("id") String id, @Param("paramId") String paramId,
                          @Param("optionValue") String optionValue, @Param("packageId") String packageId,
                          @Param("sortOrder") int sortOrder);
}
