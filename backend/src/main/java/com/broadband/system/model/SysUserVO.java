package com.broadband.system.model;

import java.util.ArrayList;
import java.util.List;

/** 用户视图对象：对外返回，不含密码；含角色与权限码。 */
public class SysUserVO {
    public String id;
    public String username;
    public String name;
    public String dept;
    public String status;
    public long createdTime;

    public List<String> roles = new ArrayList<>();      // 角色标识，如 ADMIN
    public List<String> roleNames = new ArrayList<>();  // 角色名称，如 超级管理员
    public List<String> roleIds = new ArrayList<>();

    public SysUserVO() {}

    public static SysUserVO of(SysUser u) {
        SysUserVO vo = new SysUserVO();
        vo.id = u.id;
        vo.username = u.username;
        vo.name = u.name;
        vo.dept = u.dept;
        vo.status = u.status;
        vo.createdTime = u.createdTime;
        return vo;
    }
}
