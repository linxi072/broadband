package com.broadband.product.service;

import com.broadband.product.mapper.ReportAdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售 / 财务报表与下钻明细（PC 后台读侧聚合）。数据访问经 {@link ReportAdminMapper}。
 *
 * <p>聚合口径：</p>
 * <ul>
 *   <li>销售「完成率」= 该销售已完成订单 / 总订单（不用臆造的转化率——没有线索数据支撑）。</li>
 *   <li>财务「应收」= 该月未支付订单金额；赔付金额取自 compensation 表（真实成本）。</li>
 * </ul>
 *
 * <p>下钻明细（US-2.2）与聚合共用同一套过滤条件，保证「点哪一行看到的就是这一行的数据」。</p>
 */
@Service
public class ReportAdminService {

    @Autowired private ReportAdminMapper reportAdminMapper;

    // ==================================================================== 销售

    /** 销售业绩报表：按销售 × 月份聚合订单量、金额与完成率。 */
    public List<Map<String, Object>> salesReport() {
        return reportAdminMapper.selectSalesReport();
    }

    /** 销售业绩明细下钻：按销售 / 月份返回底层业务订单（构成销售报表每一行的明细）。 */
    public List<Map<String, Object>> salesReportDetail(String salesName, String month) {
        return reportAdminMapper.selectSalesDetail(salesName, month);
    }

    // ==================================================================== 财务

    /** 财务月报：按月聚合营收 / 退款 / 取消 / 应收 / 赔付，并标记对账状态。 */
    public List<Map<String, Object>> financeReport() {
        String currentMonth = LocalDate.now().toString().substring(0, 7);
        List<Map<String, Object>> rows = reportAdminMapper.selectFinanceReport();
        Map<String, Long> compByMonth = new LinkedHashMap<>();
        for (Map<String, Object> c : reportAdminMapper.selectCompensationByMonth()) {
            compByMonth.put(String.valueOf(c.get("month")), ((Number) c.get("comp")).longValue());
        }
        for (Map<String, Object> row : rows) {
            String month = String.valueOf(row.get("month"));
            row.put("compensation", compByMonth.getOrDefault(month, 0L));
            row.put("status", month.equals(currentMonth) ? "对账中" : "已结账");
        }
        return rows;
    }

    /** 财务月度明细下钻：某月营收/退款/赔付的底层业务订单 + 赔付工单。 */
    public Map<String, Object> financeReportDetail(String month) {
        List<Map<String, Object>> orders = reportAdminMapper.selectFinanceOrders(month);
        List<Map<String, Object>> comps = reportAdminMapper.selectFinanceCompensations(month);
        List<Map<String, Object>> rows = new java.util.ArrayList<>(orders);
        rows.addAll(comps);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("orderCount", orders.size());
        result.put("compCount", comps.size());
        result.put("rows", rows);
        return result;
    }
}
