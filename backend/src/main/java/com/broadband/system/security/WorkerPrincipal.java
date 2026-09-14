package com.broadband.system.security;

/**
 * 师傅端登录主体（dept=WORKER）。
 *
 * <p>与 {@link CustomerPrincipal} 类似，不映射到后台 sys_user，仅承载师傅身份，
 * 并附带师傅端所需的权限码（工单查看、SLA 评估），以便通过 Spring Security 的
 * 方法级 {@code @PreAuthorize} 关卡。</p>
 */
public class WorkerPrincipal {
    public final String id;
    public final String phone;
    public final String name;

    public WorkerPrincipal(String id, String phone, String name) {
        this.id = id;
        this.phone = phone;
        this.name = name;
    }

    public String getId() { return id; }

    public String getUsername() { return phone; }

    public String getName() { return name; }
}
