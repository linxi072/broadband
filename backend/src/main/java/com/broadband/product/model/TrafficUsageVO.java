package com.broadband.product.model;

import java.util.List;

/** 客户流量用量视图对象（纯模型，无 Spring 依赖） */
public class TrafficUsageVO {
    public String pkgName;
    public int mobileTotal;
    public int mobileUsed;
    public String unit;
    public int broadbandHours;
    public String broadbandPeak;
    public int pct;
    public int remaining;
    public List<Integer> trend;
    public boolean warn;

    public TrafficUsageVO() {}

    public TrafficUsageVO(String pkgName, int mobileTotal, int mobileUsed, String unit,
                          int broadbandHours, String broadbandPeak, int pct, int remaining,
                          List<Integer> trend, boolean warn) {
        this.pkgName = pkgName;
        this.mobileTotal = mobileTotal;
        this.mobileUsed = mobileUsed;
        this.unit = unit;
        this.broadbandHours = broadbandHours;
        this.broadbandPeak = broadbandPeak;
        this.pct = pct;
        this.remaining = remaining;
        this.trend = trend;
        this.warn = warn;
    }

    @Override
    public String toString() {
        return "TrafficUsageVO{pkg=" + pkgName + ", used=" + mobileUsed + unit + "/" + mobileTotal + unit
                + ", pct=" + pct + "%, peak=" + broadbandPeak + ", warn=" + warn + "}";
    }
}
