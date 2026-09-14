package com.broadband.system.model;

/** 角色（对应 DB 表 sys_role）。code 同时用于 Spring Security 的 ROLE_ 前缀。 */
public class SysRole {
    public String id;
    public String code;      // ADMIN / OPERATOR / FINANCE / CS / SALES
    public String name;
    public String remark;

    public SysRole() {}
}
