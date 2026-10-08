package com.broadband.product.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

/**
 * 微信支付 APIv3 实现（仅在 {@code wechat.pay.enabled=true} 时由 {@link WechatPayConfig} 装配）。
 *
 * <p>下单采用 JSAPI：服务端统一下单拿到 prepay_id → 组装 wx.requestPayment 参数返回给小程序 →
 * 用户支付 → 微信异步通知 /api/pay/wechat/notify（或 /api/pay/notify）→ 由回调把订单置 PAID。
 * 因此本实现 {@code createPayment} 仅返回支付参数（awaitPayment），不直接置 PAID，避免伪造支付推进履约。</p>
 *
 * <p>实现 {@link PayService} 接口（createPayment / verifyAndParse / refund），与
 * {@link MockWeChatPayServiceImpl} 可互换：默认（enabled=false）由 Mock 接管，生产启用后本类自动生效。</p>
 */
public class WechatPayServiceImpl implements PayService {

    private final WechatPayV3Client client;
    private final WechatPayProperties props;
    private final ObjectMapper om = new ObjectMapper();

    public WechatPayServiceImpl(WechatPayV3Client client, WechatPayProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public PayOrder createPayment(String bizOrderId, int amount, String channel, String clientIp, String openid) {
        if (!"WECHAT".equalsIgnoreCase(channel) && !"WECHAT_PAY".equalsIgnoreCase(channel)) {
            throw new IllegalStateException("暂仅支持微信支付(WECHAT)");
        }
        if (openid == null || openid.isBlank()) {
            throw new IllegalStateException("缺少用户 openid，无法发起 JSAPI 支付");
        }
        String outTradeNo = "OUT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
        String prepayId = client.jsapiPrepay(outTradeNo, amount, "宽带业务订单-" + bizOrderId, openid, null);
        if (prepayId == null) {
            throw new IllegalStateException("微信统一下单未返回 prepay_id");
        }
        Map<String, String> params = client.buildPayParams(prepayId);
        String payParams;
        try {
            payParams = om.writeValueAsString(params);
        } catch (Exception e) {
            throw new IllegalStateException("支付参数序列化失败", e);
        }
        long expireAt = System.currentTimeMillis() + 30L * 60 * 1000;
        return new PayOrder(outTradeNo, channel, payParams, expireAt, "PAYING");
    }

    @Override
    public NotifyResult verifyAndParse(Map<String, String> headers, String body) {
        String timestamp = headers.get("Wechatpay-Timestamp");
        String nonce = headers.get("Wechatpay-Nonce");
        String signature = headers.get("Wechatpay-Signature");
        String serial = headers.get("Wechatpay-Serial");
        if (timestamp == null || nonce == null || signature == null || serial == null || body == null) {
            return new NotifyResult(false, null, null, "FAIL");
        }
        if (!client.verifyNotify(timestamp, nonce, body, signature, serial)) {
            return new NotifyResult(false, null, null, "FAIL");
        }
        try {
            JsonNode root = om.readTree(body);
            JsonNode res = root.path("resource");
            String plain = client.decryptResource(
                    res.path("ciphertext").asText(),
                    res.path("nonce").asText(),
                    res.path("associated_data").asText(""));
            JsonNode dec = om.readTree(plain);
            String outTradeNo = dec.path("out_trade_no").asText();
            String transactionId = dec.path("transaction_id").asText();
            String state = dec.path("trade_state").asText();
            boolean ok = "SUCCESS".equalsIgnoreCase(state) && outTradeNo != null && !outTradeNo.isEmpty();
            return new NotifyResult(ok, outTradeNo, transactionId, ok ? "SUCCESS" : "FAIL");
        } catch (Exception e) {
            return new NotifyResult(false, null, null, "FAIL");
        }
    }

    @Override
    public RefundResult refund(String outTradeNo, int amount, String reason) {
        String outRefundNo = "RFX" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
        String refundId = client.refund(outTradeNo, outRefundNo, amount, reason);
        return new RefundResult(refundId != null, refundId, refundId != null ? "退款已受理" : "微信退款调用失败");
    }
}
