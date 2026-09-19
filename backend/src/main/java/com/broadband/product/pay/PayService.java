package com.broadband.product.pay;


/**
 * 支付服务接口：订单与支付解耦，微信支付 / 支付宝等均可作为实现插入。
 *
 * <p>业务闭环（下单→支付→派单→装机→赔付→评价）中，支付这一环通过本接口完成。</p>
 *
 * <p><b>当前状态：本工程不提供任何实现（含模拟实现）。</b>此前的
 * {@code MockWeChatPayServiceImpl} 属测试/演示代码（永远返回成功、伪造交易号），
 * 已移除——让「支付」看起来成功会把未真实收款的订单推进到已支付并派单，
 * 是资金与履约上的双重隐患。</p>
 *
 * <p><b>接入方式：</b>接入真实支付渠道时，实现本接口并注册为 Spring Bean 即可
 * （如 {@code @Service public class WechatPayServiceImpl implements PayService}），
 * 调用方 {@code ClientOrderController} 无需改动：它按「是否存在 PayService Bean」
 * 判断支付能力，缺失时直接拒绝支付并给出明确提示，绝不放行。</p>
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
