package com.broadband.system.controller;

import com.broadband.system.model.SysMenu;
import com.broadband.system.service.SysMenuService;
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

/** 菜单权限：完整权限树（与前端 router/routes.js、后端 @PreAuthorize 三处同源）+ 节点增删改。 */
@RestController
@RequestMapping("/api/system/menus")
@PreAuthorize("hasAuthority('system:menu')")
public class SysMenuController {

    @Autowired private SysMenuService sysMenuService;

    @GetMapping("/tree")
    public List<SysMenu> tree() {
        return sysMenuService.tree();
    }

    /** 新增菜单节点（目录 / 菜单 / 按钮）。 */
    @PostMapping
    public SysMenu create(@RequestBody Map<String, Object> req) {
        return sysMenuService.create(req);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return sysMenuService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        return sysMenuService.delete(id);
    }
}
