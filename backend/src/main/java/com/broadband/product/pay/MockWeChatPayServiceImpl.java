package com.broadband.product.pay;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 模拟微信支付实现：无真实商户号亦可联调「下单→支付→派单→装机→赔付→评价」完整业务闭环。
 *
 * <p>生产替换：实现 {@link PayService} 并调用微信支付 SDK（统一下单 + 支付结果通知校验），
 * Spring 注入点不变，订单侧无需改动。</p>
 */
@Service
public class MockWeChatPayServiceImpl implements PayService {

    @Override
    public PayResult pay(String orderId, int amount, String channel) {
        // 模拟微信支付：真实接入时此处调微信统一下单 API，并核对异步回调签名后回填支付状态
        String txn = "WX" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
        return new PayResult(true, txn, "MOCK_WECHAT", "支付成功（模拟微信支付）");
    }
}
