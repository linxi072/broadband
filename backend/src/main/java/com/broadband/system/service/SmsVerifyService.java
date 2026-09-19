package com.broadband.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 短信验证码校验服务（师傅端登录 / 客户手机号登录的验证码校验接入点）。
 *
 * <p><b>为什么存在：</b>原实现把验证码硬编码为固定值 {@code 1234}，属测试数据，
 * 且意味着「任何人都能用任意手机号登录」，是实打实的安全漏洞。移除测试数据的同时
 * 必须补上真实的校验入口，否则就是把漏洞从「固定验证码」变成「无需验证码」。</p>
 *
 * <p><b>失败关闭（fail-closed）原则：</b>未接入短信服务商（{@code app.sms.enabled=false}）
 * 或尚未实现真实校验时，一律返回 {@code false}，即登录被拒绝，绝不放行。</p>
 *
 * <p><b>接入方式：</b>配置 {@code SMS_ENABLED=true} 及对应服务商凭证后，
 * 在 {@link #verify(String, String)} 中调用服务商的校验接口
 * （下发时把 code 存入 Redis/DB 并设置 5 分钟过期，此处比对并一次性作废）。
 * 本类即为该对接的唯一位置，无需改动 Controller。</p>
 */
@Service
public class SmsVerifyService {

    private static final Logger log = LoggerFactory.getLogger(SmsVerifyService.class);

    /** 是否已接入短信服务商（由 app.sms.enabled 控制，默认 false = 未接入）。 */
    private final boolean enabled;

    public SmsVerifyService(@Value("${app.sms.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    /**
     * 校验手机号 + 验证码。
     *
     * @param phone 手机号（11 位，1 开头）
     * @param code  用户输入的短信验证码
     * @return true 表示校验通过；false 表示拒绝（未接入 / 验证码错误 / 已过期）
     */
    public boolean verify(String phone, String code) {
        if (phone == null || code == null || code.isEmpty()) {
            return false;
        }
        if (!enabled) {
            log.warn("短信验证码校验被拒绝：未接入短信服务商（app.sms.enabled=false），phone={}", phone);
            return false;
        }
        // 接入点：调用短信服务商校验 code 与该手机号下发的一次性验证码是否匹配。
        // 未实现前保持拒绝（fail-closed），避免留下「任意验证码可登录」的漏洞。
        log.warn("短信验证码校验被拒绝：app.sms.enabled=true 但尚未接入服务商实现，phone={}", phone);
        return false;
    }
}
