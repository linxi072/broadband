package com.broadband.product.model;

import java.util.List;

/**
 * 套餐详情聚合视图（GET /api/package/detail 返回体）。
 * 由 info + images + converge + params(含 options) 组装而成。
 */
public class PackageDetailVO {
    public String id;
    public String name;
    public String category;
    public int monthlyFee;
    public int originalFee;
    public String status;
    public int deposit;
    public int deviceRent;
    public String penalty;
    public String slaInfo;

    public List<PackageImage> images;
    public List<PackageConvergeItem> converge;
    public List<PackageParamVO> params;

    public PackageDetailVO() {}
}
