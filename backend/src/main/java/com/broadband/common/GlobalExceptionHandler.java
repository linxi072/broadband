package com.broadband.common;

import com.broadband.system.spring.AuthController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理：把业务异常统一转成 {code, message} 的 JSON，前端 axios 拦截器据此提示。
 *
 * <p>特别处理两类容易「被吞掉」的异常：</p>
 * <ul>
 *   <li>{@link AccessDeniedException}：方法级 @PreAuthorize 在 DispatcherServlet 内抛出，
 *       会被 ControllerAdvice 先接住，若不显式处理就会变成 500 —— 这里转回 403。</li>
 *   <li>{@link NoResourceFoundException}：静态资源/路径不存在，转 404 而不是 500。</li>
 * </ul>
 *
 * <p>未认证（401）由 Spring Security 的 AuthenticationEntryPoint 处理，不会走到这里。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthController.BadCredentials.class)
    public ResponseEntity<Map<String, Object>> badCredentials(AuthController.BadCredentials e) {
        return body(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accessDenied(AccessDeniedException e) {
        return body(HttpStatus.FORBIDDEN, "没有访问该功能的权限");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "接口不存在：" + e.getResourcePath());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> missingParam(MissingServletRequestParameterException e) {
        return body(HttpStatus.BAD_REQUEST, "缺少请求参数：" + e.getParameterName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> illegalArgument(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        log.error("未处理异常", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "服务端异常：" + e.getClass().getSimpleName());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", status.value());
        m.put("message", message == null ? status.getReasonPhrase() : message);
        return ResponseEntity.status(status).body(m);
    }
}
