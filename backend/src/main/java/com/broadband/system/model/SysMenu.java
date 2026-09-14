package com.broadband.system.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单 / 权限（对应 DB 表 sys_menu）。
 * path 与前端 router/routes.js 一致；perm 与后端 @PreAuthorize 的 authority 一致。
 * children 字段为树形聚合用，不落库。
 */
public class SysMenu {
    public String id;
    public String parentId;
    public String name;
    public String path;
    public String perm;
    public String type = "MENU";   // DIR / MENU / BUTTON
    public int sortOrder;
    public List<SysMenu> children = new ArrayList<>();

    public SysMenu() {}
}
