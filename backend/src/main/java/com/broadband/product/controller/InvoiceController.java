package com.broadband.product.controller;

import com.broadband.product.service.InvoiceService;
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
 * 电子发票（占位）C 端接口。
 *
 * <ul>
 *   <li>POST /api/invoice/apply —— 申请开票（关联已完成订单，生成 PENDING 申请单）</li>
 *   <li>GET  /api/invoice/my     —— 按客户名查询自己的发票申请与状态</li>
 * </ul>
 *
 * <p>状态机：PENDING（申请） -&gt; OPENED（已开具） / REJECTED（驳回），由后台
 * {@code /api/admin/invoices} 审批流转（见 AdminProductController）。</p>
 *
 * <p>本类只做参数绑定与 HTTP 响应，业务逻辑见 {@link InvoiceService}。</p>
 */
@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    @Autowired private InvoiceService invoiceService;

    /** 申请开票。入参 {orderId, title, taxNo?, customerName?}。 */
    @PostMapping("/apply")
    public Map<String, Object> apply(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal CustomerPrincipal cp) {
        return invoiceService.apply(body, cp == null ? null : cp.id, cp == null ? null : cp.name);
    }

    /** 我的发票申请（按客户名过滤）。 */
    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestParam(required = false) String customerName,
                                        @AuthenticationPrincipal CustomerPrincipal cp) {
        return invoiceService.my(customerName, cp == null ? null : cp.name);
    }
}
