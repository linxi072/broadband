package com.broadband.system.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.system.mapper.SysOperLogMapper;
import com.broadband.system.model.SysOperLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务：登录、写操作、越权尝试留痕。
 *
 * <p>所有写入都做了异常吞掉处理 —— 日志失败绝不能影响主流程。</p>
 */
@Service
public class OperLogService {

    @Autowired
    private SysOperLogMapper operLogMapper;

    public void record(String username, String name, String action, String target,
                       String method, String ip, String result, long costMs) {
        try {
            operLogMapper.insertLog(new SysOperLog(
                    trim(username, 64), trim(name, 64), trim(action, 64), trim(target, 255),
                    trim(method, 16), trim(ip, 64), trim(result, 32), costMs));
        } catch (Exception ignored) {
            // 日志容错：不影响业务
        }
    }

    public void record(SysOperLog log) {
        try {
            operLogMapper.insertLog(log);
        } catch (Exception ignored) {
            // 同上
        }
    }

    public java.util.List<SysOperLog> recent(String keyword, int limit) {
        QueryWrapper<SysOperLog> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("username", keyword).or().like("action", keyword).or().like("target", keyword));
        }
        qw.orderByDesc("id").last("limit " + Math.max(1, Math.min(limit, 500)));
        return operLogMapper.selectList(qw);
    }

    private static String trim(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
