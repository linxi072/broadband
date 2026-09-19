package com.broadband.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.Ids;
import com.broadband.common.Values;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.mapper.SysRoleMapper;
import com.broadband.system.model.SysRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 角色管理业务：列表 / 新增 / 编辑 / 删除 / 角色菜单（权限树）查询与授权。
 */
@Service
public class SysRoleService {

    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysMenuMapper menuMapper;
    @Autowired private OperLogService operLog;

    public List<SysRole> list() {
        return roleMapper.selectList(new QueryWrapper<SysRole>().orderByAsc("id"));
    }

    public SysRole create(Map<String, Object> req) {
        String code = Values.str(req.get("code"));
        String name = Values.str(req.get("name"));
        if (code == null) throw new IllegalArgumentException("角色标识(code)不能为空");
        if (name == null) throw new IllegalArgumentException("角色名称不能为空");
        if (roleMapper.selectByCode(code) != null) {
            throw new IllegalArgumentException("角色标识已存在：" + code);
        }
        SysRole r = new SysRole();
        r.id = Ids.next();
        r.code = code;
        r.name = name;
        r.remark = Values.str(req.get("remark"));
        roleMapper.insert(r);
        log("新增角色 " + code + "/" + name, "/api/system/roles", "POST");
        return r;
    }

    public Map<String, Object> update(String id, Map<String, Object> req) {
        SysRole exist = roleMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("角色不存在：" + id);
        String name = Values.str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("角色名称不能为空");
        SysRole patch = new SysRole();
        patch.id = id;
        patch.name = name;
        patch.remark = Values.str(req.get("remark"));
        roleMapper.updateById(patch);
        log("编辑角色 " + id, "/api/system/roles/" + id, "PUT");
        return Map.of("ok", true);
    }

    /** 删除角色：级联清理角色-菜单、用户-角色关联。 */
    public Map<String, Object> delete(String id) {
        if (roleMapper.selectById(id) == null) throw new IllegalArgumentException("角色不存在：" + id);
        roleMapper.deleteRoleMenus(id);
        roleMapper.deleteUserRoles(id);
        roleMapper.deleteById(id);
        log("删除角色 " + id, "/api/system/roles/" + id, "DELETE");
        return Map.of("ok", true);
    }

    /** 角色已授权的菜单（menuIds 用于前端回填勾选，menus 为该角色可见菜单）。 */
    public Map<String, Object> menus(String id) {
        return Map.of(
                "roleId", id,
                "menuIds", menuMapper.selectMenuIds(id),
                "menus", menuMapper.selectByRole(id)
        );
    }

    /** 保存角色菜单授权（先清后建）。 */
    public Map<String, Object> saveMenus(String id, Object raw) {
        menuMapper.deleteRoleMenus(id);
        int n = 0;
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                menuMapper.insertRoleMenu(id, String.valueOf(o).trim());
                n++;
            }
        }
        log("更新角色权限 " + id + "（" + n + " 项）", "/api/system/roles/" + id + "/menus", "PUT");
        return Map.of("ok", true, "count", n);
    }

    // ------------------------------------------------------------------ 辅助

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
