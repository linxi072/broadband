package com.broadband.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.Ids;
import com.broadband.common.Values;
import com.broadband.system.mapper.SysUserMapper;
import com.broadband.system.model.SysUser;
import com.broadband.system.model.SysUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户管理（RBAC）业务：列表 / 新增 / 启停 / 分配角色 / 重置密码 / 编辑 / 删除。
 */
@Service
public class SysUserService {

    @Autowired private SysUserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OperLogService operLog;

    /** 用户列表（含角色编码 / 名称），可按账号或姓名模糊匹配。 */
    public List<SysUserVO> list(String keyword) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("username", keyword).or().like("name", keyword));
        }
        qw.orderByAsc("id");

        List<SysUserVO> out = new ArrayList<>();
        for (SysUser u : userMapper.selectList(qw)) {
            out.add(withRoles(SysUserVO.of(u)));
        }
        return out;
    }

    /** 新增用户（默认启用；未传密码时回落初始密码 123456，登录后强制改密由管理员另行操作）。 */
    public SysUserVO create(Map<String, Object> req) {
        String username = Values.str(req.get("username"));
        if (username == null) throw new IllegalArgumentException("账号不能为空");
        if (userMapper.selectByUsername(username) != null) {
            throw new IllegalArgumentException("账号已存在：" + username);
        }

        SysUser u = new SysUser();
        u.id = Ids.next();
        u.username = username;
        u.name = Values.str(req.get("name"), username);
        u.deptId = Values.str(req.get("deptId"));
        u.status = "ENABLED";
        u.createdTime = System.currentTimeMillis();
        String raw = Values.str(req.get("password"));
        u.password = passwordEncoder.encode(raw == null ? "123456" : raw);
        userMapper.insert(u);

        for (String roleId : Values.strList(req.get("roleIds"))) userMapper.insertUserRole(u.id, roleId);

        log("新增用户 " + username, "/api/system/users", "POST");
        return withRoles(SysUserVO.of(u));
    }

    /** 启停用户（ENABLED / DISABLED）。 */
    public Map<String, Object> updateStatus(String id, String status) {
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw new IllegalArgumentException("状态只能是 ENABLED / DISABLED");
        }
        SysUser patch = new SysUser();
        patch.id = id;
        patch.status = status;
        userMapper.updateById(patch);
        log("更新用户状态 " + id + " -> " + status, "/api/system/users/" + id + "/status", "PUT");
        return Map.of("ok", true);
    }

    /** 分配角色（先清后建）。 */
    public Map<String, Object> assignRoles(String id, List<String> roleIds) {
        userMapper.deleteUserRoles(id);
        for (String roleId : roleIds) userMapper.insertUserRole(id, roleId);
        log("分配角色 " + id + " -> " + roleIds, "/api/system/users/" + id + "/roles", "PUT");
        return Map.of("ok", true, "roleIds", roleIds);
    }

    /**
     * 重置密码（管理员）：管理员重置后强制用户下次登录改密（mustChangePassword=1）。
     * @throws IllegalArgumentException 密码不足 6 位或用户不存在
     */
    public Map<String, Object> resetPassword(String id, String raw) {
        if (raw == null || raw.length() < 6) throw new IllegalArgumentException("密码至少 6 位");
        SysUser exist = userMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("用户不存在：" + id);
        // 载入完整实体再更新，避免 updateById 覆盖 createdTime/status 等字段
        exist.password = passwordEncoder.encode(raw);
        exist.mustChangePassword = 1;
        userMapper.updateById(exist);
        log("重置密码 " + id, "/api/system/users/" + id + "/password", "PUT");
        return Map.of("ok", true);
    }

    /** 编辑用户基本信息（姓名 / 部门；密码与角色走独立接口）。 */
    public Map<String, Object> update(String id, Map<String, Object> req) {
        SysUser exist = userMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("用户不存在：" + id);
        SysUser patch = new SysUser();
        patch.id = id;
        patch.name = Values.str(req.get("name"), exist.name);
        patch.deptId = Values.str(req.get("deptId"), exist.deptId);
        userMapper.updateById(patch);
        log("编辑用户 " + id, "/api/system/users/" + id, "PUT");
        return Map.of("ok", true);
    }

    /** 删除用户：禁止删除自己、禁止删除内置 admin；级联清理用户-角色关联。 */
    public Map<String, Object> delete(String id) {
        SysUser exist = userMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("用户不存在：" + id);
        var me = CurrentUser.get();
        if (me != null && me.user.id.equals(id)) throw new IllegalArgumentException("不能删除当前登录账号");
        if ("admin".equals(exist.username)) throw new IllegalArgumentException("内置管理员账号不可删除");
        userMapper.deleteUserRoles(id);
        userMapper.deleteById(id);
        log("删除用户 " + exist.username, "/api/system/users/" + id, "DELETE");
        return Map.of("ok", true);
    }

    // ------------------------------------------------------------------ 辅助

    private SysUserVO withRoles(SysUserVO vo) {
        userMapper.selectRoles(vo.id).forEach(r -> {
            vo.roles.add(r.code);
            vo.roleNames.add(r.name);
            vo.roleIds.add(r.id);
        });
        return vo;
    }

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
