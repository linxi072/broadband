package com.broadband.system.model;

import java.util.ArrayList;
import java.util.List;

/** 用户视图对象：对外返回，不含密码；含角色与权限码。 */
public class SysUserVO {
    public String id;
    public String username;
    public String name;
    public String deptId;       // 部门ID（关联 sys_department）
    public String deptName;     // 部门名称（展示用，由控制器回填）
    public String status;
    public int mustChangePassword = 0;   // 1=首次登录必须改密
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
        vo.deptId = u.deptId;
        vo.status = u.status;
        vo.mustChangePassword = u.mustChangePassword;
        vo.createdTime = u.createdTime;
        return vo;
    }
}
