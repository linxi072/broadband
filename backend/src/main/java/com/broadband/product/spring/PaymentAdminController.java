package com.broadband.product.spring;

import com.broadband.common.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.broadband.product.pay.PaymentService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付管理后台（v1.15 支付真闭环）：支付流水查询与退款处理。
 *
 * <p>权限 {@code payment:view}（菜单 M180/M181/M182）。流水直接来自 pay_transaction 真实表，
 * 与 C 端支付状态机同源；退款调用 {@link PaymentService#refund} 回写状态并同步渠道退款。</p>
 */
@RestController
@RequestMapping("/api/admin/pay")
@PreAuthorize("hasAuthority('payment:view')")
public class PaymentAdminController {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PaymentService paymentService;

    /**
     * 支付流水列表（返回数组，前端 r.data 直接为数组）。
     * 支持 status / customerId / channel 筛选。
     */
    @GetMapping("/transactions")
    public List<Map<String, Object>> transactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String channel) {
        StringBuilder sql = new StringBuilder(
                "select pt.id as id, pt.out_trade_no as outTradeNo, pt.biz_order_id as bizOrderId, "
                        + "pt.customer_id as customerId, pt.channel as channel, pt.amount as amount, "
                        + "pt.status as status, pt.transaction_id as transactionId, pt.paid_time as paidTime, "
                        + "pt.created_time as createdTime, "
                        + "coalesce(c.name, pt.customer_id) as customerName, "
                        + "coalesce(b.package_name, '-') as packageName "
                        + "from pay_transaction pt "
                        + "left join customer c on c.id = pt.customer_id "
                        + "left join biz_order b on b.id = pt.biz_order_id where 1=1");
        List<Object> params = new ArrayList<>();
        if (Values.isNotBlank(status)) {
            sql.append(" and pt.status = ?");
            params.add(status);
        }
        if (Values.isNotBlank(customerId)) {
            sql.append(" and pt.customer_id = ?");
            params.add(customerId);
        }
        if (Values.isNotBlank(channel)) {
            sql.append(" and pt.channel = ?");
            params.add(channel);
        }
        sql.append(" order by pt.created_time desc");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    /** 退款：仅对已支付流水可发起。 */
    @PostMapping("/refund")
    public Map<String, Object> refund(@RequestBody Map<String, Object> body) {
        String outTradeNo = Values.str(body.get("outTradeNo"));
        String reason = Values.str(body.get("reason"), "管理员发起退款");
        return paymentService.refund(outTradeNo, reason);
    }
}
