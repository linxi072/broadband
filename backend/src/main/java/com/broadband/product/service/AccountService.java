package com.broadband.product.service;

import com.broadband.common.Values;
import com.broadband.product.mapper.AccountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 账户与账单中心（C 端，V1.14 运营留存）：账户概览与账单明细。数据访问经 {@link AccountMapper}。
 */
@Service
public class AccountService {

    @Autowired private AccountMapper accountMapper;

    /** 账户概览：积分余额 / 本月消费 / 合约到期 / 客户分层。 */
    public Map<String, Object> summary(String customerId) {
        if (Values.isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        Map<String, Object> cust = accountMapper.selectCustomer(customerId);
        if (cust == null) throw new IllegalArgumentException("客户不存在：" + customerId);

        // 积分余额
        Map<String, Object> pa = accountMapper.selectPoints(customerId);
        int points = pa == null ? 0 : Values.intOf(pa.get("balance"));

        // 本月消费：业务订单（PAID/INSTALLING/DONE）当日历月内
        long[] m = thisMonthRange();
        Integer monthConsume = accountMapper.selectMonthConsume(customerId, m[0], m[1]);

        // 合约到期日（取最近一条 ACTIVE 合约）
        Map<String, Object> contract = accountMapper.selectContractEnd(customerId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("customerId", customerId);
        resp.put("name", cust.get("name"));
        resp.put("level", cust.get("level"));
        resp.put("points", points);
        resp.put("monthConsume", monthConsume == null ? 0 : monthConsume);
        resp.put("contractEnd", contract == null ? null : contract.get("end_date"));
        return resp;
    }

    /** 账单明细：按账期聚合业务订单（period 为 yyyy-MM 可选过滤）。 */
    public List<Map<String, Object>> bills(String customerId, String period) {
        if (Values.isBlank(customerId)) return List.of();
        List<Map<String, Object>> rows = accountMapper.selectBills(customerId, period);
        for (Map<String, Object> r : rows) {
            r.put("statusText", orderStatusText(String.valueOf(r.get("status"))));
        }
        return rows;
    }

    // ---------------------------------------------------------------- 辅助

    /** 当月起止时间戳 [start, end)。 */
    private long[] thisMonthRange() {
        ZoneId z = ZoneId.systemDefault();
        LocalDate first = LocalDate.now().withDayOfMonth(1);
        long start = first.atStartOfDay(z).toInstant().toEpochMilli();
        long end = first.plusMonths(1).atStartOfDay(z).toInstant().toEpochMilli();
        return new long[]{start, end};
    }

    private static String orderStatusText(String s) {
        if (s == null) return "未知";
        return switch (s) {
            case "PENDING" -> "待支付";
            case "PAID" -> "已支付";
            case "INSTALLING" -> "安装中";
            case "DONE" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "REFUND" -> "已退款";
            default -> s;
        };
    }
}
