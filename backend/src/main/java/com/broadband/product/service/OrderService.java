package com.broadband.product.service;

import com.broadband.common.Values;
import com.broadband.product.mapper.OrderMapper;
import com.broadband.product.pay.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * C 端客户订单业务：下单 / 支付 / 我的订单 / 退款申请 / 全链路追踪。数据访问经 {@link OrderMapper}。
 *
 * <p>与 PC 后台 {@code /api/admin/orders}（需 order:view 权限）不同，这里是面向用户的开放接口。</p>
 *
 * <p>支付能力通过 {@link PayService} 以可选 Bean 注入：未接入真实支付渠道时 Bean 不存在，
 * 支付直接拒绝（fail-closed），而不是伪造支付成功把未收款订单推进到已支付并派单。</p>
 */
@Service
public class OrderService {

    @Autowired private OrderMapper orderMapper;
    @Autowired private JdbcTemplate jdbc;

    /**
     * 支付服务（可选 Bean）。
     *
     * <p>required=false：本工程不再内置任何支付实现（原 MockWeChatPayServiceImpl 已作为测试数据移除）。</p>
     */
    @Autowired(required = false) private PayService payService;

    /** 我的订单。 */
    public List<Map<String, Object>> my(String customerId) {
        return orderMapper.selectMy(customerId == null ? "" : customerId);
    }

    /**
     * 下单：C 端客户创建业务订单（默认待支付 PENDING）。
     * @param fallbackCustomerId 登录客户 id（请求体未带 customerId 时使用）
     */
    public Map<String, Object> create(Map<String, Object> body, String fallbackCustomerId) {
        String customerId = Values.str(body.get("customerId"), fallbackCustomerId);
        String packageId = Values.str(body.get("packageId"));
        String communityId = Values.str(body.get("communityId"));
        String orderType = Values.str(body.get("orderType"), "NEW_INSTALL");
        String timeSlot = Values.str(body.get("timeSlot"));
        String contactName = Values.str(body.get("contactName"));
        String contactPhone = Values.str(body.get("contactPhone"));

        if (Values.isBlank(customerId) || Values.isBlank(packageId) || Values.isBlank(communityId)) {
            throw new IllegalArgumentException("customerId / packageId / communityId 必填");
        }
        Map<String, Object> pkg = orderMapper.selectPackage(packageId);
        if (pkg == null) throw new IllegalArgumentException("套餐不存在：" + packageId);
        String pkgName = String.valueOf(pkg.get("name"));
        int amount = Values.intOf(pkg.get("monthly_fee"));

        Map<String, Object> com = orderMapper.selectCommunity(communityId);
        String communityName = com == null ? "" : String.valueOf(com.get("name"));
        String deptId = com == null ? null : Values.str(com.get("dept_id"));

        String customerName = contactName;
        String phone = contactPhone;
        if (Values.isBlank(customerName) || Values.isBlank(phone)) {
            Map<String, Object> cust = orderMapper.selectCustomer(customerId);
            if (cust != null) {
                if (Values.isBlank(customerName)) customerName = Values.str(cust.get("name"), "客户");
                if (Values.isBlank(phone)) phone = Values.str(cust.get("phone"));
            }
        }
        if (Values.isBlank(customerName)) customerName = "客户";

        if (Values.isBlank(timeSlot)) {
            timeSlot = LocalDate.now().plusDays(1).toString() + "#AM";
        }

        String orderId = "B" + System.currentTimeMillis();
        long now = System.currentTimeMillis();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", orderId);
        m.put("customerId", customerId);
        m.put("customerName", customerName);
        m.put("phone", phone);
        m.put("packageId", packageId);
        m.put("packageName", pkgName);
        m.put("amount", amount);
        m.put("communityId", communityId);
        m.put("communityName", communityName);
        m.put("orderType", orderType);
        m.put("status", "PENDING");
        m.put("deptId", deptId);
        m.put("createdTime", now);
        orderMapper.insertOrder(m);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("orderId", orderId);
        resp.put("amount", amount);
        resp.put("packageName", pkgName);
        resp.put("timeSlot", timeSlot);
        resp.put("status", "PENDING");
        return resp;
    }

    // 支付入口已迁移至 PaymentService（v1.15 支付真闭环编排器）。
    // ClientOrderController.pay() 现调用 paymentService.initiatePayment(...)，由支付状态机统一驱动订单置 PAID。

    /**
     * 支付回调确认：把订单置 PAID 并生成关联安装工单（幂等）。
     * 仅由 {@code WechatPayNotifyController} 在微信异步通知校验通过后调用。
     */
    public Map<String, Object> confirmPaid(String orderId, String transactionId) {
        Map<String, Object> order = orderMapper.selectOrderStatus(orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在：" + orderId);
        if ("PAID".equals(order.get("status"))) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok", true);
            r.put("alreadyPaid", true);
            r.put("orderId", orderId);
            r.put("workOrderId", orderMapper.selectWorkOrderId(orderId));
            r.put("status", "PAID");
            return r;
        }
        return finalizePaid(orderId, order, transactionId);
    }

    private Map<String, Object> finalizePaid(String orderId, Map<String, Object> order, String transactionId) {
        orderMapper.updatePaid(orderId);
        String workOrderId = "WO" + System.currentTimeMillis();
        String timeSlot = LocalDate.now().plusDays(1).toString() + "#AM";
        Map<String, Object> wo = new LinkedHashMap<>();
        wo.put("id", workOrderId);
        wo.put("communityId", order.get("community_id"));
        wo.put("address", order.get("community_name"));
        wo.put("timeSlot", timeSlot);
        wo.put("customerName", order.get("customer_name"));
        wo.put("packageName", order.get("package_name"));
        wo.put("status", "PENDING");
        wo.put("bizOrderId", orderId);
        orderMapper.insertWorkOrder(wo);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("orderId", orderId);
        resp.put("workOrderId", workOrderId);
        resp.put("transactionId", transactionId);
        resp.put("status", "PAID");
        return resp;
    }

    /**
     * 申请退款（退款 / 对账状态机入口，C 端）。
     * 仅「已支付 / 安装中 / 已完成」订单可发起；幂等：已存在 PENDING/REFUNDED 退款单则直接返回。
     */
    public Map<String, Object> refund(Map<String, Object> body, String fallbackCustomerId) {
        String orderId = Values.str(body.get("orderId"));
        String reason = Values.str(body.get("reason"), "客户申请退款");
        if (Values.isBlank(orderId)) throw new IllegalArgumentException("orderId 必填");

        Map<String, Object> order = orderMapper.selectOrderForRefund(orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在：" + orderId);

        String status = String.valueOf(order.get("status"));
        if (!List.of("PAID", "INSTALLING", "DONE").contains(status)) {
            throw new IllegalStateException("仅「已支付/安装中/已完成」订单可申请退款，当前：" + status);
        }
        String existing = orderMapper.selectExistingRefund(orderId);
        if (existing != null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok", true);
            r.put("alreadyRequested", true);
            r.put("refundId", existing);
            r.put("orderId", orderId);
            return r;
        }

        String refundId = "RF" + System.currentTimeMillis();
        int amount = Values.intOf(order.get("amount"));
        String cid = Values.str(order.get("customer_id"), fallbackCustomerId);
        String cname = Values.str(order.get("customer_name"), "客户");
        // channel：真实支付渠道接入后回填该订单实际支付所用渠道；接入前记为 UNKNOWN，避免写入伪造渠道。
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", refundId);
        m.put("orderId", orderId);
        m.put("orderNo", orderId);
        m.put("customerId", cid);
        m.put("customerName", cname);
        m.put("amount", amount);
        m.put("reason", reason);
        // 真实支付渠道接入后回填实际渠道；接入前记为 UNKNOWN，避免写入伪造渠道。
        m.put("channel", payService != null ? "WECHAT" : "UNKNOWN");
        m.put("status", "PENDING");
        m.put("createdTime", System.currentTimeMillis());
        orderMapper.insertRefund(m);

        // 已接入真实支付渠道时，同步向微信发起退款（金额单位 分 = amount*100）
        if (payService != null) {
            try {
                String outTradeNo = jdbc.queryForObject(
                        "select out_trade_no from pay_transaction where biz_order_id=? order by created_time desc limit 1",
                        String.class, orderId);
                if (outTradeNo != null) {
                    payService.refund(outTradeNo, amount * 100, reason);
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(OrderService.class)
                        .warn("微信退款调用失败 orderId={} refundId={}，本地已落单，待对账/重试", orderId, refundId, e);
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("refundId", refundId);
        resp.put("orderId", orderId);
        resp.put("amount", amount);
        resp.put("status", "PENDING");
        return resp;
    }

    /** 订单全链路追踪（C 端）：业务订单 + 关联工单 + SLA 评估 + 评价。 */
    public Map<String, Object> tracking(String orderId) {
        Map<String, Object> order = orderMapper.selectOrderStatus(orderId);
        if (order == null) return Map.of();
        Map<String, Object> wo = orderMapper.selectWorkOrderByBiz(orderId);
        String woId = wo == null ? null : String.valueOf(wo.get("id"));
        List<Map<String, Object>> sla = woId == null ? List.of() : orderMapper.selectSla(woId);
        List<Map<String, Object>> reviews = orderMapper.selectReviews(orderId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("order", order);
        resp.put("workOrder", wo == null ? Map.of() : wo);
        resp.put("slaRecords", sla);
        resp.put("reviews", reviews);
        return resp;
    }
}
