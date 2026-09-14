package com.broadband.install.model;

/** 单条派单结果。 */
public class DispatchResult {
    public String workOrderId;
    public String workerId;
    public String timeSlot;
    public String clusterId;       // 相邻聚类簇 ID（用于路线合并）
    public boolean adjacentRoute;  // true=走相邻合并路线；false=非相邻独立派单
    public String note;

    public DispatchResult(String workOrderId, String workerId, String timeSlot,
                          String clusterId, boolean adjacentRoute, String note) {
        this.workOrderId = workOrderId;
        this.workerId = workerId;
        this.timeSlot = timeSlot;
        this.clusterId = clusterId;
        this.adjacentRoute = adjacentRoute;
        this.note = note;
    }
}
