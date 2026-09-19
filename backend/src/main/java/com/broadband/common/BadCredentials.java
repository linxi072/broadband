package com.broadband.common;

/**
 * 认证失败异常（401），由 {@code GlobalExceptionHandler} 转成 JSON 响应。
 *
 * <p>原先作为 AuthController 的内部类存在，导致 common 层的全局异常处理器反向依赖
 * system.controller。下沉到 common 包后，依赖方向变为单向：controller/service → common。</p>
 */
public class BadCredentials extends RuntimeException {

    public BadCredentials(String message) {
        super(message);
    }
}
