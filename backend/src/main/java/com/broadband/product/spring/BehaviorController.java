package com.broadband.product.spring;

import com.broadband.system.security.CustomerPrincipal;
import com.broadband.system.security.WorkerPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 行为埋点（v1.18 新模块 · 埋点基础）。
 *
 * <p>本端点面向 C 端小程序与师傅端，采集用户对「高频入口 / 关键功能」的触达与交互，
 * 为 {@code RecommendController} 的「高频入口智能推荐」提供行为数据底座（对应路线图 US-3.2 的前置依赖）。</p>
 *
 * <p><b>鉴权</b>：列于 {@code SecurityConfig.CLIENT_API}，默认随 C 端开放层 {@code permitAll()}；
 * 当 {@code app.security.protect-client-api=true} 时要求 CUSTOMER / WORKER 令牌。</p>
 *
 * <p><b>身份绑定（IDOR 安全）</b>：优先取当前登录主体 ID；仅在匿名（开发态 {@code protect-client-api=false}）时
 * 回退到请求体 {@code userId}。绝不在受保护态信任客户端传入的 {@code userId} 覆盖主体身份。</p>
 *
 * <p>权限：无独立权限码（采集端无角色概念）；后台读取侧由 {@code recommend:view} 守护。</p>
 */
@RestController
@RequestMapping("/api/behavior")
public class BehaviorController {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 上报一次行为事件。
     * 入参：{ userId?(dev), eventType, entry, entryTitle?, payload? }
     * eventType 建议取值：PAGE_VIEW / CLICK / ORDER / SEARCH / SHARE / PAY
     * entry 为入口/功能标识（如 package_upgrade / community_check / pay / support），用于聚合推荐。
     */
    @PostMapping("/track")
    public Map<String, Object> track(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = resolveUserId(auth, body);
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("无法识别上报主体（请先登录或开发态携带 userId）");
        }
        String userType = resolveUserType(auth);
        String eventType = str(body, "eventType");
        String entry = str(body, "entry");
        if (eventType == null || eventType.isBlank()) eventType = "CLICK";
        if (entry == null || entry.isBlank()) {
            throw new IllegalArgumentException("entry 必填（入口/功能标识）");
        }
        String entryTitle = str(body, "entryTitle");
        if (entryTitle == null) entryTitle = entry;
        String payload = str(body, "payload");
        if (payload == null) payload = "";

        String id = "BEV" + System.nanoTime();
        jdbc.update(
                "INSERT INTO user_behavior_event (id, user_id, user_type, event_type, entry, entry_title, payload, created_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, userId, userType, eventType, entry, entryTitle, payload, System.currentTimeMillis());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("id", id);
        return m;
    }

    // ============================================================ 内部辅助

    private String resolveUserId(Authentication auth, Map<String, Object> body) {
        if (auth != null && auth.getPrincipal() != null) {
            Object p = auth.getPrincipal();
            if (p instanceof CustomerPrincipal cp) return cp.id;
            if (p instanceof WorkerPrincipal wp) return wp.id;
            if (auth.getName() != null && !auth.getName().isBlank()) return auth.getName();
        }
        // 开发态匿名回退（protect-client-api=false）：允许显式 userId 便于联调
        Object v = body.get("userId");
        return v == null ? null : String.valueOf(v);
    }

    private String resolveUserType(Authentication auth) {
        if (auth != null && auth.getPrincipal() != null) {
            Object p = auth.getPrincipal();
            if (p instanceof CustomerPrincipal) return "CUSTOMER";
            if (p instanceof WorkerPrincipal) return "WORKER";
            return "ADMIN";
        }
        return "ANONYMOUS";
    }

    private static String str(Map<String, Object> body, String k) {
        Object v = body.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
