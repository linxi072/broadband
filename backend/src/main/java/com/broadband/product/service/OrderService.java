package com.broadband.product.service;

import com.broadband.common.Values;
import com.broadband.product.mapper.OrderMapper;
import com.broadband.product.pay.PayService;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * 支付：调用真实支付渠道（{@link PayService}）完成扣款。
     * 支付成功 -&gt; 业务订单置 PAID，并创建关联安装工单（PENDING）作为派单数据源。
     * 幂等：已支付订单再次支付直接返回已关联工单。
     *
     * @throws IllegalStateException 未接入支付服务 / 支付渠道返回失败
     */
    public Map<String, Object> pay(Map<String, Object> body) {
        if (payService == null) {
            throw new IllegalStateException(
                    "未接入支付服务（缺少 PayService 实现），无法完成支付。请接入真实支付渠道后重试");
        }
        String orderId = Values.str(body.get("orderId"));
        String channel = Values.str(body.get("channel"), "WECHAT");
        if (Values.isBlank(orderId)) throw new IllegalArgumentException("orderId 必填");

        Map<String, Object> order = orderMapper.selectOrderStatus(orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在：" + orderId);

        if (!"PENDING".equals(order.get("status"))) {
            String existingWo = orderMapper.selectWorkOrderId(orderId);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok", true);
            r.put("alreadyPaid", true);
            r.put("orderId", orderId);
            r.put("workOrderId", existingWo);
            r.put("status", order.get("status"));
            return r;
        }

        int amount = orderMapper.selectAmount(orderId);
        PayService.PayResult pr = payService.pay(orderId, amount, channel);
        if (!pr.success) throw new IllegalStateException("支付失败：" + pr.message);

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
        resp.put("transactionId", pr.transactionId);
        resp.put("channel", pr.channel);
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
        m.put("channel", "UNKNOWN");
        m.put("status", "PENDING");
        m.put("createdTime", System.currentTimeMillis());
        orderMapper.insertRefund(m);

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
