package com.broadband.system.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.Ids;
import com.broadband.system.mapper.SysUserMapper;
import com.broadband.system.model.SysUser;
import com.broadband.system.model.SysUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 用户管理（RBAC）：列表 / 新增 / 启停 / 分配角色 / 重置密码。 */
@RestController
@RequestMapping("/api/system/users")
@PreAuthorize("hasAuthority('system:user')")
public class SysUserController {

    @Autowired private SysUserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OperLogService operLog;

    @GetMapping
    public List<SysUserVO> list(@RequestParam(required = false) String keyword) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("username", keyword).or().like("name", keyword));
        }
        qw.orderByAsc("id");

        List<SysUserVO> out = new ArrayList<>();
        for (SysUser u : userMapper.selectList(qw)) {
            SysUserVO vo = SysUserVO.of(u);
            userMapper.selectRoles(u.id).forEach(r -> {
                vo.roles.add(r.code);
                vo.roleNames.add(r.name);
                vo.roleIds.add(r.id);
            });
            out.add(vo);
        }
        return out;
    }

    @PostMapping
    public SysUserVO create(@RequestBody Map<String, Object> req) {
        String username = str(req.get("username"));
        if (username == null) throw new IllegalArgumentException("账号不能为空");
        if (userMapper.selectByUsername(username) != null) {
            throw new IllegalArgumentException("账号已存在：" + username);
        }

        SysUser u = new SysUser();
        u.id = Ids.next();
        u.username = username;
        u.name = str(req.get("name")) == null ? username : str(req.get("name"));
        u.deptId = str(req.get("deptId"));
        u.status = "ENABLED";
        u.createdTime = System.currentTimeMillis();
        String raw = str(req.get("password"));
        u.password = passwordEncoder.encode(raw == null ? "123456" : raw);
        userMapper.insert(u);

        for (String roleId : strList(req.get("roleIds"))) userMapper.insertUserRole(u.id, roleId);

        log("新增用户 " + username, "/api/system/users", "POST");
        SysUserVO vo = SysUserVO.of(u);
        userMapper.selectRoles(u.id).forEach(r -> {
            vo.roles.add(r.code);
            vo.roleNames.add(r.name);
            vo.roleIds.add(r.id);
        });
        return vo;
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable String id, @RequestBody Map<String, Object> req) {
        String status = str(req.get("status"));
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

    @PutMapping("/{id}/roles")
    public Map<String, Object> assignRoles(@PathVariable String id, @RequestBody Map<String, Object> req) {
        List<String> roleIds = strList(req.get("roleIds"));
        userMapper.deleteUserRoles(id);
        for (String roleId : roleIds) userMapper.insertUserRole(id, roleId);
        log("分配角色 " + id + " -> " + roleIds, "/api/system/users/" + id + "/roles", "PUT");
        return Map.of("ok", true, "roleIds", roleIds);
    }

    @PutMapping("/{id}/password")
    public Map<String, Object> resetPassword(@PathVariable String id, @RequestBody Map<String, Object> req) {
        String raw = str(req.get("password"));
        if (raw == null || raw.length() < 6) throw new IllegalArgumentException("密码至少 6 位");
        SysUser exist = userMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("用户不存在：" + id);
        // 载入完整实体再更新，避免 updateById 覆盖 createdTime/status 等字段；
        // 管理员重置后强制用户下次登录改密（mustChangePassword=1）。
        exist.password = passwordEncoder.encode(raw);
        exist.mustChangePassword = 1;
        userMapper.updateById(exist);
        log("重置密码 " + id, "/api/system/users/" + id + "/password", "PUT");
        return Map.of("ok", true);
    }

    /** 编辑用户基本信息（姓名 / 部门；密码与角色走独立接口）。 */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        SysUser exist = userMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("用户不存在：" + id);
        SysUser patch = new SysUser();
        patch.id = id;
        patch.name = str(req.get("name")) == null ? exist.name : str(req.get("name"));
        patch.deptId = str(req.get("deptId")) == null ? exist.deptId : str(req.get("deptId"));
        userMapper.updateById(patch);
        log("编辑用户 " + id, "/api/system/users/" + id, "PUT");
        return Map.of("ok", true);
    }

    /** 删除用户：禁止删除自己、禁止删除内置 admin；级联清理用户-角色关联。 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        SysUser exist = userMapper.selectById(id);
        if (exist == null) throw new IllegalArgumentException("用户不存在：" + id);
        var me = AuthController.current();
        if (me != null && me.user.id.equals(id)) throw new IllegalArgumentException("不能删除当前登录账号");
        if ("admin".equals(exist.username)) throw new IllegalArgumentException("内置管理员账号不可删除");
        userMapper.deleteUserRoles(id);
        userMapper.deleteById(id);
        log("删除用户 " + exist.username, "/api/system/users/" + id, "DELETE");
        return Map.of("ok", true);
    }

    // ------------------------------------------------------------------ 辅助

    private void log(String action, String target, String method) {
        var me = AuthController.current();
        operLog.record(me == null ? null : me.user.username,
                me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static List<String> strList(Object v) {
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> list) {
            for (Object o : list) {
                String s = str(o);
                if (s != null) out.add(s);
            }
        } else {
            String s = str(v);
            if (s != null) {
                for (String x : s.split(",")) if (!x.isBlank()) out.add(x.trim());
            }
        }
        return out;
    }
}
