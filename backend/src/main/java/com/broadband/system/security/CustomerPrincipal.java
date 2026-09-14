package com.broadband.system.security;

/**
 * 小程序客户身份载体（dept=CUSTOMER）。
 *
 * <p>客户数据在 {@code customer} 表、不在 {@code sys_user} 表，因此不承载后台角色/权限码；
 * 仅用于通过「已认证」关卡，使其能访问 C 端开放接口（这些接口无角色/权限码约束）。</p>
 */
public class CustomerPrincipal {
    public final String id;
    public final String username;
    public final String name;

    public CustomerPrincipal(String id, String username, String name) {
        this.id = id;
        this.username = username;
        this.name = name;
    }

    @Override
    public String toString() {
        return name != null ? name : username;
    }
}
