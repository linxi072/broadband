package com.broadband.install.model;

/** 师傅时段容量看板项（供 PC 派单页 / 小程序师傅端展示）。 */
public class CapacityBoardItem {
    public String workerId;
    public String workerName;
    public String timeSlot;
    public int adjUsed;      // 已排相邻工单
    public int adjCap;       // 相邻容量上限
    public int nonUsed;      // 已排非相邻工单
    public int nonCap;       // 非相邻容量上限
    public boolean full;    // 相邻与非相邻均已满 -> 不可再接

    public CapacityBoardItem() {}

    public CapacityBoardItem(String workerId, String workerName, String timeSlot,
                            int adjUsed, int adjCap, int nonUsed, int nonCap, boolean full) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.timeSlot = timeSlot;
        this.adjUsed = adjUsed;
        this.adjCap = adjCap;
        this.nonUsed = nonUsed;
        this.nonCap = nonCap;
        this.full = full;
    }
}
