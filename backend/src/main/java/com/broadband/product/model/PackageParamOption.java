package com.broadband.product.model;

/**
 * 参数选项。例如速率参数下的 300M/500M/1000M，增值服务下的 FTTR/全屋WiFi…
 * extraFee 为该选项相对套餐月租的加价（元/月）。
 */
public class PackageParamOption {
    public String id;
    public String paramId;
    public String value;        // 选项值，如 "500M" / "FTTR全屋光纤"
    public int extraFee;        // 月加价（元/月）
    public int sortOrder;

    public PackageParamOption() {}
}
