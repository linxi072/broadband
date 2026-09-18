package com.broadband.system.spring;

import com.broadband.common.Ids;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.model.SysMenu;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 菜单权限：完整权限树（与前端 router/routes.js、后端 @PreAuthorize 三处同源）+ 节点增删改。 */
@RestController
@RequestMapping("/api/system/menus")
@PreAuthorize("hasAuthority('system:menu')")
public class SysMenuController {

    @Autowired private SysMenuMapper menuMapper;
    @Autowired private OperLogService operLog;

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

    /** 新增菜单节点（目录 / 菜单 / 按钮）。 */
    @PostMapping
    public SysMenu create(@RequestBody Map<String, Object> req) {
        String name = str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("名称不能为空");
        String type = str(req.get("type"));
        if (type == null) type = "MENU";
        if (!List.of("DIR", "MENU", "BUTTON").contains(type)) {
            throw new IllegalArgumentException("类型只能是 DIR / MENU / BUTTON");
        }
        SysMenu m = new SysMenu();
        m.id = Ids.next();
        m.parentId = str(req.get("parentId"));
        m.name = name;
        m.path = str(req.get("path"));
        m.perm = str(req.get("perm"));
        m.type = type;
        Object so = req.get("sortOrder");
        m.sortOrder = so instanceof Number ? ((Number) so).intValue() : 0;
        menuMapper.insert(m);
        log("新增菜单 " + name, "/api/system/menus", "POST");
        return m;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        if (menuMapper.selectById(id) == null) throw new IllegalArgumentException("菜单不存在：" + id);
        SysMenu m = new SysMenu();
        m.id = id;
        m.parentId = str(req.get("parentId"));
        String name = str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("名称不能为空");
        m.name = name;
        m.path = str(req.get("path"));
        m.perm = str(req.get("perm"));
        String type = str(req.get("type"));
        if (type != null && !List.of("DIR", "MENU", "BUTTON").contains(type)) {
            throw new IllegalArgumentException("类型只能是 DIR / MENU / BUTTON");
        }
        if (type != null) m.type = type;
        Object so = req.get("sortOrder");
        if (so instanceof Number) m.sortOrder = ((Number) so).intValue();
        menuMapper.update(m);
        log("编辑菜单 " + id, "/api/system/menus/" + id, "PUT");
        return Map.of("ok", true);
    }

    /** 删除菜单节点（级联删除子节点 + 清理角色-菜单授权）。 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        if (menuMapper.selectById(id) == null) throw new IllegalArgumentException("菜单不存在：" + id);
        cascadeDelete(id);
        log("删除菜单 " + id, "/api/system/menus/" + id, "DELETE");
        return Map.of("ok", true);
    }

    private void cascadeDelete(String id) {
        for (SysMenu child : menuMapper.selectChildren(id)) cascadeDelete(child.id);
        menuMapper.deleteRoleMenusByMenu(id);
        menuMapper.deleteById(id);
    }

    // ------------------------------------------------------------------ 辅助

    private void log(String action, String target, String method) {
        var me = AuthController.current();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
