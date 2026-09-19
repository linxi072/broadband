package com.broadband.system.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.broadband.system.service.OperLogService;

import java.io.IOException;

/**
 * 统一鉴权失败响应：401（未认证）/ 403（无权限）返回 JSON，并对越权尝试写操作日志。
 */
@Component
public class RestAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final OperLogService operLogService;

    public RestAuthHandlers(OperLogService operLogService) {
        this.operLogService = operLogService;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        logDenied(request, "未认证(401)", "未携带或携带无效 token 访问受保护接口");
        write(response, 401, "未认证或登录已过期，请重新登录");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        logDenied(request, "拒绝(403)", "已登录但缺少所需权限码");
        write(response, 403, "没有访问该功能的权限");
    }

    private void logDenied(HttpServletRequest request, String result, String action) {
        operLogService.record(null, null, action + " " + result,
                request.getRequestURI(), request.getMethod(),
                clientIp(request), result, 0);
    }

    private static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }

    public static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real;
        return request.getRemoteAddr();
    }
}
