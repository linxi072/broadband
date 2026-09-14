package com.broadband.product.model;

/**
 * 套餐升级申请单（纯模型，无 Spring/ORM 依赖）。
 * 对应 PC 后台「套餐升级管理」模块的数据源；小程序提交升档后落此表。
 */
public class PackageUpgradeOrder {
    public String id;
    public String customerId;
    public String fromPackageId;
    public String targetBandKey;   // 目标带宽选项键
    public String addonKeys;       // 加购选项键，逗号分隔
    public String effectType;      // immediate / nextMonth
    public int currentFee;
    public int monthDiff;
    public int newFee;
    public int oneTimeDiff;
    public String status = "SUBMITTED"; // SUBMITTED / EFFECTIVE / REJECTED
    public long createdTime;

    public PackageUpgradeOrder() {}
}
