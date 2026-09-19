package com.broadband.system.service;

import com.broadband.common.Values;
import com.broadband.system.mapper.SysConfigMapper;
import com.broadband.system.model.SysConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 参数配置（sys_config）业务：增删改查与配置缓存刷新。
 */
@Service
public class SysConfigService {

    private static final String RESOURCE = "/api/system/config";

    @Autowired private SysConfigMapper configMapper;
    @Autowired private OperLogService operLog;
    @Autowired private ConfigCacheService configCache;

    public List<SysConfig> list() {
        return configMapper.selectList(null);
    }

    /** 单条参数；不存在返回空对象（前端表单不报错）。 */
    public SysConfig one(String key) {
        SysConfig c = configMapper.selectById(key);
        return c == null ? new SysConfig() : c;
    }

    public SysConfig create(Map<String, Object> req) {
        String key = require(Values.str(req.get("configKey")), "参数键(configKey)必填");
        if (configMapper.selectById(key) != null) {
            throw new IllegalArgumentException("参数键已存在：" + key);
        }
        SysConfig c = new SysConfig();
        c.configKey = key;
        c.configName = require(Values.str(req.get("configName")), "参数名称(configName)必填");
        c.configValue = Values.str(req.get("configValue"), "");
        c.configType = Values.str(req.get("configType"), "STRING");
        c.remark = Values.str(req.get("remark"));
        c.createTime = System.currentTimeMillis();
        configMapper.insert(c);
        configCache.reloadConfig();
        log("新增参数 " + key);
        return c;
    }

    public Map<String, Object> update(String key, Map<String, Object> req) {
        SysConfig exist = configMapper.selectById(key);
        if (exist == null) throw new IllegalArgumentException("参数不存在：" + key);
        SysConfig patch = new SysConfig();
        patch.configKey = key;
        patch.configName = Values.str(req.get("configName"), exist.configName);
        patch.configValue = Values.str(req.get("configValue"), exist.configValue);
        patch.configType = Values.str(req.get("configType"), exist.configType);
        patch.remark = Values.str(req.get("remark"), exist.remark);
        configMapper.updateById(patch);
        configCache.reloadConfig();
        log("编辑参数 " + key);
        return Map.of("ok", true);
    }

    public Map<String, Object> delete(String key) {
        configMapper.deleteById(key);
        configCache.reloadConfig();
        log("删除参数 " + key);
        return Map.of("ok", true);
    }

    /** 手动刷新参数缓存（T-05 热刷新兜底，适用于直接改库或跨节点强制同步）。 */
    public Map<String, Object> refresh() {
        configCache.reloadConfig();
        return configCache.status();
    }

    // ------------------------------------------------------------------ 辅助

    private static String require(String v, String msg) {
        if (v == null) throw new IllegalArgumentException(msg);
        return v;
    }

    private void log(String action) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, RESOURCE, "WRITE", "-", "成功", 0);
    }
}
