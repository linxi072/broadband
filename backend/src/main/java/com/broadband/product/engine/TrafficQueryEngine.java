package com.broadband.product.engine;

import com.broadband.product.model.TrafficUsageVO;
import java.util.Arrays;
import java.util.List;

/** 流量用量计算引擎（纯 Java，可独立运行） */
public class TrafficQueryEngine {

    public static TrafficUsageVO compute(String customerId) {
        // mock：实际应聚合 traffic_usage / traffic_record
        int total = 60, used = 48;
        int remaining = total - used;
        int pct = used * 100 / total;
        boolean warn = remaining <= 5;
        List<Integer> trend = Arrays.asList(40, 55, 48, 70, 62, 85, 73);
        return new TrafficUsageVO("1000M 融合", total, used, "G",
                286, "943M", pct, remaining, trend, warn);
    }
}
