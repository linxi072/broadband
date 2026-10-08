package com.broadband.product.pay;

import com.broadband.common.Ids;
import com.broadband.product.service.OrderService;
import com.broadband.system.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付编排器（v1.15 支付真闭环）：解耦「业务订单」与「支付渠道」。
 *
 * <p>状态机：pay_transaction 行 CREATED → PAYING（发起支付）→ PAID（网关回调确认）→ REFUNDED。
 * 关键不变量：仅在网关确认（/api/pay/notify 真实验签，或 /api/pay/simulate 开发态模拟）后，
 * 才把业务订单置 PAID，杜绝「伪造支付成功」直接推进履约。幂等：同一 out_trade_no 重复回调直接返回已确认结果。</p>
 */
@Service
public class PaymentService {

    @Autowired private JdbcTemplate jdbc;
    @Autowired(required = false) private PayService payService;
    @Autowired private OrderService orderService;
    @Autowired(required = false) private NotificationService notifications;

    /** 发起支付：写入支付流水（PAYING）并调用支付渠道拿到拉起参数；业务订单此时仍为 PENDING。 */
    public Map<String, Object> initiatePayment(String bizOrderId, String customerId, String channel, String clientIp, String openid) {
        if (bizOrderId == null || bizOrderId.isBlank()) throw new IllegalArgumentException("orderId 必填");
        Map<String, Object> order;
        try {
            order = jdbc.queryForMap(
                    "select id, customer_id, customer_name, package_name, amount, status from biz_order where id=?", bizOrderId);
        } catch (Exception e) {
            throw new IllegalArgumentException("订单不存在：" + bizOrderId);
        }
        String status = String.valueOf(order.get("status"));
        if ("PAID".equals(status) || "INSTALLING".equals(status) || "DONE".equals(status)) {
            Map<String, Object> existing = findLatest(bizOrderId);
            if (existing != null) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("orderId", bizOrderId);
                r.put("alreadyPaid", true);
                r.put("status", "PAID");
                r.put("outTradeNo", existing.get("out_trade_no"));
                r.put("payParams", existing.get("pay_params"));
                r.put("expireAt", existing.get("expire_time"));
                return r;
            }
        }
        if (payService == null) {
            throw new IllegalStateException("未接入支付服务（缺少 PayService 实现），无法完成支付。请接入真实支付渠道后重试");
        }
        int amountFen = toInt(order.get("amount")) * 100;
        PayService.PayOrder po = payService.createPayment(bizOrderId, amountFen, channel, clientIp, openid);

        String txnId = Ids.next();
        jdbc.update(
                "insert into pay_transaction(id, out_trade_no, biz_order_id, customer_id, channel, amount, status, pay_params, expire_time, created_time, updated_time) "
                        + "values(?,?,?,?,?,?,?,?,?,?,?)",
                txnId, po.outTradeNo, bizOrderId,
                order.get("customer_id"), channel, toInt(order.get("amount")),
                po.status, po.payParams, po.expireAt, System.currentTimeMillis(), System.currentTimeMillis());

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("orderId", bizOrderId);
        r.put("status", po.status);
        r.put("outTradeNo", po.outTradeNo);
        r.put("payParams", po.payParams);
        r.put("expireAt", po.expireAt);
        r.put("channel", po.channel);
        return r;
    }

    /** 开发态模拟网关：直接置该流水 PAID 并推进订单（幂等）。 */
    public Map<String, Object> simulate(String outTradeNo) {
        Map<String, Object> txn = findTxn(outTradeNo);
        if (txn == null) throw new IllegalArgumentException("支付流水不存在：" + outTradeNo);
        if ("PAID".equals(String.valueOf(txn.get("status")))) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok", true);
            r.put("alreadyPaid", true);
            r.put("status", "PAID");
            r.put("outTradeNo", outTradeNo);
            return r;
        }
        String txnId = "MOCKTXN" + outTradeNo;
        long now = System.currentTimeMillis();
        jdbc.update("update pay_transaction set status='PAID', transaction_id=?, paid_time=?, updated_time=?, notify_raw='{\"simulated\":true}' where out_trade_no=?",
                txnId, now, now, outTradeNo);
        String bizOrderId = String.valueOf(txn.get("biz_order_id"));
        orderService.confirmPaid(bizOrderId, txnId);
        notifyPaymentSuccess(bizOrderId, txnId, txn.get("amount"));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("status", "PAID");
        r.put("outTradeNo", outTradeNo);
        r.put("bizOrderId", bizOrderId);
        return r;
    }

    /** 支付网关回调（真实微信 / 结构化通知）：验签解析后确认支付。 */
    public Map<String, Object> notify(Map<String, String> headers, String body) {
        if (payService == null) return Map.of("code", "FAIL");
        PayService.NotifyResult nr = payService.verifyAndParse(headers, body);
        if (!nr.verified || !"SUCCESS".equals(nr.status)) return Map.of("code", "FAIL");
        finalize(nr.outTradeNo, nr.transactionId, body);
        return Map.of("code", "SUCCESS");
    }

    /** 查询某业务订单的支付状态（供 C 端轮询）。 */
    public Map<String, Object> queryStatus(String bizOrderId) {
        Map<String, Object> txn = findLatest(bizOrderId);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("orderId", bizOrderId);
        if (txn == null) {
            r.put("status", "NONE");
            return r;
        }
        r.put("outTradeNo", txn.get("out_trade_no"));
        r.put("status", txn.get("status"));
        r.put("transactionId", txn.get("transaction_id"));
        r.put("paidTime", txn.get("paid_time"));
        return r;
    }

    /** 退款：仅「已支付」流水可发起；同步调用渠道退款并回写 pay_transaction 状态。 */
    public Map<String, Object> refund(String outTradeNo, String reason) {
        Map<String, Object> txn = findTxn(outTradeNo);
        if (txn == null) throw new IllegalArgumentException("支付流水不存在：" + outTradeNo);
        String status = String.valueOf(txn.get("status"));
        if (!"PAID".equals(status)) throw new IllegalStateException("仅「已支付」流水可退款，当前：" + status);
        int amountFen = toInt(txn.get("amount")) * 100;
        if (payService != null) {
            try {
                payService.refund(outTradeNo, amountFen, reason);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(PaymentService.class)
                        .warn("微信退款调用失败 outTradeNo={}，本地已落单，待对账/重试", outTradeNo, e);
            }
        }
        jdbc.update("update pay_transaction set status='REFUNDED', updated_time=? where out_trade_no=?",
                System.currentTimeMillis(), outTradeNo);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("outTradeNo", outTradeNo);
        r.put("status", "REFUNDED");
        return r;
    }

    // ============================================================ 内部

    private void finalize(String outTradeNo, String transactionId, String rawBody) {
        Map<String, Object> txn = findTxn(outTradeNo);
        if (txn == null) return;
        if ("PAID".equals(String.valueOf(txn.get("status")))) return; // 幂等
        long now = System.currentTimeMillis();
        jdbc.update("update pay_transaction set status='PAID', transaction_id=?, paid_time=?, updated_time=?, notify_raw=? where out_trade_no=?",
                transactionId, now, now, rawBody == null ? "" : rawBody, outTradeNo);
        String bizOrderId = String.valueOf(txn.get("biz_order_id"));
        orderService.confirmPaid(bizOrderId, transactionId);
        notifyPaymentSuccess(bizOrderId, transactionId, txn.get("amount"));
    }

    /** 支付成功通知（事件源扩展 · v1.23 后续增量）：旁路容错，通知子系统异常不影响支付主流程。 */
    private void notifyPaymentSuccess(String bizOrderId, String txnId, Object amount) {
        if (notifications == null) return;
        try {
            int yuan = toInt(amount);
            String content = "订单 " + bizOrderId + " 已支付成功，金额 " + yuan + " 元，交易号 " + txnId;
            notifications.notify("PAYMENT_SUCCESS", "支付成功", content, "biz_order", bizOrderId, "ALL");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PaymentService.class)
                    .warn("支付成功通知发送失败 bizOrderId={}", bizOrderId, e);
        }
    }

    private Map<String, Object> findLatest(String bizOrderId) {
        try {
            return jdbc.queryForMap(
                    "select out_trade_no, pay_params, expire_time, status, transaction_id from pay_transaction where biz_order_id=? order by created_time desc limit 1",
                    bizOrderId);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> findTxn(String outTradeNo) {
        try {
            return jdbc.queryForMap(
                    "select biz_order_id, status, amount, transaction_id from pay_transaction where out_trade_no=?", outTradeNo);
        } catch (Exception e) {
            return null;
        }
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return 0;
        }
    }
}
