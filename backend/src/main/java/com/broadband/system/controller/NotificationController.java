package com.broadband.system.controller;

import com.broadband.system.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息通知中心控制器（v1.23 新模块 · PC 后台）。
 *
 * <p>权限 {@code notification:view}（M240/M241 菜单授权 ADMIN/OPERATOR）。</p>
 *
 * <p>与既有模块集成：各业务模块在服务层调用 {@link NotificationService#notify} 产生通知；
 * 本控制器仅负责查阅/已读/管理端主动推送。角色过滤基于当前登录主体的 authority
 * （{@code ROLE_ADMIN}/{@code ROLE_OPERATOR}），无法解析时回退 {@code ALL}（展示全部）。</p>
 *
 * <p>注意：本文件所属后端模块在受限沙箱无法执行 Maven 构建，需在具备本地仓库的机器上
 * {@code mvn -o compile} 验证编译通过后再合并。</p>
 */
@RestController
@RequestMapping("/api/admin/notification")
public class NotificationController {

    @Autowired
    private NotificationService svc;

    // ============================================================ 通知列表 / 未读计数

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('notification:view')")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return svc.list(page, size, unreadOnly, currentRole());
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('notification:view')")
    public Map<String, Object> unreadCount() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", svc.unreadCount(currentRole()));
        return m;
    }

    // ============================================================ 已读

    @PostMapping("/mark-read")
    @PreAuthorize("hasAuthority('notification:view')")
    public Map<String, Object> markRead(@RequestBody Map<String, Object> body) {
        List<String> ids = extractIds(body.get("ids"));
        int n = svc.markRead(ids, currentRole());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("updated", n);
        return m;
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("hasAuthority('notification:view')")
    public Map<String, Object> markAllRead() {
        int n = svc.markAllRead(currentRole());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("updated", n);
        return m;
    }

    // ============================================================ 管理端主动推送

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('notification:view')")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        List<String> ids = svc.create(str(body, "title"), str(body, "content"),
                str(body, "eventType"), str(body, "refType"), str(body, "refId"), str(body, "targetRole"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("ids", ids);
        return m;
    }

    // ============================================================ 内部辅助

    /** 从请求体提取 ids 列表（兼容 JSON 数组，保证类型安全） */
    @SuppressWarnings("unchecked")
    private static List<String> extractIds(Object raw) {
        List<String> ids = new ArrayList<>();
        if (raw instanceof List) {
            for (Object o : (List<Object>) raw) {
                if (o != null) {
                    ids.add(String.valueOf(o));
                }
            }
        }
        return ids;
    }

    /** 当前登录主体的角色：优先取 authority，无法解析时回退 ALL（展示全部）。 */
    private String currentRole() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities() != null) {
                for (var a : auth.getAuthorities()) {
                    String r = a.getAuthority();
                    if (r != null) {
                        if (r.equals("ROLE_ADMIN") || r.equals("ADMIN")) {
                            return "ADMIN";
                        }
                        if (r.equals("ROLE_OPERATOR") || r.equals("OPERATOR")) {
                            return "OPERATOR";
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 保留回退
        }
        return "ALL";
    }

    private static String str(Map<String, Object> body, String k) {
        Object v = body.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
