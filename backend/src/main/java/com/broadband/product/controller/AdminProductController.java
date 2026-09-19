package com.broadband.product.controller;

import com.broadband.product.service.CustomerAdminService;
import com.broadband.product.service.InvoiceService;
import com.broadband.product.service.MarketingAdminService;
import com.broadband.product.service.PackageAdminService;
import com.broadband.product.service.RefundAdminService;
import com.broadband.product.service.ReportAdminService;
import com.broadband.product.service.ReviewAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 客户 / 套餐 / 升级单 / 投诉评价 / 销售 / 财务 的后台读侧接口（PC 后台）。
 *
 * <p>本类只做权限校验、参数绑定与 HTTP 响应；查询与写逻辑按域分散到：</p>
 * <ul>
 *   <li>{@link CustomerAdminService} —— 客户列表与客户 360</li>
 *   <li>{@link PackageAdminService} —— 套餐与升级单</li>
 *   <li>{@link ReviewAdminService} —— 投诉与评价闭环</li>
 *   <li>{@link ReportAdminService} —— 销售 / 财务报表与下钻</li>
 *   <li>{@link RefundAdminService} —— 退款审批</li>
 *   <li>{@link InvoiceService} —— 发票开具 / 驳回</li>
 *   <li>{@link MarketingAdminService} —— 套餐营销看板</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

    @Autowired private CustomerAdminService customerAdminService;
    @Autowired private PackageAdminService packageAdminService;
    @Autowired private ReviewAdminService reviewAdminService;
    @Autowired private ReportAdminService reportAdminService;
    @Autowired private RefundAdminService refundAdminService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private MarketingAdminService marketingAdminService;

    // ==================================================================== 客户

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('customer:view')")
    public List<Map<String, Object>> customers(@RequestParam(required = false) String keyword) {
        return customerAdminService.customers(keyword);
    }

    /**
     * 客户 360 全景：档案 + 业务订单 + 合约 + 流量 + 投诉评价 + 安装工单 + 升级申请 + 汇总指标。
     */
    @GetMapping("/customer/{id}/360")
    @PreAuthorize("hasAuthority('customer:view')")
    public Map<String, Object> customer360(@PathVariable String id) {
        return customerAdminService.customer360(id);
    }

    // ==================================================================== 套餐

    @GetMapping("/packages")
    @PreAuthorize("hasAuthority('package:view')")
    public List<Map<String, Object>> packages() {
        return packageAdminService.packages();
    }

    /** 套餐保存：主表 upsert + 轮播图重建 + 动态参数重建（未变更的选项保留原加价）。 */
    @PostMapping("/packages")
    @PreAuthorize("hasAuthority('package:edit')")
    public Map<String, Object> savePackage(@RequestBody Map<String, Object> req) {
        return packageAdminService.savePackage(req);
    }

    // ==================================================================== 套餐升级单

    @GetMapping("/upgrade-orders")
    @PreAuthorize("hasAuthority('upgrade:view')")
    public List<Map<String, Object>> upgradeOrders() {
        return packageAdminService.upgradeOrders();
    }

    // ==================================================================== 投诉与评价

    @GetMapping("/reviews")
    @PreAuthorize("hasAuthority('review:view')")
    public List<Map<String, Object>> reviews(@RequestParam(required = false) String type,
                                             @RequestParam(required = false) String status) {
        return reviewAdminService.reviews(type, status);
    }

    @PutMapping("/reviews/{id}/close")
    @PreAuthorize("hasAuthority('review:view')")
    public Map<String, Object> closeReview(@PathVariable String id) {
        return reviewAdminService.closeReview(id);
    }

    // ==================================================================== 销售 / 财务

    @GetMapping("/sales/report")
    @PreAuthorize("hasAuthority('sales:view')")
    public List<Map<String, Object>> salesReport() {
        return reportAdminService.salesReport();
    }

    @GetMapping("/finance/report")
    @PreAuthorize("hasAuthority('finance:view')")
    public List<Map<String, Object>> financeReport() {
        return reportAdminService.financeReport();
    }

    // ============================================================ 报表下钻明细（US-2.2）

    /** 销售业绩明细下钻：按销售 / 月份返回底层业务订单。 */
    @GetMapping("/sales/report/detail")
    @PreAuthorize("hasAuthority('sales:view')")
    public List<Map<String, Object>> salesReportDetail(
            @RequestParam(required = false) String salesName,
            @RequestParam(required = false) String month) {
        return reportAdminService.salesReportDetail(salesName, month);
    }

    /** 财务月度明细下钻：某月营收/退款/赔付的底层业务订单 + 赔付工单。 */
    @GetMapping("/finance/report/detail")
    @PreAuthorize("hasAuthority('finance:view')")
    public Map<String, Object> financeReportDetail(@RequestParam String month) {
        return reportAdminService.financeReportDetail(month);
    }

    // ==================================================================== 退款 / 对账状态机

    /** 退款单列表，可选 ?status=PENDING/APPROVED/REJECTED/REFUNDED 过滤。 */
    @GetMapping("/refunds")
    @PreAuthorize("hasAuthority('finance:view')")
    public List<Map<String, Object>> refunds(@RequestParam(required = false) String status) {
        return refundAdminService.refunds(status);
    }

    /** 审批退款：PENDING -> REFUNDED，生成退款流水号，并将业务订单置 REFUND（营收扣减）。 */
    @PostMapping("/refunds/{id}/approve")
    @PreAuthorize("hasAuthority('finance:view')")
    public Map<String, Object> approveRefund(@PathVariable String id,
                                             @RequestBody(required = false) Map<String, Object> body) {
        return refundAdminService.approveRefund(id, body);
    }

    /** 驳回退款：PENDING -> REJECTED。 */
    @PostMapping("/refunds/{id}/reject")
    @PreAuthorize("hasAuthority('finance:view')")
    public Map<String, Object> rejectRefund(@PathVariable String id,
                                            @RequestBody(required = false) Map<String, Object> body) {
        return refundAdminService.rejectRefund(id, body);
    }

    // ==================================================================== 电子发票（占位）

    /** 发票申请列表，可选 ?status=PENDING/OPENED/REJECTED 过滤。 */
    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('finance:view')")
    public List<Map<String, Object>> invoices(@RequestParam(required = false) String status) {
        return invoiceService.list(status);
    }

    /** 开具发票：PENDING -> OPENED，生成发票号与占位 PDF 地址。 */
    @PostMapping("/invoices/{id}/open")
    @PreAuthorize("hasAuthority('finance:view')")
    public Map<String, Object> openInvoice(@PathVariable String id,
                                           @RequestBody(required = false) Map<String, Object> body) {
        return invoiceService.open(id, body);
    }

    /** 驳回发票：PENDING -> REJECTED，记录驳回原因。 */
    @PostMapping("/invoices/{id}/reject")
    @PreAuthorize("hasAuthority('finance:view')")
    public Map<String, Object> rejectInvoice(@PathVariable String id,
                                             @RequestBody(required = false) Map<String, Object> body) {
        return invoiceService.reject(id, body);
    }

    // ==================================================================== 套餐营销看板

    /**
     * 套餐营销看板：汇总指标 + 套餐销量排行 + 业务类型分布 + 升级单状态分布 + 客户分层分布 + 近 6 月营收趋势。
     */
    @GetMapping("/product/marketing")
    @PreAuthorize("hasAuthority('package:view')")
    public Map<String, Object> productMarketing() {
        return marketingAdminService.productMarketing();
    }

    /**
     * 套餐营销看板「转化漏斗」下钻：返回指定漏斗阶段的订单 / 升级单明细。
     * 前端点击漏斗阶段时调用（stage ∈ 业务订单/已支付/已完成/升级申请/升级生效）。
     */
    @GetMapping("/product/funnel-detail")
    @PreAuthorize("hasAuthority('package:view')")
    public List<Map<String, Object>> productFunnelDetail(@RequestParam String stage) {
        return marketingAdminService.funnelDetail(stage);
    }
}
