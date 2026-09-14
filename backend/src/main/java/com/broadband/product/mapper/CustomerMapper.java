package com.broadband.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.broadband.product.model.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 客户表 Mapper。 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /** 按微信 openid 查客户（小程序真实登录绑定）。 */
    @Select("SELECT * FROM customer WHERE openid = #{openid} LIMIT 1")
    Customer selectByOpenid(@Param("openid") String openid);
}
