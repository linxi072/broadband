package com.broadband.system.service;

import com.broadband.system.model.LoginUser;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录主体读取器。
 *
 * <p>原先「取当前登录用户」的方法挂在 AuthController 上，导致各业务 Service 反向依赖
 * Web 层（controller）。这里下沉到 service 包，供 controller 与 service 同向调用。</p>
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** 当前登录的后台用户；未认证或主体不是 LoginUser 时返回 {@code null}。 */
    public static LoginUser get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = auth == null ? null : auth.getPrincipal();
        return p instanceof LoginUser lu ? lu : null;
    }
}
