package com.broadband.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.broadband.product.model.Customer;
import org.apache.ibatis.annotations.Mapper;

/** 客户表 Mapper。 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
