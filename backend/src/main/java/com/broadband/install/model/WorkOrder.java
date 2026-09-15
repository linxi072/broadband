package com.broadband.install.model;

/**
 * 安装工单。派单算法只处理 status = PENDING 的工单。
 * timeSlot 为派单时段标识，建议格式 "yyyy-MM-dd#AM|PM" 或 "yyyy-MM-dd HH:mm-HH:mm"。
 */
public class WorkOrder {
    public String id;
    public String communityId;     // 关联 Community.id
    public String address;
    public String timeSlot;
    public String customerName;
    public String packageDesc;
    public WorkOrderStatus status = WorkOrderStatus.PENDING;

    // 派单结果回填字段
    public String workerId;        // 指派师傅
    public String clusterId;       // 相邻聚类簇 ID
    public Boolean adjacentRoute;  // 是否走相邻合并路线（true=合并；false=非相邻独立）

    public String bizOrderId;      // 关联业务订单 biz_order.id（业务闭环追溯）

    public WorkOrder() {}

    public WorkOrder(String id, String communityId, String timeSlot, String customerName) {
        this.id = id;
        this.communityId = communityId;
        this.timeSlot = timeSlot;
        this.customerName = customerName;
        this.status = WorkOrderStatus.PENDING;
    }
}
