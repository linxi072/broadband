package com.broadband.product.mapper;

import com.broadband.product.model.PackageConvergeItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 融合套餐组成项 Mapper。
 *
 * <p><b>保留字桥接</b>：接口/前端契约字段名为 {@code desc}，但 {@code DESC} 是 MySQL 保留字，
 * 不能作为未加引号的列名出现在自动拼接的 SQL 中。因此表列名取 {@code description}，
 * 通过下面的显式 SQL 用列别名 {@code AS `desc`} 桥接回 {@code desc}。</p>
 *
 * <p>这样实体 {@link PackageConvergeItem} 保持零 ORM 注解（纯 Java），
 * 与「核心算法/模型与框架解耦、可独立编译验证」的架构原则一致。</p>
 *
 * <p>注意：package_converge_item 请勿直接使用 BaseMapper 的自动 CRUD（会拼出未转义的 desc），
 * 读写统一走 {@link #selectByPackage(String)} 或显式 SQL。</p>
 */
@Mapper
public interface PackageConvergeMapper extends BaseMapper<PackageConvergeItem> {

    /** 按套餐查询组成项（列 description 别名回 desc）。 */
    @Select("SELECT id, package_id, label, description AS `desc`, sort_order "
            + "FROM package_converge_item WHERE package_id = #{packageId} ORDER BY sort_order")
    List<PackageConvergeItem> selectByPackage(@Param("packageId") String packageId);
}
