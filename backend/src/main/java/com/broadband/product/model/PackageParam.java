package com.broadband.product.model;

/**
 * 套餐动态可选参数组（对应后台「套餐管理 / 动态参数」）。
 * 例如：速率（单选必选）、合约期（单选必选）、增值服务（多选可选）。
 */
public class PackageParam {
    public String id;
    public String packageId;
    public String groupKey;     // 业务键，如 bandwidth / contract / addon
    public String name;         // 展示名，如「宽带速率」
    public String type;         // SINGLE=单选 / MULTI=多选
    public boolean required;   // 是否必选
    public int sortOrder;

    public PackageParam() {}
}
