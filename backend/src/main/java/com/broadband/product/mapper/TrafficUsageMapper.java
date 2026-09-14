package com.broadband.product.mapper;

import com.broadband.product.model.TrafficRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 流量用量 Mapper（MyBatis-Plus）。 */
@Mapper
public interface TrafficUsageMapper extends BaseMapper<TrafficRecord> {
}
