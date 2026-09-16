package com.broadband.system.spring;

import com.broadband.system.mapper.SysConfigMapper;
import com.broadband.system.model.SysConfig;
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
 *   <li>GET /api/config/public/{key} —— 返回 {key, value}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/config/public")
public class PublicConfigController {

    @Autowired private SysConfigMapper configMapper;

    @GetMapping("/{key}")
    public Map<String, Object> publicValue(@PathVariable String key) {
        SysConfig c = configMapper.selectById(key);
        String value = c == null ? "" : c.configValue;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("key", key);
        r.put("value", value);
        return r;
    }
}
