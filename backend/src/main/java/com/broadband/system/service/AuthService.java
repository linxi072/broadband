package com.broadband.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.BadCredentials;
import com.broadband.common.Ids;
import com.broadband.install.mapper.WorkerMapper;
import com.broadband.install.model.Worker;
import com.broadband.product.mapper.CustomerMapper;
import com.broadband.product.model.Customer;
import com.broadband.system.mapper.SysMenuMapper;
import com.broadband.system.mapper.SysUserMapper;
import com.broadband.system.model.LoginUser;
import com.broadband.system.model.SysMenu;
import com.broadband.system.model.SysUser;
import com.broadband.system.security.JwtUtil;
import com.broadband.system.wechat.WechatMiniAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证业务：后台账号密码登录、微信小程序登录、师傅端登录、当前用户信息、自服务改密。
 *
 * <p>Controller 只负责接收请求与写 HTTP 响应，令牌签发、账号校验、留痕全部收敛在本类。</p>
 */
@Service
public class AuthService {

    @Autowired private SysUserMapper userMapper;
    @Autowired private SysMenuMapper menuMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private OperLogService operLogService;
    @Autowired private CustomerMapper customerMapper;
    @Autowired private WechatMiniAppService wechatService;
    @Autowired private WorkerMapper workerMapper;
    @Autowired private SmsVerifyService smsVerifyService;

    // ==================================================================== 后台登录

    /**
     * 账号密码登录：校验账号 / 密码 / 状态，签发 JWT（登录成功与失败均留痕）。
     * @throws BadCredentials 参数缺失、账号密码错误或账号被禁用
     */
    public Map<String, Object> login(String username, String password, String ip) {
        long begin = System.currentTimeMillis();
        String name = username == null ? null : username.trim();

        if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
            operLogService.record(name, null, "登录", "/api/auth/login", "POST", ip,
                    "失败(参数缺失)", System.currentTimeMillis() - begin);
            throw new BadCredentials("请输入账号与密码");
        }

        SysUser user = userMapper.selectByUsername(name);
        if (user == null || !passwordEncoder.matches(password, user.password)) {
            operLogService.record(name, user == null ? null : user.name, "登录", "/api/auth/login",
                    "POST", ip, "失败(账号或密码错误)", System.currentTimeMillis() - begin);
            throw new BadCredentials("账号或密码错误");
        }
        if (!"ENABLED".equals(user.status)) {
            operLogService.record(name, user.name, "登录", "/api/auth/login", "POST", ip,
                    "失败(账号已禁用)", System.currentTimeMillis() - begin);
            throw new BadCredentials("账号已被禁用，请联系管理员");
        }

        // JWT 的 dept 声明是 token 类型判别符（SYS/CUSTOMER/WORKER），后台用户固定为 SYS；
        // 组织部门 deptId 由 LoginUser.user.deptId 承载，用于数据权限（部门级行隔离）。
        String token = jwtUtil.issue(user.username, user.id, user.name, "SYS");
        operLogService.record(name, user.name, "登录", "/api/auth/login", "POST", ip,
                "成功", System.currentTimeMillis() - begin);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", token);
        resp.put("expiresIn", jwtUtil.getTtlSeconds());
        resp.put("user", profile(user));
        return resp;
    }

    // ==================================================================== 小程序登录

    /**
     * 小程序登录（C 端客户）：微信 code → openid → 查/绑客户，签发 JWT（dept=CUSTOMER）。
     *
     * <p>必须配置微信小程序凭证（{@code WECHAT_APPID}/{@code WECHAT_SECRET}）才可用。
     * 已移除「手机号 + 固定验证码」的演示分支（等同免验证登录）；未配置凭证时直接拒绝。</p>
     */
    public Map<String, Object> miniappLogin(String code) {
        if (!wechatService.configured()) {
            throw new BadCredentials(
                    "小程序登录需配置微信小程序凭证（环境变量 WECHAT_APPID / WECHAT_SECRET），当前未配置");
        }
        if (code == null || code.isEmpty()) {
            throw new BadCredentials("缺少微信登录凭证 code");
        }

        WechatMiniAppService.SessionResult s;
        try {
            s = wechatService.code2Session(code);
        } catch (RuntimeException e) {
            throw new BadCredentials("微信登录失败：" + e.getMessage());
        }
        Customer c = customerMapper.selectByOpenid(s.openid);
        if (c == null) {
            c = new Customer();
            c.id = Ids.next();
            c.openid = s.openid;
            c.name = "微信用户" + (s.openid.length() >= 6 ? s.openid.substring(0, 6) : s.openid);
            c.level = "GOLD";
            c.createdTime = System.currentTimeMillis();
            customerMapper.insert(c);
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

    // ==================================================================== 师傅端登录

    /**
     * 师傅端登录：手机号 + 短信验证码。
     *
     * <p>验证码由 {@link SmsVerifyService} 校验：未接入短信服务商时校验一律失败（fail-closed），
     * 需配置 {@code app.sms.enabled=true} 并对接真实服务商后才可登录。</p>
     */
    public Map<String, Object> workerLogin(String phone, String code) {
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new BadCredentials("请输入正确的手机号");
        }
        if (!smsVerifyService.verify(phone, code)) {
            throw new BadCredentials(smsVerifyService.enabled()
                    ? "验证码错误或已失效"
                    : "未接入短信验证码服务，师傅端登录暂不可用，请联系管理员");
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

    // ==================================================================== 当前用户

    /** 当前登录用户 + 角色 + 权限码 + 菜单树（前端据此渲染侧边栏）。 */
    public Map<String, Object> me() {
        LoginUser login = CurrentUser.get();
        if (login == null) throw new BadCredentials("未认证");

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("user", profile(login.user));
        resp.put("roles", login.roleCodes());
        resp.put("roleNames", login.roleNames());
        resp.put("perms", login.perms);
        resp.put("menus", tree(menuMapper.selectByUser(login.user.id)));
        return resp;
    }

    /** 无状态登出（前端丢弃 token；此处仅留痕）。 */
    public Map<String, Object> logout(String ip) {
        LoginUser login = CurrentUser.get();
        operLogService.record(login == null ? null : login.user.username,
                login == null ? null : login.user.name,
                "退出登录", "/api/auth/logout", "POST", ip, "成功", 0);
        return Map.of("ok", true);
    }

    // ==================================================================== 自服务改密

    /**
     * 自服务改密（T-02 安全治理）：已登录用户凭旧密码修改自己的密码。
     * 若账号处于「首次登录必须改密」状态，改密成功后自动清除标记。
     */
    public Map<String, Object> changePassword(String oldPassword, String newPassword, String ip) {
        LoginUser me = CurrentUser.get();
        if (me == null) throw new BadCredentials("未认证");
        if (oldPassword == null || oldPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("旧密码与新密码均不能为空");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        SysUser u = userMapper.selectById(me.user.id);
        if (u == null) throw new BadCredentials("用户不存在");
        if (!passwordEncoder.matches(oldPassword, u.password)) {
            operLogService.record(me.user.username, me.user.name, "修改密码", "/api/auth/change-password",
                    "POST", ip, "失败(旧密码错误)", 0);
            throw new BadCredentials("旧密码错误");
        }
        // 先载入完整实体再更新，避免 updateById 覆盖 createdTime/status 等字段（MyBatis-Plus 全字段更新语义）。
        u.password = passwordEncoder.encode(newPassword);
        u.mustChangePassword = 0;
        userMapper.updateById(u);
        operLogService.record(me.user.username, me.user.name, "修改密码", "/api/auth/change-password",
                "POST", ip, "成功", 0);
        return Map.of("ok", true, "mustChangePassword", 0);
    }

    // ==================================================================== 辅助

    private Map<String, Object> profile(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.id);
        m.put("username", u.username);
        m.put("name", u.name);
        m.put("deptId", u.deptId);
        m.put("status", u.status);
        m.put("mustChangePassword", u.mustChangePassword);
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
}
