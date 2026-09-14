package com.broadband.system.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 微信小程序登录：code2Session 换取 openid / session_key。
 *
 * <p>appid/secret 通过环境变量 {@code WECHAT_APPID} / {@code WECHAT_SECRET} 注入
 * （application.yml 仅占位、不落明文）。未配置时 {@link #configured()} 返回 false，
 * 调用方（AuthController）回退到演示登录分支，保证本地无真实凭证亦可联调。</p>
 */
@Service
public class WechatMiniAppService {

    @Value("${app.wechat.miniapp.appid:}")
    private String appid;

    @Value("${app.wechat.miniapp.secret:}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 微信小程序凭证是否已配置（决定是否走真实换取）。 */
    public boolean configured() {
        return appid != null && !appid.isBlank() && secret != null && !secret.isBlank();
    }

    /** 调用微信 jscode2session 换取会话信息。 */
    public SessionResult code2Session(String code) {
        if (!configured()) {
            throw new IllegalStateException("微信小程序 appid/secret 未配置，无法走真实登录");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + appid
                + "&secret=" + secret
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        String body;
        try {
            body = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new RuntimeException("调用微信 jscode2session 网络失败：" + e.getMessage(), e);
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("errcode") && node.get("errcode").asInt(0) != 0) {
                String errmsg = node.has("errmsg") ? node.get("errmsg").asText() : "未知错误";
                throw new RuntimeException("errcode=" + node.get("errcode").asInt() + "，" + errmsg);
            }
            SessionResult r = new SessionResult();
            r.openid = node.has("openid") ? node.get("openid").asText() : null;
            r.sessionKey = node.has("session_key") ? node.get("session_key").asText() : null;
            r.unionid = node.has("unionid") ? node.get("unionid").asText() : null;
            if (r.openid == null || r.openid.isEmpty()) {
                throw new RuntimeException("微信未返回 openid：" + body);
            }
            return r;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("解析微信返回失败：" + body, e);
        }
    }

    /** 微信会话结果。 */
    public static class SessionResult {
        public String openid;
        public String sessionKey;
        public String unionid;
    }
}
