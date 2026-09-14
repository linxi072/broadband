package com.broadband.system.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.mapper.SysRoleMapper;
import com.broadband.system.model.SysMenu;
import com.broadband.system.model.SysRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 角色管理：列表 / 角色菜单（权限树）查询与授权。 */
@RestController
@RequestMapping("/api/system/roles")
@PreAuthorize("hasAuthority('system:role')")
public class SysRoleController {

    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysMenuMapper menuMapper;
    @Autowired private OperLogService operLog;

    @GetMapping
    public List<SysRole> list() {
        return roleMapper.selectList(new QueryWrapper<SysRole>().orderByAsc("id"));
    }

    @GetMapping("/{id}/menus")
    public Map<String, Object> menus(@PathVariable String id) {
        return Map.of(
                "roleId", id,
                "menuIds", menuMapper.selectMenuIds(id),
                "menus", menuMapper.selectByRole(id)
        );
    }

    @PutMapping("/{id}/menus")
    public Map<String, Object> saveMenus(@PathVariable String id, @RequestBody Map<String, Object> req) {
        Object raw = req.get("menuIds");
        menuMapper.deleteRoleMenus(id);
        int n = 0;
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                menuMapper.insertRoleMenu(id, String.valueOf(o).trim());
                n++;
            }
        }
        var me = AuthController.current();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                "更新角色权限 " + id + "（" + n + " 项）", "/api/system/roles/" + id + "/menus",
                "PUT", "-", "成功", 0);
        return Map.of("ok", true, "count", n);
    }
}
