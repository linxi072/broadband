package com.broadband.product.pay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付集成装配。
 *
 * <p><b>安全默认</b>：{@code wechat.pay.enabled} 缺省为 false，不创建任何支付 Bean。
 * 此时 {@code OrderService.pay()} 仍走「无 PayService 实现 → 拒绝支付」的安全分支，
 * 现有运行时行为完全不变。</p>
 *
 * <p><b>凭证到位后</b>：在 application.yml / 环境变量设 {@code wechat.pay.enabled=true}
 * 并填齐下列项，即自动装配真实微信支付（T-03 闭环）。</p>
 */
@Configuration
public class WechatPayConfig {

    @Bean
    @ConditionalOnProperty(name = "wechat.pay.enabled", havingValue = "true")
    public WechatPayProperties wechatPayProperties(
            @Value("${wechat.pay.mch-id:}") String mchId,
            @Value("${wechat.pay.app-id:}") String appId,
            @Value("${wechat.pay.api-v3-key:}") String apiV3Key,
            @Value("${wechat.pay.serial-no:}") String serialNo,
            @Value("${wechat.pay.private-key:}") String privateKey,
            @Value("${wechat.pay.notify-url:}") String notifyUrl) {
        return new WechatPayProperties(mchId, appId, apiV3Key, serialNo, privateKey, notifyUrl);
    }

    @Bean
    @ConditionalOnProperty(name = "wechat.pay.enabled", havingValue = "true")
    public WechatPayV3Client wechatPayV3Client(WechatPayProperties props) {
        if (!props.isReady()) {
            throw new IllegalStateException("微信支付已启用但配置不完整，请检查 wechat.pay.* 配置");
        }
        return new WechatPayV3Client(props);
    }

    @Bean
    @ConditionalOnProperty(name = "wechat.pay.enabled", havingValue = "true")
    public WechatPayServiceImpl wechatPayService(WechatPayV3Client client, WechatPayProperties props) {
        return new WechatPayServiceImpl(client, props);
    }
}
