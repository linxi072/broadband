package com.broadband.product.model;

/** 套餐升级补差预览（纯模型，无 Spring 依赖） */
public class PackageUpgradePreview {
    public int currentFee;       // 当前月费
    public int monthDiff;        // 月补差
    public int newFee;           // 升级后月费
    public int oneTimeDiff;      // 合约剩余折算一次性补差
    public String effectType;    // immediate / nextMonth

    public PackageUpgradePreview() {}

    public PackageUpgradePreview(int currentFee, int monthDiff, int newFee, int oneTimeDiff) {
        this.currentFee = currentFee;
        this.monthDiff = monthDiff;
        this.newFee = newFee;
        this.oneTimeDiff = oneTimeDiff;
    }

    @Override
    public String toString() {
        return "PackageUpgradePreview{currentFee=" + currentFee + ", monthDiff=" + monthDiff
                + ", newFee=" + newFee + ", oneTimeDiff=" + oneTimeDiff + "}";
    }
}
