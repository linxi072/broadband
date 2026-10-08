package com.broadband.product.controller;

import com.broadband.common.Values;
import com.broadband.product.pay.PaymentService;
import com.broadband.product.service.OrderService;
import com.broadband.system.security.CustomerPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * C 端客户订单接口（开放层，供小程序「我的订单 / 下单 / 支付 / 评价」使用）。
 *
 * <p>与 PC 后台的 {@code /api/admin/orders}（需 {@code order:view} 权限）不同，
 * 这里是面向用户的开放接口。业务闭环：</p>
 * <ul>
 *   <li>POST /api/order/create   —— 下单（PENDING）</li>
 *   <li>POST /api/order/pay      —— 支付（PAID，并生成关联安装工单待派单）</li>
 *   <li>GET  /api/order/my       —— 我的订单</li>
 *   <li>GET  /api/order/tracking —— 订单全链路追踪（订单→工单→SLA→评价）</li>
 * </ul>
 *
 * <p>本类只做参数绑定与 HTTP 响应，业务逻辑见 {@link OrderService}。</p>
 */
@RestController
@RequestMapping("/api/order")
public class ClientOrderController {

    @Autowired private OrderService orderService;
    @Autowired private PaymentService paymentService;

    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestParam(required = false) String customerId) {
        return orderService.my(customerId);
    }

    /**
     * 下单：C 端客户创建业务订单（默认待支付 PENDING）。
     * 入参 {customerId, packageId, communityId, orderType?, address?, contactName?, contactPhone?, timeSlot?}。
     * 返回 {ok, orderId, amount, packageName, timeSlot, status}。
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> body,
                                      @AuthenticationPrincipal CustomerPrincipal cp) {
        return orderService.create(body, cp == null ? null : cp.id);
    }

    /**
     * 支付：发起支付（v1.15 支付真闭环）。委托 PaymentService 写入 pay_transaction 并返回拉起参数，
     * 订单置 PAID 由支付网关回调（/api/pay/notify 或 /api/pay/simulate）最终确认。
     */
    @PostMapping("/pay")
    public Map<String, Object> pay(@RequestBody Map<String, Object> body,
                                   @AuthenticationPrincipal CustomerPrincipal cp) {
        String orderId = Values.str(body.get("orderId"));
        String channel = Values.str(body.get("channel"), "WECHAT_MOCK");
        String customerId = Values.str(body.get("customerId"), cp == null ? null : cp.id);
        String clientIp = Values.str(body.get("clientIp"));
        String openid = Values.str(body.get("openid"));
        return paymentService.initiatePayment(orderId, customerId, channel, clientIp, openid);
    }

    /** 支付状态轮询（C 端拉起支付后轮询订单支付结果）。 */
    @GetMapping("/payment-status")
    public Map<String, Object> paymentStatus(@RequestParam String orderId) {
        return paymentService.queryStatus(orderId);
    }

    /** 申请退款（退款 / 对账状态机入口，C 端）。入参 {orderId, reason?}。 */
    @PostMapping("/refund")
    public Map<String, Object> refund(@RequestBody Map<String, Object> body,
                                      @AuthenticationPrincipal CustomerPrincipal cp) {
        return orderService.refund(body, cp == null ? null : cp.id);
    }

    /** 订单全链路追踪（C 端）：业务订单 + 关联工单 + SLA 评估 + 评价。 */
    @GetMapping("/tracking")
    public Map<String, Object> tracking(@RequestParam String orderId) {
        return orderService.tracking(orderId);
    }
}
