package com.broadband.system.controller;

import com.broadband.system.security.RestAuthHandlers;
import com.broadband.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口。
 *
 * <ul>
 *   <li>POST /api/auth/login  —— 账号密码登录，签发 JWT（登录成功/失败均留痕）</li>
 *   <li>POST /api/auth/miniapp-login —— 微信小程序登录（C 端客户）</li>
 *   <li>POST /api/auth/worker-login  —— 师傅端登录（手机号 + 短信验证码）</li>
 *   <li>GET  /api/auth/me     —— 当前用户 + 角色 + 权限码 + 菜单树（前端据此渲染侧边栏）</li>
 *   <li>POST /api/auth/logout —— 无状态登出（前端丢弃 token；此处仅留痕）</li>
 *   <li>POST /api/auth/change-password —— 自服务改密</li>
 * </ul>
 *
 * <p>本类只做参数绑定与 HTTP 响应，认证逻辑见 {@link AuthService}。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> req, HttpServletRequest http) {
        return authService.login(req.get("username"), req.get("password"), RestAuthHandlers.clientIp(http));
    }

    /** 小程序登录（C 端客户）：微信 code → openid → 查/绑客户，签发 JWT（dept=CUSTOMER）。 */
    @PostMapping("/miniapp-login")
    public Map<String, Object> miniappLogin(@RequestBody Map<String, String> req) {
        return authService.miniappLogin(req.get("code"));
    }

    /** 师傅端登录：手机号 + 短信验证码。 */
    @PostMapping("/worker-login")
    public Map<String, Object> workerLogin(@RequestBody Map<String, String> req) {
        return authService.workerLogin(req.get("phone"), req.get("code"));
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return authService.me();
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest http) {
        return authService.logout(RestAuthHandlers.clientIp(http));
    }

    /** 自服务改密（T-02 安全治理）：已登录用户凭旧密码修改自己的密码。 */
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> req, HttpServletRequest http) {
        return authService.changePassword(req.get("oldPassword"), req.get("newPassword"),
                RestAuthHandlers.clientIp(http));
    }
}
