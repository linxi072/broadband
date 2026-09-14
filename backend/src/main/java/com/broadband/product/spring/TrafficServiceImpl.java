package com.broadband.product.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.product.mapper.CustomerMapper;
import com.broadband.product.mapper.PackageMapper;
import com.broadband.product.mapper.TrafficUsageMapper;
import com.broadband.product.model.Customer;
import com.broadband.product.model.PackageInfo;
import com.broadband.product.model.TrafficRecord;
import com.broadband.product.model.TrafficUsageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 流量监控服务实现（真实数据库版，已去除 mock）。
 *
 * <p>数据流：traffic_usage 取该客户最新一期记录 -> 计算 pct / remaining / warn -> 注入 7 日趋势，
 * 套餐名从 customer + package_info 关联得到（页面标题展示用）。
 * customerId 未命中或当期无记录时返回 {@code null}，由前端走本地兜底。</p>
 */
@Service
public class TrafficServiceImpl implements TrafficServiceApi {

    /** 剩余流量低于该值（G）触发预警。 */
    private static final int WARN_REMAINING_GB = 5;
    private static final String UNIT = "G";

    @Autowired private TrafficUsageMapper trafficUsageMapper;
    @Autowired private CustomerMapper customerMapper;
    @Autowired private PackageMapper packageMapper;

    @Override
    public TrafficUsageVO getUsage(String customerId) {
        if (customerId == null || customerId.isEmpty()) return null;

        TrafficRecord rec = trafficUsageMapper.selectOne(new QueryWrapper<TrafficRecord>()
                .eq("customer_id", customerId)
                .orderByDesc("period_month")
                .last("limit 1"));
        if (rec == null) return null;

        int total = rec.mobileTotal;
        int used = rec.mobileUsed;
        int remaining = Math.max(0, total - used);
        int pct = total <= 0 ? 0 : (int) Math.round(used * 100.0 / total);
        boolean warn = remaining <= WARN_REMAINING_GB;

        List<Integer> trend = new ArrayList<>();
        if (rec.dailyTrend != null && !rec.dailyTrend.isEmpty()) {
            for (String s : rec.dailyTrend.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) trend.add(Integer.parseInt(t));
            }
        }

        return new TrafficUsageVO(resolvePackageName(customerId), total, used, UNIT,
                rec.broadbandHours, rec.broadbandPeak, pct, remaining, trend, warn);
    }

    /** 客户当前套餐名（用于页面展示，查不到返回 null）。 */
    private String resolvePackageName(String customerId) {
        Customer c = customerMapper.selectOne(new QueryWrapper<Customer>()
                .eq("id", customerId).last("limit 1"));
        if (c == null || c.packageId == null) return null;
        PackageInfo p = packageMapper.selectOne(new QueryWrapper<PackageInfo>()
                .eq("id", c.packageId).last("limit 1"));
        return p == null ? null : p.name;
    }
}
