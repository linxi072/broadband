package com.broadband.system.spring;

import com.broadband.common.Ids;
import com.broadband.system.mapper.SysConfigMapper;
import com.broadband.system.model.SysConfig;
import com.broadband.system.service.ConfigCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 参数配置（sys_config）。权限码 {@code system:config} 与前端、@PreAuthorize 三处同源。
 *
 * <ul>
 *   <li>GET    /api/system/config            —— 全部参数列表</li>
 *   <li>GET    /api/system/config/{key}      —— 单条参数</li>
 *   <li>POST   /api/system/config            —— 新增参数</li>
 *   <li>PUT    /api/system/config/{key}      —— 编辑参数</li>
 *   <li>DELETE /api/system/config/{key}      —— 删除参数</li>
 *   <li>GET    /api/config/public/{key}      —— 开放读取参数值（免鉴权）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/system/config")
@PreAuthorize("hasAuthority('system:config')")
public class SysConfigController {

    @Autowired private SysConfigMapper configMapper;
    @Autowired private OperLogService operLog;
    @Autowired private ConfigCacheService configCache;

    @GetMapping
    public List<SysConfig> list() {
        return configMapper.selectList(null);
    }

    @GetMapping("/{key}")
    public SysConfig one(@PathVariable String key) {
        SysConfig c = configMapper.selectById(key);
        return c == null ? new SysConfig() : c;
    }

    @PostMapping
    public SysConfig create(@RequestBody Map<String, Object> req) {
        String key = require(str(req.get("configKey")), "参数键(configKey)必填");
        if (configMapper.selectById(key) != null) {
            throw new IllegalArgumentException("参数键已存在：" + key);
        }
        SysConfig c = new SysConfig();
        c.configKey = key;
        c.configName = require(str(req.get("configName")), "参数名称(configName)必填");
        c.configValue = str(req.get("configValue")) == null ? "" : str(req.get("configValue"));
        c.configType = str(req.get("configType")) == null ? "STRING" : str(req.get("configType"));
        c.remark = str(req.get("remark"));
        c.createTime = System.currentTimeMillis();
        configMapper.insert(c);
        configCache.reloadConfig();
        log("新增参数 " + key);
        return c;
    }

    @PutMapping("/{key}")
    public Map<String, Object> update(@PathVariable String key, @RequestBody Map<String, Object> req) {
        SysConfig exist = configMapper.selectById(key);
        if (exist == null) throw new IllegalArgumentException("参数不存在：" + key);
        SysConfig patch = new SysConfig();
        patch.configKey = key;
        patch.configName = str(req.get("configName")) == null ? exist.configName : str(req.get("configName"));
        patch.configValue = str(req.get("configValue")) == null ? exist.configValue : str(req.get("configValue"));
        patch.configType = str(req.get("configType")) == null ? exist.configType : str(req.get("configType"));
        patch.remark = str(req.get("remark")) == null ? exist.remark : str(req.get("remark"));
        configMapper.updateById(patch);
        configCache.reloadConfig();
        log("编辑参数 " + key);
        return Map.of("ok", true);
    }

    @DeleteMapping("/{key}")
    public Map<String, Object> delete(@PathVariable String key) {
        configMapper.deleteById(key);
        configCache.reloadConfig();
        log("删除参数 " + key);
        return Map.of("ok", true);
    }

    /**
     * 手动刷新参数配置缓存（T-05 热刷新兜底端点）。
     * 适用于直接改库、或需跨节点强制同步的场景，无需重启。
     */
    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        configCache.reloadConfig();
        return configCache.status();
    }

    // ------------------------------------------------------------------ 辅助

    private void log(String action) {
        var me = AuthController.current();
        operLog.record(me == null ? null : me.user.username,
                me == null ? null : me.user.name,
                action, "/api/system/config", "WRITE", "-", "成功", 0);
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String require(String v, String msg) {
        if (v == null) throw new IllegalArgumentException(msg);
        return v;
    }
}
