package com.broadband.system.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 配置（无状态 JWT）。
 *
 * <p><b>接口保护分层（有意设计）</b>：</p>
 * <ul>
 *   <li><b>开放层（C 端小程序）</b>：套餐详情/升档、小区校验与需求登记、流量查询。
 *       这些是面向用户的开放在线业务接口，小程序侧的 token（openid/session 换取）尚未落地，
 *       若一并强制鉴权会直接打断「小程序 → 后端 → MySQL」已验证的链路。</li>
 *   <li><b>受保护层（运营/管理端）</b>：/api/dispatch/**、/api/sla/**、/api/admin/**、/api/system/**
 *       —— 全部要求登录，并按权限码（@PreAuthorize）细粒度授权，越权返回 403。</li>
 * </ul>
 *
 * <p>把 <code>app.security.protect-client-api</code> 设为 true 即让开放层也强制鉴权
 * （小程序接入 token 后切换）。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** C 端开放接口（小程序使用，需 CUSTOMER 令牌） */
    private static final String[] CLIENT_API = {
            "/api/package/list",
            "/api/package/detail",
            "/api/package/upgrade-options",
            "/api/package/upgrade",
            "/api/community/check",
            "/api/community/demand",
            "/api/order/my",
            "/api/order/create",
            "/api/order/pay",
            "/api/order/tracking",
            "/api/review/create",
            "/api/review/my",
            "/api/traffic/usage",
            "/api/repair/create",
            "/api/repair/my",
            // V1.14 运营留存（C 端开放层）
            "/api/account/summary",
            "/api/account/bills",
            "/api/points/balance",
            "/api/points/sign",
            "/api/points/tasks",
            "/api/points/mall",
            "/api/points/redeem",
            "/api/promotions",
            "/api/support/faq",
            "/api/support/ticket"
    };

    @Value("${app.security.protect-client-api:false}")
    private boolean protectClientApi;

    @Value("${app.jwt.secret:broadband-rbac-secret-please-change-in-production}")
    private String jwtSecret;

    @Value("${app.jwt.ttl-minutes:120}")
    private long jwtTtlMinutes;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(jwtSecret, jwtTtlMinutes * 60);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                           MustChangePasswordFilter mustChangePasswordFilter,
                                           RestAuthHandlers handlers) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
            .authorizeHttpRequests(reg -> {
                reg.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                reg.requestMatchers("/api/auth/login", "/api/auth/miniapp-login", "/api/auth/worker-login", "/error").permitAll();
                // 监控（G6）：仅暴露 health/info/prometheus 三个只读端点，prometheus 由内网抓取，不对外网开放。
                reg.requestMatchers("/actuator/**").permitAll();
                // 数据字典 / 参数配置 开放读取（C 端小程序下拉与参数获取用，免鉴权）
                reg.requestMatchers("/api/dict/public/**", "/api/config/public/**").permitAll();
                if (!protectClientApi) {
                    reg.requestMatchers(CLIENT_API).permitAll();
                }
                reg.anyRequest().authenticated();
            })
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(mustChangePasswordFilter, JwtAuthFilter.class);

        return http.build();
    }

    /** 开发期 CORS：允许本地前端直连（生产由 Nginx 同源反代，无需 CORS）。 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
