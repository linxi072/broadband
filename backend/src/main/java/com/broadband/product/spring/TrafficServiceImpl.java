package com.broadband.product.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.product.engine.TrafficQueryEngine;
import com.broadband.product.mapper.TrafficUsageMapper;
import com.broadband.product.model.TrafficRecord;
import com.broadband.product.model.TrafficUsageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 流量监控服务实现。
 * 当前默认走 {@link TrafficQueryEngine} 的 mock 聚合；
 * 接入后改为：从 traffic_usage 取当期记录 -> 计算 pct/remaining/warn -> 注入 7 日趋势。
 */
@Service
public class TrafficServiceImpl implements TrafficServiceApi {

    @Autowired
    private TrafficUsageMapper trafficUsageMapper;

    @Override
    public TrafficUsageVO getUsage(String customerId) {
        // TODO 接入 DB：取当期 TrafficRecord 后聚合
        // TrafficRecord rec = trafficUsageMapper.selectOne(
        //     new QueryWrapper<TrafficRecord>().eq("customer_id", customerId)
        //         .orderByDesc("period_month").last("LIMIT 1"));
        // if (rec == null) return null;
        // return assemble(rec);
        return TrafficQueryEngine.compute(customerId);
    }

    /** DB 接入后的组装示例 */
    private TrafficUsageVO assemble(TrafficRecord rec) {
        int remaining = rec.mobileTotal - rec.mobileUsed;
        int pct = rec.mobileTotal == 0 ? 0 : rec.mobileUsed * 100 / rec.mobileTotal;
        boolean warn = remaining <= 5;
        List<Integer> trend = new ArrayList<>();
        if (rec.dailyTrend != null && !rec.dailyTrend.isEmpty()) {
            for (String s : rec.dailyTrend.split(",")) {
                trend.add(Integer.parseInt(s.trim()));
            }
        }
        return new TrafficUsageVO("1000M 融合", rec.mobileTotal, rec.mobileUsed, "G",
                rec.broadbandHours, rec.broadbandPeak, pct, remaining, trend, warn);
    }
}
