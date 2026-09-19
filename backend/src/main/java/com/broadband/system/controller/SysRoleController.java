package com.broadband.system.controller;

import com.broadband.system.model.SysRole;
import com.broadband.system.service.SysRoleService;
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

/** 角色管理：列表 / 新增 / 编辑 / 删除 / 角色菜单（权限树）查询与授权。 */
@RestController
@RequestMapping("/api/system/roles")
@PreAuthorize("hasAuthority('system:role')")
public class SysRoleController {

    @Autowired private SysRoleService sysRoleService;

    @GetMapping
    public List<SysRole> list() {
        return sysRoleService.list();
    }

    @PostMapping
    public SysRole create(@RequestBody Map<String, Object> req) {
        return sysRoleService.create(req);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return sysRoleService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        return sysRoleService.delete(id);
    }

    @GetMapping("/{id}/menus")
    public Map<String, Object> menus(@PathVariable String id) {
        return sysRoleService.menus(id);
    }

    @PutMapping("/{id}/menus")
    public Map<String, Object> saveMenus(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return sysRoleService.saveMenus(id, req.get("menuIds"));
    }
}
