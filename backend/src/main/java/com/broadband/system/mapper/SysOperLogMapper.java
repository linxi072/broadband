package com.broadband.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.broadband.system.model.SysOperLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 操作日志 Mapper。 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    /**
     * 显式列插入（不使用 BaseMapper.insert）：id 交给数据库 AUTO_INCREMENT，
     * 避免 MyBatis-Plus 默认雪花 ID 策略覆盖自增列。
     */
    @Insert("""
            INSERT INTO sys_oper_log (username, name, action, target, method, ip, result, cost_ms, created_time)
            VALUES (#{username}, #{name}, #{action}, #{target}, #{method}, #{ip}, #{result}, #{costMs}, #{createdTime})
            """)
    int insertLog(SysOperLog log);

    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM sys_oper_log WHERE result LIKE '拒绝%'")
    long countDenied();
}
