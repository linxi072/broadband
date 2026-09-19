package com.broadband.product.service;

import com.broadband.product.model.TrafficUsageVO;

/**
 * 流量监控服务接口。
 */
public interface TrafficServiceApi {

    /**
     * 查询客户当前流量用量（手机流量 + 宽带时长 + 7 日趋势 + 阈值提醒）。
     * @param customerId 客户 ID
     * @return 用量视图；无数据返回 null
     */
    TrafficUsageVO getUsage(String customerId);
}
