package com.broadband.product.mapper;

import com.broadband.product.model.PackageInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 套餐主表 Mapper（MyBatis-Plus）。 */
@Mapper
public interface PackageMapper extends BaseMapper<PackageInfo> {
}
