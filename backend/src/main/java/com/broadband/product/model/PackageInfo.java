package com.broadband.product.model;

/**
 * 套餐（主表）。融合套餐含宽带+手机+IPTV+副卡等，详见 PackageConvergeItem。
 * 状态 ON_SHELF=上架 / OFF_SHELF=下架。
 */
public class PackageInfo {
    public String id;
    public String name;
    public String category;     // 套餐分类，如 "融合套餐" / "单宽带"
    public int monthlyFee;      // 月租（元）
    public int originalFee;     // 原价（元）
    public String status = "ON_SHELF";
    public int deposit;         // 调测费（元，一次性）
    public int deviceRent;      // 设备租赁（元/月）
    public String penalty;      // 违约金说明
    public String slaInfo;      // 装维 SLA 说明（如「城区当日装当日修，超时赔付」）

    public PackageInfo() {}
}
