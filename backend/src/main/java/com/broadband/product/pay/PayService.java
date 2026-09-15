package com.broadband.product.pay;

/**
 * 支付服务接口：订单与支付解耦，微信支付 / 支付宝 / 模拟支付均可作为实现插入。
 *
 * <p>业务闭环（下单→支付→派单→装机→赔付→评价）中，支付这一环通过本接口完成，
 * 默认由 {@link MockWeChatPayServiceImpl} 模拟，生产环境替换为真实微信支付 SDK 实现即可，
 * 订单侧代码无需改动。</p>
 */
public interface PayService {

    /** 发起一次支付。真实微信支付应在此调用统一下单 API 并校验异步回调签名。 */
    PayResult pay(String orderId, int amount, String channel);

    /** 支付结果载体。 */
    class PayResult {
        public final boolean success;
        public final String transactionId;
        public final String channel;
        public final String message;

        public PayResult(boolean success, String transactionId, String channel, String message) {
            this.success = success;
            this.transactionId = transactionId;
            this.channel = channel;
            this.message = message;
        }
    }
}
