package com.broadband.product.model;

/**
 * 融合套餐组成项（宽带 / 手机 / IPTV / 副卡 等）。
 */
public class PackageConvergeItem {
    public String id;
    public String packageId;
    public String label;        // 组成名，如「宽带」
    public String desc;         // 说明，如「500M 高速宽带」
    public int sortOrder;

    public PackageConvergeItem() {}
}
