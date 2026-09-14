package com.broadband.install.model;

/**
 * 师傅时段容量配置。adjacentCap / nonAdjacentCap 为 null 时回退到 CapacityPolicy 默认值
 * （相邻 3-4 单、非相邻 1-2 单）。可按师傅技能分级微调上限。
 */
public class WorkerCapacity {
    public String workerId;
    public String timeSlot;
    public Integer adjacentCap;       // 相邻小区每时段可接单上限（默认 4，下限 3）
    public Integer nonAdjacentCap;    // 非相邻小区每时段可接单上限（默认 2，下限 1）

    public WorkerCapacity() {}

    public WorkerCapacity(String workerId, String timeSlot, Integer adjacentCap, Integer nonAdjacentCap) {
        this.workerId = workerId;
        this.timeSlot = timeSlot;
        this.adjacentCap = adjacentCap;
        this.nonAdjacentCap = nonAdjacentCap;
    }
}
