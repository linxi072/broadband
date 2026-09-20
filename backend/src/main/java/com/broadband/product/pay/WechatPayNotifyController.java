package com.broadband.product.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 微信支付结果异步通知（公网回调入口）。
 *
 * <p>微信以 POST 推送支付结果，携带 {@code Wechatpay-Signature} 等头与加密的 {@code resource}。
 * 本控制器：① 用平台证书验签；② 用 APIv3 密钥解密；③ 仅当 {@code trade_state=SUCCESS}
 * 时调用 {@link com.broadband.product.service.OrderService#confirmPaid} 把订单置 PAID 并生成安装工单。</p>
 *
 * <p>该 Bean 始终存在（无 @Conditional），但 {@code WechatPayV3Client} 仅在 {@code wechat.pay.enabled=true}
 * 时装配；未启用时以 {@code Optional} 空值安全返回 FAIL，不影响其他链路。</p>
 */
@RestController
@RequestMapping("/api/pay/wechat")
public class WechatPayNotifyController {

    private final WechatPayV3Client client;
    private final com.broadband.product.service.OrderService orderService;
    private final ObjectMapper om = new ObjectMapper();

    public WechatPayNotifyController(Optional<WechatPayV3Client> client,
                                     com.broadband.product.service.OrderService orderService) {
        this.client = client.orElse(null);
        this.orderService = orderService;
    }

    @PostMapping("/notify")
    public ResponseEntity<String> notify(
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestBody(required = false) String rawBody) {

        // fail-closed（质量加固）：支付未启用，或请求缺少微信回调头 / 请求体（非微信推送 / 探测），
        // 一律返回 FAIL(401)。避免把这类请求变成 500（MissingRequestHeaderException / 缺体）破坏契约。
        if (client == null || signature == null || timestamp == null || nonce == null || serial == null
                || rawBody == null) {
            return ResponseEntity.status(401).body("{\"code\":\"FAIL\"}");
        }
        if (!client.verifyNotify(timestamp, nonce, rawBody, signature, serial)) {
            return ResponseEntity.status(401).body("{\"code\":\"FAIL\"}");
        }
        try {
            JsonNode root = om.readTree(rawBody);
            JsonNode res = root.path("resource");
            String plain = client.decryptResource(
                    res.path("ciphertext").asText(),
                    res.path("nonce").asText(),
                    res.path("associated_data").asText(""));
            JsonNode dec = om.readTree(plain);
            String outTradeNo = dec.path("out_trade_no").asText();
            String transId = dec.path("transaction_id").asText();
            String state = dec.path("trade_state").asText();
            if ("SUCCESS".equals(state)) {
                orderService.confirmPaid(outTradeNo, transId);
            }
            return ResponseEntity.ok("{\"code\":\"SUCCESS\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"code\":\"FAIL\"}");
        }
    }
}
