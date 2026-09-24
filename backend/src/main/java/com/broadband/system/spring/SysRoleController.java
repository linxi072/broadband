package com.broadband.system.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.Ids;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.mapper.SysRoleMapper;
import com.broadband.system.model.SysMenu;
import com.broadband.system.model.SysRole;
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

    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysMenuMapper menuMapper;
    @Autowired private OperLogService operLog;

    @GetMapping
    public List<SysRole> list() {
        return roleMapper.selectList(new QueryWrapper<SysRole>().orderByAsc("id"));
    }

    @PostMapping
    public SysRole create(@RequestBody Map<String, Object> req) {
        String code = str(req.get("code"));
        String name = str(req.get("name"));
        if (code == null) throw new IllegalArgumentException("角色标识(code)不能为空");
        if (name == null) throw new IllegalArgumentException("角色名称不能为空");
        if (roleMapper.selectByCode(code) != null) {
            throw new IllegalArgumentException("角色标识已存在：" + code);
        }
        SysRole r = new SysRole();
        r.id = Ids.next();
        r.code = code;
        r.name = name;
        r.remark = str(req.get("remark"));
        roleMapper.insert(r);
        log("新增角色 " + code + "/" + name, "/api/system/roles", "POST");
        return r;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        SysRole exist = roleMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("角色不存在：" + id);
        String name = str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("角色名称不能为空");
        SysRole patch = new SysRole();
        patch.id = id;
        patch.name = name;
        patch.remark = str(req.get("remark"));
        roleMapper.updateById(patch);
        log("编辑角色 " + id, "/api/system/roles/" + id, "PUT");
        return Map.of("ok", true);
    }

    /** 删除角色：级联清理角色-菜单、用户-角色关联。 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        if (roleMapper.selectById(id) == null) throw new IllegalArgumentException("角色不存在：" + id);
        roleMapper.deleteRoleMenus(id);
        roleMapper.deleteUserRoles(id);
        roleMapper.deleteById(id);
        log("删除角色 " + id, "/api/system/roles/" + id, "DELETE");
        return Map.of("ok", true);
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
