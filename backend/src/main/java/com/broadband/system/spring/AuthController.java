package com.broadband.system.spring;

import com.broadband.common.Ids;
import com.broadband.product.mapper.CustomerMapper;
import com.broadband.product.model.Customer;
import com.broadband.install.mapper.WorkerMapper;
import com.broadband.install.model.Worker;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.wechat.WechatMiniAppService;
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
    @Autowired private WechatMiniAppService wechatService;
    @Autowired private WorkerMapper workerMapper;

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
     * 小程序登录（C 端客户 / 师傅端通用）：手机号 + 短信验证码 或 微信 code。
     *
     * <ul>
     *   <li>当微信小程序 appid/secret 已配置（{@code WECHAT_APPID}/{@code WECHAT_SECRET}）时，
     *       走真实路径：用 {@code code}（wx.login 换取的临时凭证）调 code2Session 拿 openid，
     *       按 openid 查/绑客户，签发 JWT（dept=CUSTOMER）。</li>
     *   <li>未配置微信凭证时回退演示分支：手机号 + 固定验证码 {@code 1234}，签发演示 JWT，
     *       保证本地无真实凭证亦可联调。</li>
     * </ul>
     *
     * <p>配合 {@code app.security.protect-client-api=true}，C 端开放接口现已强制鉴权，
     * 该 token 即小程序访问开放层的凭证（JwtAuthFilter 对 CUSTOMER 建立客户身份 Authentication）。</p>
     */
    @PostMapping("/miniapp-login")
    public Map<String, Object> miniappLogin(@RequestBody Map<String, String> req) {
        String phone = req.get("phone");
        String code = req.get("code");

        Customer c;
        if (wechatService.configured()) {
            // 真实微信小程序登录：code 为 wx.login 换取的临时登录凭证
            WechatMiniAppService.SessionResult s;
            try {
                s = wechatService.code2Session(code);
            } catch (RuntimeException e) {
                throw new BadCredentials("微信登录失败：" + e.getMessage());
            }
            c = customerMapper.selectByOpenid(s.openid);
            if (c == null) {
                c = new Customer();
                c.id = Ids.next();
                c.openid = s.openid;
                c.name = "微信用户" + (s.openid.length() >= 6 ? s.openid.substring(0, 6) : s.openid);
                c.level = "GOLD";
                c.createdTime = System.currentTimeMillis();
                customerMapper.insert(c);
            }
        } else {
            // 演示分支（未配置微信凭证）：手机号 + 固定验证码 1234
            if (phone == null || !phone.matches("^1\\d{10}$")) {
                throw new BadCredentials("请输入正确的手机号");
            }
            if (code == null || !code.equals("1234")) {
                throw new BadCredentials("验证码错误（演示验证码：1234）");
            }
            c = customerMapper.selectOne(
                    new QueryWrapper<Customer>().eq("phone", phone).last("limit 1"));
            if (c == null) {
                // 演示态：返回合成客户，不落库（避免污染种子数据）
                c = new Customer();
                c.id = "demo";
                c.phone = phone;
                c.name = "演示客户";
                c.level = "GOLD";
                c.createdTime = System.currentTimeMillis();
            }
        }

        String subject = (c.phone != null && !c.phone.isEmpty()) ? c.phone : c.openid;
        String token = jwtUtil.issue(subject, c.id, c.name, "CUSTOMER");

        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("id", c.id);
        customer.put("name", c.name);
        customer.put("phone", c.phone);
        customer.put("level", c.level);
        customer.put("openid", c.openid);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", token);
        resp.put("expiresIn", jwtUtil.getTtlSeconds());
        resp.put("wechatBound", wechatService.configured());
        resp.put("customer", customer);
        return resp;
    }

    /**
     * 师傅端登录：手机号 + 短信验证码（演示验证码 {@code 1234}）。
     *
     * <p>按手机号查 {@code worker} 表，签发 JWT（dept=WORKER，uid=worker.id）。
     * JwtAuthFilter 对 WORKER 建立师傅身份 Authentication（ROLE_WORKER + workorder:view + sla:view），
     * 使其能访问工单、容量看板、SLA 评估等师傅端接口，但拿不到后台管理权限。</p>
     */
    @PostMapping("/worker-login")
    public Map<String, Object> workerLogin(@RequestBody Map<String, String> req) {
        String phone = req.get("phone");
        String code = req.get("code");
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new BadCredentials("请输入正确的手机号");
        }
        if (code == null || !code.equals("1234")) {
            throw new BadCredentials("验证码错误（演示验证码：1234）");
        }
        Worker w = workerMapper.selectOne(
                new QueryWrapper<Worker>().eq("phone", phone).last("limit 1"));
        if (w == null) {
            throw new BadCredentials("未找到该师傅账号，请联系管理员");
        }

        String token = jwtUtil.issue(phone, w.id, w.name, "WORKER");

        Map<String, Object> worker = new LinkedHashMap<>();
        worker.put("id", w.id);
        worker.put("name", w.name);
        worker.put("phone", w.phone);
        worker.put("region", w.region);
        worker.put("skillLevel", w.skillLevel);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", token);
        resp.put("expiresIn", jwtUtil.getTtlSeconds());
        resp.put("wechatBound", false);
        resp.put("worker", worker);
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
