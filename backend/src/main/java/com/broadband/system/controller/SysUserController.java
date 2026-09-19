package com.broadband.system.controller;

import com.broadband.common.Values;
import com.broadband.system.model.SysUserVO;
import com.broadband.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 用户管理（RBAC）：列表 / 新增 / 启停 / 分配角色 / 重置密码。 */
@RestController
@RequestMapping("/api/system/users")
@PreAuthorize("hasAuthority('system:user')")
public class SysUserController {

    @Autowired private SysUserService sysUserService;

    @GetMapping
    public List<SysUserVO> list(@RequestParam(required = false) String keyword) {
        return sysUserService.list(keyword);
    }

    @PostMapping
    public SysUserVO create(@RequestBody Map<String, Object> req) {
        return sysUserService.create(req);
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return sysUserService.updateStatus(id, Values.str(req.get("status")));
    }

    @PutMapping("/{id}/roles")
    public Map<String, Object> assignRoles(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return sysUserService.assignRoles(id, Values.strList(req.get("roleIds")));
    }

    @PutMapping("/{id}/password")
    public Map<String, Object> resetPassword(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return sysUserService.resetPassword(id, Values.str(req.get("password")));
    }

    /** 编辑用户基本信息（姓名 / 部门；密码与角色走独立接口）。 */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        return sysUserService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        return sysUserService.delete(id);
    }
}
