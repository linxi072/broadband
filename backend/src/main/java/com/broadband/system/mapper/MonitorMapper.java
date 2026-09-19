package com.broadband.system.mapper;

import org.apache.ibatis.annotations.Mapper;

/** 监控心跳（数据库往返探测）映射。 */
@Mapper
public interface MonitorMapper {

    /** 一次真实往返查询，返回 1 表示数据库可达。 */
    Integer ping();
}
