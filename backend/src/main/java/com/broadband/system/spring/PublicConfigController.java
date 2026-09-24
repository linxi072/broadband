package com.broadband.system.spring;

import com.broadband.system.model.SysConfig;
import com.broadband.system.service.ConfigCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 参数配置开放读取接口（免鉴权）。供 C 端小程序读取系统参数（如客服电话）。
 *
 * <ul>
 *   <li>GET /api/config/public/{key} —— 返回 {key, value, configName, configType, remark}</li>
 * </ul>
 *
 * <p>读走 {@link ConfigCacheService} 内存缓存（T-05：改 sys_config 后免重启即生效）。</p>
 */
@RestController
@RequestMapping("/api/config/public")
public class PublicConfigController {

    @Autowired private ConfigCacheService configCache;

    @GetMapping("/{key}")
    public Map<String, Object> publicValue(@PathVariable String key) {
        Map<String, Object> cached = configCache.getConfig(key);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("key", key);
        if (cached == null) {
            r.put("value", "");
            r.put("configName", null);
            r.put("configType", null);
            r.put("remark", null);
            return r;
        }
        r.put("value", cached.getOrDefault("value", ""));
        r.put("configName", cached.get("configName"));
        r.put("configType", cached.get("configType"));
        r.put("remark", cached.get("remark"));
        return r;
    }
}
