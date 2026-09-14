package com.broadband.install.model;

/** 工单状态。 */
public enum WorkOrderStatus {
    PENDING,    // 待派单
    ASSIGNED,   // 已派单
    INSTALLING, // 施工中
    DONE,       // 已完工
    CANCELLED   // 已取消
}
