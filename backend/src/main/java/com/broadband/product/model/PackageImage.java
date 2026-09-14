package com.broadband.product.model;

/**
 * 套餐图片：主图(MAIN) 1 张 / 轮播图(CAROUSEL) 多张 / 详情图(DETAIL) 多张。
 */
public class PackageImage {
    public String id;
    public String packageId;
    public String type;         // MAIN / CAROUSEL / DETAIL
    public String url;          // 图片访问地址（OSS / 本地）
    public int sortOrder;

    public PackageImage() {}
}
