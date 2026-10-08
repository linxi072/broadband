package com.broadband.product.pay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 支付网关入口（v1.15 支付真闭环）。
 *
 * <ul>
 *   <li>POST /api/pay/notify —— 支付渠道结果回调（真实微信 / 结构化通知）。
 *       路径已在 SecurityConfig 放行（微信服务器主动 POST，无登录态）。</li>
 *   <li>POST /api/pay/simulate/{outTradeNo} —— 开发态模拟网关：直接置该流水 PAID 并推进订单，
 *       闭环与真实微信一致，便于无商户号联调。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/pay")
public class PaymentController {

    @Autowired private PaymentService paymentService;

    /** 支付结果回调：验签解析后由 PaymentService 确认支付并推进订单。 */
    @PostMapping("/notify")
    public Map<String, Object> notify(@RequestHeader Map<String, String> headers, @RequestBody String body) {
        return paymentService.notify(headers, body);
    }

    /** 开发态模拟网关回调：幂等地置 PAID。 */
    @PostMapping("/simulate/{outTradeNo}")
    public Map<String, Object> simulate(@PathVariable String outTradeNo) {
        return paymentService.simulate(outTradeNo);
    }
}
