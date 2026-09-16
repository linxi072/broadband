package com.broadband.system.model;

/**
 * 系统用户（对应 DB 表 sys_user）。
 * 纯 POJO：不引入 ORM / Jackson 注解；密码字段不会直接对外返回，对外用 {@link SysUserVO}。
 */
public class SysUser {
    public String id;
    public String username;
    public String password;      // BCrypt 哈希
    public String name;
    public String deptId;       // 关联 sys_department.id（NULL = 不限定部门，看全部）
    public String status = "ENABLED";   // ENABLED / DISABLED
    public int mustChangePassword = 0;   // 1=首次登录必须改密（默认管理员种子置 1）
    public long createdTime;

    public SysUser() {}
}
