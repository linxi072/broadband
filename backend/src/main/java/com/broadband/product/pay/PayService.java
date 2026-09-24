package com.broadband.product.pay;

import java.util.Map;

/**
 * 支付服务接口：订单与支付解耦，微信支付 / 支付宝 / 模拟支付均可作为实现插入。
 *
 * <p>v1.15 支付真闭环：下单后由 {@code PaymentService} 调用 {@link #createPayment} 发起支付，
 * 返回「前端拉起支付所需参数（payParams）+ 商户单号（outTradeNo）」，此时业务订单仍为 PENDING；
 * 支付网关通过 {@link #verifyAndParse} 回传结果，校验通过后由 PaymentService 将订单置 PAID。
 * 默认由 {@link MockWeChatPayServiceImpl} 模拟（开发态经 /api/pay/simulate 模拟网关回调）；
 * 生产环境配置 app.pay.wechat.enabled=true 后由 {@link WeChatPayServiceImpl} 接管，
 * 订单侧与支付编排代码无需改动。</p>
 */
public interface PayService {

    /** 发起支付。返回前端拉起支付所需参数、商户单号与过期时间（此时订单尚未支付成功）。 */
    PayOrder createPayment(String bizOrderId, int amount, String channel, String clientIp);

    /**
     * 校验并解析异步支付通知。verify=true 时验签（真实微信回调）。
     * 返回：是否通过、商户单号、第三方交易号、支付状态（SUCCESS/FAIL）。
     */
    NotifyResult verifyAndParse(Map<String, String> headers, String body);

    /** 发起退款，返回第三方退款单号与结果。 */
    RefundResult refund(String outTradeNo, int amount, String reason);

    /** 发起支付返回体。 */
    class PayOrder {
        public final String outTradeNo;
        public final String channel;
        public final String payParams;   // JSON：前端拉起支付所需
        public final long expireAt;      // 过期时间（毫秒）
        public final String status;      // CREATED / PAYING

        public PayOrder(String outTradeNo, String channel, String payParams, long expireAt, String status) {
            this.outTradeNo = outTradeNo;
            this.channel = channel;
            this.payParams = payParams;
            this.expireAt = expireAt;
            this.status = status;
        }
    }

    /** 异步通知解析结果。 */
    class NotifyResult {
        public final boolean verified;
        public final String outTradeNo;
        public final String transactionId;
        public final String status;      // SUCCESS / FAIL

        public NotifyResult(boolean verified, String outTradeNo, String transactionId, String status) {
            this.verified = verified;
            this.outTradeNo = outTradeNo;
            this.transactionId = transactionId;
            this.status = status;
        }
    }

    /** 退款结果。 */
    class RefundResult {
        public final boolean success;
        public final String refundNo;
        public final String message;

        public RefundResult(boolean success, String refundNo, String message) {
            this.success = success;
            this.refundNo = refundNo;
            this.message = message;
        }
    }
}
