package com.broadband.product.pay;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 微信支付 APIv3 实现（仅在 {@code wechat.pay.enabled=true} 时由 {@link WechatPayConfig} 装配）。
 *
 * <p>下单采用 JSAPI：服务端统一下单拿到 prepay_id → 组装 wx.requestPayment 参数返回给小程序 →
 * 用户支付 → 微信异步通知 {@code /api/pay/wechat/notify} → 由回调把订单置 PAID。
 * 因此本实现 {@code pay()} 仅返回支付参数（awaitPayment），不直接置 PAID，避免伪造支付推进履约。</p>
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
    public PayResult pay(String orderId, int amount, String channel) {
        return pay(orderId, amount, channel, null);
    }

    @Override
    public PayResult pay(String orderId, int amount, String channel, String openid) {
        if (!"WECHAT".equalsIgnoreCase(channel)) {
            return new PayResult(false, null, channel, "暂仅支持微信支付(WECHAT)");
        }
        if (openid == null || openid.isBlank()) {
            return new PayResult(false, null, channel, "缺少用户 openid，无法发起 JSAPI 支付");
        }
        String prepayId = client.jsapiPrepay(orderId, amount, "宽带业务订单-" + orderId, openid, null);
        if (prepayId == null) {
            return new PayResult(false, null, channel, "微信统一下单未返回 prepay_id");
        }
        Map<String, String> params = client.buildPayParams(prepayId);
        try {
            return new PayResult(true, prepayId, channel, om.writeValueAsString(params));
        } catch (Exception e) {
            return new PayResult(false, null, channel, "支付参数序列化失败");
        }
    }

    @Override
    public String refund(String outTradeNo, String outRefundNo, int refundFeeFen, String reason) {
        return client.refund(outTradeNo, outRefundNo, refundFeeFen, reason);
    }
}
