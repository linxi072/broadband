package com.broadband.system.security;

import com.broadband.system.mapper.SysUserMapper;
import com.broadband.system.model.LoginUser;
import com.broadband.system.model.SysRole;
import com.broadband.system.model.SysUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JWT 鉴权过滤器：解析 Bearer token → 载入用户/角色/权限码 → 写入 SecurityContext。
 *
 * <p>角色与权限码「每请求实时查库」而不是固化在 token 里，因此后台改角色/授权后立即生效。</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, SysUserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        log.debug("[jwt] uri={} headerPresent={} value={}", request.getRequestURI(),
                header != null, header == null ? "-" : header.substring(0, Math.min(16, header.length())));
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Map<String, Object> claims = jwtUtil.verify(header.substring(7).trim());
                String username = str(claims.get("sub"));
                log.debug("[jwt] verified sub={} preAuth={}", username,
                        SecurityContextHolder.getContext().getAuthentication() != null);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    authenticate(username, request);
                    log.debug("[jwt] authenticated={}",
                            SecurityContextHolder.getContext().getAuthentication() != null);
                }
            } catch (Exception e) {
                // token 无效/过期：不设置认证信息，由 EntryPoint 统一返回 401。
                // 必须打日志：这里曾静默吞掉 JwtUtil 的解析异常，导致「token 完全正确却始终 401」
                // 且启动日志毫无线索，排查成本极高。
                log.warn("JWT 鉴权失败 uri={} 原因={}: {}",
                        request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String username, HttpServletRequest request) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null || !"ENABLED".equals(user.status)) return;

        List<SysRole> roles = userMapper.selectRoles(user.id);
        List<String> perms = userMapper.selectPerms(user.id);

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (SysRole r : roles) authorities.add(new SimpleGrantedAuthority("ROLE_" + r.code));
        for (String p : perms) authorities.add(new SimpleGrantedAuthority(p));

        LoginUser principal = new LoginUser(user, roles, perms);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
