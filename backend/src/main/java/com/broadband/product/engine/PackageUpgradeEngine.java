package com.broadband.product.engine;

import com.broadband.product.model.PackageUpgradePreview;

/** 套餐升级补差计算引擎（纯 Java，可独立运行） */
public class PackageUpgradeEngine {

    /**
     * 计算升档补差。
     * @param currentFee       当前月费
     * @param targetExtra      目标带宽加价（相对当前带宽的月差，元）
     * @param addonExtra       增值服务加价合计
     * @param contractLeftDays 合约剩余天数
     */
    public static PackageUpgradePreview compute(int currentFee, int targetExtra, int addonExtra, int contractLeftDays) {
        int monthDiff = targetExtra + addonExtra;
        int newFee = currentFee + monthDiff;
        // 合约剩余折算一次性补差：月差 × 剩余月数（按 30 天/月）
        int oneTimeDiff = monthDiff * (contractLeftDays / 30);
        return new PackageUpgradePreview(currentFee, monthDiff, newFee, oneTimeDiff);
    }
}
