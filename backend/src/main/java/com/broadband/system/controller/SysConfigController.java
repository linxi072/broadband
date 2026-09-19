package com.broadband.system.controller;

import com.broadband.system.model.SysConfig;
import com.broadband.system.service.SysConfigService;
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
 * <p>本类只做权限校验与参数绑定，业务逻辑见 {@link SysConfigService}。</p>
 */
@RestController
@RequestMapping("/api/system/config")
@PreAuthorize("hasAuthority('system:config')")
public class SysConfigController {

    @Autowired private SysConfigService sysConfigService;

    @GetMapping
    public List<SysConfig> list() {
        return sysConfigService.list();
    }

    @GetMapping("/{key}")
    public SysConfig one(@PathVariable String key) {
        return sysConfigService.one(key);
    }

    @PostMapping
    public SysConfig create(@RequestBody Map<String, Object> req) {
        return sysConfigService.create(req);
    }

    @PutMapping("/{key}")
    public Map<String, Object> update(@PathVariable String key, @RequestBody Map<String, Object> req) {
        return sysConfigService.update(key, req);
    }

    @DeleteMapping("/{key}")
    public Map<String, Object> delete(@PathVariable String key) {
        return sysConfigService.delete(key);
    }

    /** 手动刷新参数配置缓存（T-05 热刷新兜底端点），无需重启。 */
    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        return sysConfigService.refresh();
    }
}
