package com.broadband.product.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信支付 APIv3 客户端（纯 JDK 实现，无第三方 SDK 依赖）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>构造 {@code Authorization: WECHATPAY2-SHA256-RSA2048 ...} 请求头（商户私钥 RSA 签名）；</li>
 *   <li>调用 JSAPI 统一下单 / 查询 / 关单 / 退款；</li>
 *   <li>用 APIv3 密钥（AES-256-GCM）解密回调/证书资源；</li>
 *   <li>校验微信回调签名（平台证书公钥）。</li>
 * </ul>
 *
 * <p>敏感内容（私钥、APIv3 密钥）仅存在于运行时内存，不落日志。</p>
 */
public class WechatPayV3Client {

    private static final String SCHEMA = "WECHATPAY2-SHA256-RSA2048 ";
    private static final String BASE = "https://api.mch.weixin.qq.com";

    private final WechatPayProperties props;
    private final PrivateKey merchantPrivateKey;
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final Map<String, X509Certificate> certCache = new ConcurrentHashMap<>();

    public WechatPayV3Client(WechatPayProperties props) {
        this.props = props;
        this.merchantPrivateKey = loadPrivateKey(props.getPrivateKey());
    }

    /* ===================== 对外 API ===================== */

    /** JSAPI 统一下单，返回 prepay_id（供小程序 wx.requestPayment 组装支付参数）。 */
    public String jsapiPrepay(String outTradeNo, int totalFeeFen, String description,
                              String openid, String timeExpire) {
        Map<String, Object> amount = new HashMap<>();
        amount.put("total", totalFeeFen);
        amount.put("currency", "CNY");

        Map<String, Object> payer = new HashMap<>();
        payer.put("openid", openid);

        Map<String, Object> body = new HashMap<>();
        body.put("appid", props.getAppId());
        body.put("mchid", props.getMchId());
        body.put("description", description);
        body.put("out_trade_no", outTradeNo);
        body.put("notify_url", props.getNotifyUrl());
        body.put("amount", amount);
        body.put("payer", payer);
        if (timeExpire != null) body.put("time_expire", timeExpire);

        JsonNode resp = postJson("/v3/pay/transactions/jsapi", body);
        return resp.path("prepay_id").asText(null);
    }

    /** 组装小程序调起支付所需的参数（wx.requestPayment 入参）。 */
    public Map<String, String> buildPayParams(String prepayId) {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = randomNonce();
        String pkg = "prepay_id=" + prepayId;
        String message = props.getAppId() + "\n" + timestamp + "\n" + nonce + "\n" + pkg + "\n";
        String paySign = sign(message);
        Map<String, String> p = new HashMap<>();
        p.put("appId", props.getAppId());
        p.put("timeStamp", String.valueOf(timestamp));
        p.put("nonceStr", nonce);
        p.put("package", pkg);
        p.put("signType", "RSA");
        p.put("paySign", paySign);
        return p;
    }

    /** 申请退款，返回微信退款单号 out_refund_no（真实退款单号，对接 R4）。 */
    public String refund(String outTradeNo, String outRefundNo, int refundFeeFen, String reason) {
        Map<String, Object> amount = new HashMap<>();
        amount.put("refund", refundFeeFen);
        amount.put("total", refundFeeFen); // 由调用方保证 total>=refund；此处简化
        amount.put("currency", "CNY");

        Map<String, Object> body = new HashMap<>();
        body.put("out_trade_no", outTradeNo);
        body.put("out_refund_no", outRefundNo);
        body.put("reason", reason);
        body.put("amount", amount);

        JsonNode resp = postJson("/v3/refund/domestic/refunds", body);
        return resp.path("refund_id").asText(null);
    }

    /** 关闭订单（支付超时未支付场景）。 */
    public void closeOrder(String outTradeNo) {
        Map<String, Object> body = new HashMap<>();
        body.put("mchid", props.getMchId());
        postJson("/v3/pay/transactions/out-trade-no/" + outTradeNo + "/close", body);
    }

    /* ===================== 回调解密 / 验签 ===================== */

    /**
     * 校验微信回调签名。
     * @param timestamp 头 Wechatpay-Timestamp
     * @param nonce     头 Wechatpay-Nonce
     * @param rawBody   原始请求体（与微信签名所用一致）
     * @param signature 头 Wechatpay-Signature（base64）
     * @param serial    头 Wechatpay-Serial（平台证书序列号）
     */
    public boolean verifyNotify(String timestamp, String nonce, String rawBody,
                               String signature, String serial) {
        X509Certificate cert = getPlatformCert(serial);
        if (cert == null) return false;
        try {
            String message = timestamp + "\n" + nonce + "\n" + rawBody + "\n";
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(cert.getPublicKey());
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            return false;
        }
    }

    /** 解密回调中的 resource（AES-256-GCM，APIv3 密钥）。 */
    public String decryptResource(String ciphertext, String nonce, String associatedData) {
        try {
            byte[] key = props.getApiV3Key().getBytes(StandardCharsets.UTF_8);
            byte[] nonceB = nonce.getBytes(StandardCharsets.UTF_8);
            byte[] cipherB = Base64.getDecoder().decode(ciphertext);
            byte[] aad = (associatedData == null ? "" : associatedData).getBytes(StandardCharsets.UTF_8);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonceB));
            c.updateAAD(aad);
            return new String(c.doFinal(cipherB), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("微信资源解密失败", e);
        }
    }

    /* ===================== 内部：HTTP / 签名 / 证书 ===================== */

    private JsonNode postJson(String path, Map<String, Object> body) {
        try {
            String bodyStr = om.writeValueAsString(body);
            String ts = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = randomNonce();
            String auth = buildAuthHeader("POST", path, ts, nonce, bodyStr);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + path))
                    .header("Authorization", auth)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "broadband-backend/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyStr, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("微信支付 API 错误 " + resp.statusCode() + ": " + resp.body());
            }
            return om.readTree(resp.body());
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("调用微信支付失败", e); }
    }

    private String buildAuthHeader(String method, String urlPath, String timestamp, String nonce, String body) {
        String message = method + "\n" + urlPath + "\n" + timestamp + "\n" + nonce + "\n"
                + (body == null ? "" : body) + "\n";
        String signature = sign(message);
        return SCHEMA + "mchid=\"" + props.getMchId() + "\","
                + "nonce_str=\"" + nonce + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + props.getSerialNo() + "\"";
    }

    private String sign(String message) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(merchantPrivateKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) { throw new IllegalStateException("商户签名失败", e); }
    }

    private X509Certificate getPlatformCert(String serial) {
        X509Certificate cached = certCache.get(serial);
        if (cached != null) return cached;
        // 首次：拉取平台证书列表（响应本身用 APIv3 密钥加密）
        try {
            String ts = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = randomNonce();
            String auth = buildAuthHeader("GET", "/v3/certificates", ts, nonce, "");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + "/v3/certificates"))
                    .header("Authorization", auth)
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = om.readTree(resp.body());
            for (JsonNode item : root.path("data")) {
                String itemSerial = item.path("serial_no").asText();
                JsonNode enc = item.path("encrypt_certificate");
                String plain = decryptResource(enc.path("ciphertext").asText(),
                        enc.path("nonce").asText(), enc.path("associated_data").asText(""));
                X509Certificate cert = loadCert(plain);
                certCache.put(itemSerial, cert);
            }
            return certCache.get(serial);
        } catch (Exception e) {
            throw new IllegalStateException("获取微信平台证书失败", e);
        }
    }

    private static PrivateKey loadPrivateKey(String pem) {
        String k = pem.replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        try {
            byte[] bytes = Base64.getDecoder().decode(k);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception e) { throw new IllegalStateException("商户私钥解析失败", e); }
    }

    private static X509Certificate loadCert(String pem) {
        String c = pem.replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        try {
            byte[] bytes = Base64.getDecoder().decode(c);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
        } catch (Exception e) { throw new IllegalStateException("平台证书解析失败", e); }
    }

    private static String randomNonce() {
        byte[] b = new byte[16];
        new java.security.SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }
}
