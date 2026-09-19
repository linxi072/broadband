package com.broadband.product.service;

import com.broadband.common.Values;
import com.broadband.product.mapper.InvoiceMapper;
import com.broadband.system.service.CurrentUser;
import com.broadband.system.service.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电子发票：C 端申请开票 + PC 后台审批流转（占位实现，PDF 地址为占位路径）。
 *
 * <p>状态机：PENDING（申请） -&gt; OPENED（已开具） / REJECTED（驳回），
 * 前后两端共用同一张 invoice_apply 表，故合并为一个 Service；数据访问经 {@link InvoiceMapper}。</p>
 */
@Service
public class InvoiceService {

    @Autowired private InvoiceMapper invoiceMapper;
    @Autowired private OperLogService operLog;

    // ==================================================================== C 端

    /**
     * 申请开票。仅「已完成 DONE」订单可开票；幂等：同订单已有 PENDING/OPENED 申请则直接返回。
     * @param fallbackCustomerId   登录客户 id（请求体未带 customerId 时使用）
     * @param fallbackCustomerName 登录客户名（订单无客户名时使用）
     */
    public Map<String, Object> apply(Map<String, Object> body,
                                     String fallbackCustomerId, String fallbackCustomerName) {
        String orderId = Values.str(body.get("orderId"));
        String title = Values.str(body.get("title"));
        String taxNo = Values.str(body.get("taxNo"));
        if (Values.isBlank(orderId)) throw new IllegalArgumentException("orderId 必填");
        if (Values.isBlank(title)) throw new IllegalArgumentException("发票抬头必填");

        Map<String, Object> order = invoiceMapper.selectOrder(orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在：" + orderId);
        if (!"DONE".equals(order.get("status")))
            throw new IllegalStateException("仅「已完成」订单可开票，当前：" + order.get("status"));

        String existing = invoiceMapper.selectExistingId(orderId);
        if (existing != null) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok", true);
            r.put("alreadyApplied", true);
            r.put("invoiceId", existing);
            r.put("orderId", orderId);
            return r;
        }

        String invoiceId = "IN" + System.currentTimeMillis();
        int amount = Values.intOf(order.get("amount"));
        String cname = Values.str(order.get("customer_name"),
                fallbackCustomerName == null ? "客户" : fallbackCustomerName);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", invoiceId);
        m.put("orderId", orderId);
        m.put("orderNo", orderId);
        m.put("customerId", Values.str(order.get("customer_id"), fallbackCustomerId));
        m.put("customerName", cname);
        m.put("title", title);
        m.put("taxNo", taxNo);
        m.put("amount", amount);
        m.put("status", "PENDING");
        m.put("createdTime", System.currentTimeMillis());
        invoiceMapper.insertApply(m);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("invoiceId", invoiceId);
        resp.put("orderId", orderId);
        resp.put("amount", amount);
        resp.put("status", "PENDING");
        return resp;
    }

    /** 我的发票申请（按客户名过滤）。 */
    public List<Map<String, Object>> my(String customerName, String fallbackCustomerName) {
        String name = Values.str(customerName, fallbackCustomerName);
        if (Values.isBlank(name)) return List.of();
        return invoiceMapper.selectMy(name);
    }

    // ==================================================================== PC 后台

    /** 发票申请列表（复用 finance:view），可选按状态过滤。 */
    public List<Map<String, Object>> list(String status) {
        return invoiceMapper.selectList(status);
    }

    /** 开具发票：PENDING -> OPENED，生成发票号与占位 PDF 地址。 */
    public Map<String, Object> open(String id, Map<String, Object> body) {
        Map<String, Object> inv = invoiceMapper.selectInvoice(id);
        if (inv == null) throw new IllegalArgumentException("发票申请不存在：" + id);
        if (!"PENDING".equals(inv.get("status")))
            throw new IllegalStateException("仅 PENDING 发票可申请开具，当前：" + inv.get("status"));
        String invoiceNo = "INV" + System.currentTimeMillis();
        String operator = Values.str(body == null ? null : body.get("operator"), "财务");
        String pdfUrl = "/assets/invoice/" + invoiceNo + ".pdf";
        invoiceMapper.updateOpen(id, invoiceNo, pdfUrl, operator, System.currentTimeMillis());
        log("开具发票", id, "POST");
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("invoiceNo", invoiceNo);
        resp.put("pdfUrl", pdfUrl);
        resp.put("status", "OPENED");
        return resp;
    }

    /** 驳回发票：PENDING -> REJECTED，记录驳回原因。 */
    public Map<String, Object> reject(String id, Map<String, Object> body) {
        Map<String, Object> inv = invoiceMapper.selectInvoice(id);
        if (inv == null) throw new IllegalArgumentException("发票申请不存在：" + id);
        if (!"PENDING".equals(inv.get("status")))
            throw new IllegalStateException("仅 PENDING 发票可驳回，当前：" + inv.get("status"));
        String remark = Values.str(body == null ? null : body.get("remark"), "信息不全");
        String operator = Values.str(body == null ? null : body.get("operator"), "财务");
        invoiceMapper.updateReject(id, remark, operator, System.currentTimeMillis());
        log("驳回发票", id, "POST");
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("status", "REJECTED");
        return resp;
    }

    // ==================================================================== 工具

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
