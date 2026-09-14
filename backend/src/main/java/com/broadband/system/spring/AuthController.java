package com.broadband.system.spring;

import com.broadband.product.mapper.CustomerMapper;
import com.broadband.product.model.Customer;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.mapper.SysUserMapper;
import com.broadband.system.model.LoginUser;
import com.broadband.system.model.SysMenu;
import com.broadband.system.model.SysUser;
import com.broadband.system.security.JwtUtil;
import com.broadband.system.security.RestAuthHandlers;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证接口。
 *
 * <ul>
 *   <li>POST /api/auth/login  —— 账号密码登录，签发 JWT（登录成功/失败均留痕）</li>
 *   <li>GET  /api/auth/me     —— 当前用户 + 角色 + 权限码 + 菜单树（前端据此渲染侧边栏）</li>
 *   <li>POST /api/auth/logout —— 无状态登出（前端丢弃 token；此处仅留痕）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private SysUserMapper userMapper;
    @Autowired private SysMenuMapper menuMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private OperLogService operLogService;
    @Autowired private CustomerMapper customerMapper;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> req, HttpServletRequest http) {
        String username = req.get("username") == null ? null : req.get("username").trim();
        String password = req.get("password");
        long begin = System.currentTimeMillis();
        String ip = RestAuthHandlers.clientIp(http);

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            operLogService.record(username, null, "登录", "/api/auth/login", "POST", ip,
                    "失败(参数缺失)", System.currentTimeMillis() - begin);
            throw new BadCredentials("请输入账号与密码");
        }

        SysUser user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.password)) {
            operLogService.record(username, user == null ? null : user.name, "登录", "/api/auth/login",
                    "POST", ip, "失败(账号或密码错误)", System.currentTimeMillis() - begin);
            throw new BadCredentials("账号或密码错误");
        }
        if (!"ENABLED".equals(user.status)) {
            operLogService.record(username, user.name, "登录", "/api/auth/login", "POST", ip,
                    "失败(账号已禁用)", System.currentTimeMillis() - begin);
            throw new BadCredentials("账号已被禁用，请联系管理员");
        }

        String token = jwtUtil.issue(user.username, user.id, user.name, user.dept);
        operLogService.record(username, user.name, "登录", "/api/auth/login", "POST", ip,
                "成功", System.currentTimeMillis() - begin);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", token);
        resp.put("expiresIn", jwtUtil.getTtlSeconds());
        resp.put("demo", false);
        resp.put("user", profile(user));
        return resp;
    }

    /**
     * 小程序登录（C 端客户 / 师傅端通用）：手机号 + 短信验证码。
     *
     * <p>演示实现：验证码固定为 {@code 1234}；按手机号查客户档案，未注册则回退到演示档案
     * （customerId=demo）。签发 JWT（dept=CUSTOMER），该 token 由于没有对应 sys_user，
     * 在 JwtAuthFilter 中不会映射到后台权限，仅用于标识客户身份（c 端开放层本就无需鉴权）。
     * 小程序侧把 {@code app.security.protect-client-api} 置 true 后，开放层转为强制鉴权，
     * 届时需在此做「微信 code → openid → 绑定客户」的真实换取。</p>
     */
    @PostMapping("/miniapp-login")
    public Map<String, Object> miniappLogin(@RequestBody Map<String, String> req) {
        String phone = req.get("phone");
        String code = req.get("code");
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new BadCredentials("请输入正确的手机号");
        }
        if (code == null || !code.equals("1234")) {
            throw new BadCredentials("验证码错误（演示验证码：1234）");
        }

        Customer c = customerMapper.selectOne(
                new QueryWrapper<Customer>().eq("phone", phone).last("limit 1"));
        String customerId = c == null ? "demo" : c.id;
        String customerName = c == null ? "演示客户" : c.name;
        String level = c == null ? "GOLD" : c.level;

        String token = jwtUtil.issue(phone, customerId, customerName, "CUSTOMER");

        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("id", customerId);
        customer.put("name", customerName);
        customer.put("phone", phone);
        customer.put("level", level);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", token);
        resp.put("expiresIn", jwtUtil.getTtlSeconds());
        resp.put("customer", customer);
        return resp;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        LoginUser login = current();
        if (login == null) throw new BadCredentials("未认证");

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("user", profile(login.user));
        resp.put("roles", login.roleCodes());
        resp.put("roleNames", login.roleNames());
        resp.put("perms", login.perms);
        resp.put("menus", tree(menuMapper.selectByUser(login.user.id)));
        return resp;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest http) {
        LoginUser login = current();
        operLogService.record(login == null ? null : login.user.username,
                login == null ? null : login.user.name,
                "退出登录", "/api/auth/logout", "POST", RestAuthHandlers.clientIp(http), "成功", 0);
        return Map.of("ok", true);
    }

    // ------------------------------------------------------------------ 辅助

    public static LoginUser current() {
        Object p = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return p instanceof LoginUser lu ? lu : null;
    }

    private Map<String, Object> profile(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id);
        m.put("username", u.username);
        m.put("name", u.name);
        m.put("dept", u.dept);
        m.put("status", u.status);
        return m;
    }

    /** 平铺菜单列表 → 树（parentId 为空、或父节点未被授权时，作为根节点呈现）。 */
    private List<SysMenu> tree(List<SysMenu> flat) {
        Map<String, SysMenu> index = new HashMap<>();
        for (SysMenu m : flat) index.put(m.id, m);

        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : flat) {
            if (m.parentId == null || m.parentId.isEmpty() || !index.containsKey(m.parentId)) {
                roots.add(m);
            } else {
                index.get(m.parentId).children.add(m);
            }
        }
        return roots;
    }

    /** 登录失败（401），由全局异常处理转成 JSON。 */
    public static class BadCredentials extends RuntimeException {
        public BadCredentials(String message) {
            super(message);
        }
    }
}
