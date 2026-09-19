package com.broadband.system.service;

import com.broadband.common.Ids;
import com.broadband.common.Values;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.model.SysMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单权限业务：完整权限树（与前端 router/routes.js、后端 @PreAuthorize 三处同源）+ 节点增删改。
 */
@Service
public class SysMenuService {

    private static final List<String> TYPES = List.of("DIR", "MENU", "BUTTON");

    @Autowired private SysMenuMapper menuMapper;
    @Autowired private OperLogService operLog;

    /** 平铺菜单列表 -> 树（parentId 为空或父节点不存在时作为根）。 */
    public List<SysMenu> tree() {
        List<SysMenu> flat = menuMapper.selectAll();
        return toTree(flat);
    }

    /** 新增菜单节点（目录 / 菜单 / 按钮）。 */
    public SysMenu create(Map<String, Object> req) {
        String name = Values.str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("名称不能为空");
        String type = Values.str(req.get("type"), "MENU");
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException("类型只能是 DIR / MENU / BUTTON");
        }
        SysMenu m = new SysMenu();
        m.id = Ids.next();
        m.parentId = Values.str(req.get("parentId"));
        m.name = name;
        m.path = Values.str(req.get("path"));
        m.perm = Values.str(req.get("perm"));
        m.type = type;
        Object so = req.get("sortOrder");
        m.sortOrder = so instanceof Number n ? n.intValue() : 0;
        menuMapper.insert(m);
        log("新增菜单 " + name, "/api/system/menus", "POST");
        return m;
    }

    public Map<String, Object> update(String id, Map<String, Object> req) {
        if (menuMapper.selectById(id) == null) throw new IllegalArgumentException("菜单不存在：" + id);
        SysMenu m = new SysMenu();
        m.id = id;
        m.parentId = Values.str(req.get("parentId"));
        String name = Values.str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("名称不能为空");
        m.name = name;
        m.path = Values.str(req.get("path"));
        m.perm = Values.str(req.get("perm"));
        String type = Values.str(req.get("type"));
        if (type != null && !TYPES.contains(type)) {
            throw new IllegalArgumentException("类型只能是 DIR / MENU / BUTTON");
        }
        if (type != null) m.type = type;
        Object so = req.get("sortOrder");
        if (so instanceof Number n) m.sortOrder = n.intValue();
        menuMapper.update(m);
        log("编辑菜单 " + id, "/api/system/menus/" + id, "PUT");
        return Map.of("ok", true);
    }

    /** 删除菜单节点（级联删除子节点 + 清理角色-菜单授权）。 */
    public Map<String, Object> delete(String id) {
        if (menuMapper.selectById(id) == null) throw new IllegalArgumentException("菜单不存在：" + id);
        cascadeDelete(id);
        log("删除菜单 " + id, "/api/system/menus/" + id, "DELETE");
        return Map.of("ok", true);
    }

    // ------------------------------------------------------------------ 辅助

    private void cascadeDelete(String id) {
        for (SysMenu child : menuMapper.selectChildren(id)) cascadeDelete(child.id);
        menuMapper.deleteRoleMenusByMenu(id);
        menuMapper.deleteById(id);
    }

    /** 平铺 -> 树（parentId 为空、或父节点不在集合内时作为根节点呈现）。 */
    static List<SysMenu> toTree(List<SysMenu> flat) {
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

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
