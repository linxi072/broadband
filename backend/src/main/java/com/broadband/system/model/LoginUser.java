package com.broadband.system.model;

import java.util.ArrayList;
import java.util.List;

/** 当前登录主体（SecurityContext 的 principal）。 */
public class LoginUser {

    public SysUser user;
    public List<SysRole> roles = new ArrayList<>();
    public List<String> perms = new ArrayList<>();

    public LoginUser() {}

    public LoginUser(SysUser user, List<SysRole> roles, List<String> perms) {
        this.user = user;
        this.roles = roles;
        this.perms = perms;
    }

    public String getUsername() {
        return user == null ? null : user.username;
    }

    public List<String> roleCodes() {
        List<String> out = new ArrayList<>();
        for (SysRole r : roles) out.add(r.code);
        return out;
    }

    public List<String> roleNames() {
        List<String> out = new ArrayList<>();
        for (SysRole r : roles) out.add(r.name);
        return out;
    }

    /** 是否管理员（拥有 ADMIN 角色） */
    public boolean isAdmin() {
        return roleCodes().contains("ADMIN");
    }
}
