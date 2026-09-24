package com.broadband.product.pay;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 模拟微信支付实现：无真实商户号亦可联调「下单→发起支付→网关回调→订单置 PAID」完整闭环。
 *
 * <p>createPayment 仅生成商户单号与本地模拟拉起参数（含 /api/pay/simulate/{outTradeNo} 回调地址，
 * 由 PaymentController.simulate 模拟微信网关向 notify 发起回调），不立即置付；支付结果以异步回调为准，
 * 与真实微信支付行为一致。生产替换：将 {@code app.pay.wechat.enabled} 置 true 即由
 * WeChatPayServiceImpl 接管（本类因条件化装配而不再生效）。</p>
 */
@Service
@ConditionalOnProperty(prefix = "app.pay.wechat", name = "enabled", havingValue = "false", matchIfMissing = true)
public class MockWeChatPayServiceImpl implements PayService {

    @Override
    public PayOrder createPayment(String bizOrderId, int amount, String channel, String clientIp) {
        String outTradeNo = "OUT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
        // 模拟拉起参数：开发态前端凭 simulateUrl 触发「网关回调」，闭环与真实微信一致
        String payParams = "{\"channel\":\"WECHAT_MOCK\",\"simulateUrl\":\"/api/pay/simulate/"
                + outTradeNo + "\",\"qrContent\":\"mock-qr-" + outTradeNo + "\"}";
        long expireAt = System.currentTimeMillis() + 30L * 60 * 1000;
        return new PayOrder(outTradeNo, "WECHAT_MOCK", payParams, expireAt, "PAYING");
    }

    @Override
    public NotifyResult verifyAndParse(Map<String, String> headers, String body) {
        // 模拟实现：body 为 JSON，含 outTradeNo / transactionId / tradeState
        String outTradeNo = extract(body, "outTradeNo");
        String transactionId = extract(body, "transactionId");
        String state = extract(body, "tradeState");
        boolean ok = "SUCCESS".equalsIgnoreCase(state) && outTradeNo != null && !outTradeNo.isEmpty();
        return new NotifyResult(ok, outTradeNo, transactionId, ok ? "SUCCESS" : "FAIL");
    }

    @Override
    public RefundResult refund(String outTradeNo, int amount, String reason) {
        String refundNo = "RFX" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
        return new RefundResult(true, refundNo, "退款已受理（模拟微信支付）");
    }

    /** 从扁平 JSON 中提取字符串字段值（足够应对模拟报文，真实报文走验签解析）。 */
    private static String extract(String body, String key) {
        if (body == null) return null;
        int i = body.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int c = body.indexOf(':', i);
        if (c < 0) return null;
        int s = body.indexOf('"', c);
        if (s < 0) return null;
        int e = body.indexOf('"', s + 1);
        if (e < 0) return null;
        return body.substring(s + 1, e);
    }
}
