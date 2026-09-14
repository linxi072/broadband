package com.broadband.product.mapper;

import com.broadband.product.model.PackageParamOption;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 套餐参数选项 Mapper。
 *
 * <p><b>保留字桥接</b>：接口/前端契约字段名为 {@code value}，为避免与 SQL 关键字歧义，
 * 表列名取 {@code option_value}，通过显式 SQL 别名 {@code AS `value`} 桥接回 {@code value}。
 * 实体保持零 ORM 注解（纯 Java）。</p>
 *
 * <p>注意：请勿对该表直接使用 BaseMapper 的自动 CRUD，读写统一走 {@link #selectByParam(String)}。</p>
 */
@Mapper
public interface PackageParamOptionMapper extends BaseMapper<PackageParamOption> {

    /** 按参数组查询选项（列 option_value 别名回 value）。 */
    @Select("SELECT id, param_id, option_value AS `value`, extra_fee, sort_order "
            + "FROM package_param_option WHERE param_id = #{paramId} ORDER BY sort_order")
    List<PackageParamOption> selectByParam(@Param("paramId") String paramId);
}
