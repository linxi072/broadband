package com.broadband.system.security;

import com.broadband.system.model.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 首登强制改密拦截器（T-02 安全治理，M15 上线硬性门禁）。
 *
 * <p>对处于「首次登录必须改密」状态（{@code SysUser.mustChangePassword == 1}）的后台用户，
 * 仅放行改密相关与基础接口，其余后台请求一律返回 403，直到其成功改密。</p>
 *
 * <p>为什么放服务端而非只靠前端提示：前端提示可被绕过（直接调接口），
 * 真正的强制必须由服务端保证。改密成功后该标记清零，且 {@link JwtAuthFilter}
 * 每请求实时查库重建主体，因此后续请求立即解除拦截，无需重新登录。</p>
 *
 * <p>仅作用于后台用户主体（{@link LoginUser}）；CUSTOMER / WORKER 主体无此标记，直接放行。</p>
 */
@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED = Set.of(
            "/api/auth/me", "/api/auth/logout", "/api/auth/change-password", "/error");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof LoginUser lu && lu.user.mustChangePassword == 1) {
                String uri = request.getRequestURI();
                if (!ALLOWED.contains(uri) && !"OPTIONS".equals(request.getMethod())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"code\":403,\"msg\":\"首次登录必须修改密码后方可继续使用，请先修改密码\"}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
