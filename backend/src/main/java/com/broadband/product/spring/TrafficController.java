package com.broadband.product.spring;

import com.broadband.product.model.TrafficUsageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流量监控 REST 接口。
 *  - GET /api/traffic/usage?customerId=xxx   手机流量 + 宽带时长 + 7 日趋势 + 阈值提醒
 */
@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    @Autowired
    private TrafficServiceApi trafficService;

    @GetMapping("/usage")
    public TrafficUsageVO usage(@RequestParam String customerId) {
        return trafficService.getUsage(customerId);
    }
}
