package com.broadband.product.mapper;

import com.broadband.product.model.PackageInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 套餐主表 Mapper（MyBatis-Plus）。 */
@Mapper
public interface PackageMapper extends BaseMapper<PackageInfo> {

    /**
     * 在售套餐 + 主图（开放给 C 端小程序套餐列表）。
     * status 种子值为 ON_SHELF（见 data.sql），不是 ACTIVE。
     */
    @Select("""
        SELECT p.id, p.name, p.category, p.monthly_fee AS monthlyFee, p.original_fee AS originalFee,
               p.status, i.url AS cover
        FROM package_info p
        LEFT JOIN package_image i ON i.package_id = p.id AND i.type = 'MAIN'
        WHERE p.status = 'ON_SHELF'
        ORDER BY p.monthly_fee ASC
        """)
    List<Map<String, Object>> selectOnShelfWithCover();
}
