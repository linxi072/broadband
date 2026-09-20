package com.broadband.product.pay;

/**
 * 微信支付 APIv3 配置项（敏感信息，走 application.yml / 环境变量，不落 sys_config）。
 *
 * <p>全部为可选：仅当 {@code wechat.pay.enabled=true} 且关键项齐备时才创建支付 Bean，
 * 否则保持「无 PayService 实现 → 支付被拒绝」的安全默认（见 {@link OrderService}）。</p>
 */
public class WechatPayProperties {

    private final String mchId;        // 商户号
    private final String appId;        // 小程序/公众号 AppID（师傅端、客户端各自不同，此处为客户端）
    private final String apiV3Key;     // APIv3 密钥（32 字节，用于回调解密）
    private final String serialNo;     // 商户证书序列号
    private final String privateKey;   // 商户 API 私钥（PEM，PKCS#8）
    private final String notifyUrl;    // 支付结果通知地址（公网可访问）

    public WechatPayProperties(String mchId, String appId, String apiV3Key,
                               String serialNo, String privateKey, String notifyUrl) {
        this.mchId = mchId;
        this.appId = appId;
        this.apiV3Key = apiV3Key;
        this.serialNo = serialNo;
        this.privateKey = privateKey;
        this.notifyUrl = notifyUrl;
    }

    public String getMchId() { return mchId; }
    public String getAppId() { return appId; }
    public String getApiV3Key() { return apiV3Key; }
    public String getSerialNo() { return serialNo; }
    public String getPrivateKey() { return privateKey; }
    public String getNotifyUrl() { return notifyUrl; }

    /** 关键项是否齐备（用于控制是否放行真实支付）。 */
    public boolean isReady() {
        return isNotBlank(mchId) && isNotBlank(appId) && isNotBlank(apiV3Key)
                && isNotBlank(serialNo) && isNotBlank(privateKey) && isNotBlank(notifyUrl);
    }

    private static boolean isNotBlank(String s) { return s != null && !s.trim().isEmpty(); }
}
