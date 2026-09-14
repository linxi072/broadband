package com.broadband.system.spring;

import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.model.SysMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 菜单权限：完整权限树（与前端 router/routes.js、后端 @PreAuthorize 三处同源）。 */
@RestController
@RequestMapping("/api/system/menus")
@PreAuthorize("hasAuthority('system:menu')")
public class SysMenuController {

    @Autowired private SysMenuMapper menuMapper;

    @GetMapping("/tree")
    public List<SysMenu> tree() {
        List<SysMenu> flat = menuMapper.selectAll();
        Map<String, SysMenu> index = new HashMap<>();
        for (SysMenu m : flat) index.put(m.id, m);

        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : flat) {
            if (m.parentId == null || m.parentId.isEmpty() || !index.containsKey(m.parentId)) {
                roots.add(m);
            } else {
                index.get(m.parentId).children.add(m);
            }
        }
        return roots;
    }
}
