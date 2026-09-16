package com.broadband.system.spring;

import com.broadband.system.service.ConfigCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据字典开放读取接口（免鉴权）。供 C 端小程序下拉框、参数获取使用。
 *
 * <ul>
 *   <li>GET /api/dict/public/{dictType} —— 返回某类型下「已启用」数据项 [{label,value,sort}]</li>
 * </ul>
 *
 * <p>读走 {@link ConfigCacheService} 内存缓存（T-05：改 sys_dict 后免重启即生效）。</p>
 */
@RestController
@RequestMapping("/api/dict/public")
public class PublicDictController {

    @Autowired private ConfigCacheService configCache;

    @GetMapping("/{dictType}")
    public List<Map<String, Object>> publicData(@PathVariable String dictType) {
        return configCache.getDict(dictType);
    }
}
