package com.broadband.community.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 小区覆盖管理（PC 后台）数据访问。SQL 集中在 {@code resources/mapper/community/CommunityAdminMapper.xml}。
 */
@Mapper
public interface CommunityAdminMapper {

    /** 小区覆盖列表（端口余量 / 覆盖状态为 SQL 计算列，与 CommunityChecker 口径一致）。 */
    List<Map<String, Object>> list(@Param("keyword") String keyword);

    /** 新增或更新小区覆盖（按 id upsert）。 */
    int save(@Param("id") String id, @Param("name") String name, @Param("region") String region,
             @Param("street") String street, @Param("carrier") String carrier,
             @Param("lat") Double lat, @Param("lng") Double lng,
             @Param("installable") int installable,
             @Param("portTotal") int portTotal, @Param("portUsed") int portUsed);
}
