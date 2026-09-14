package com.broadband.product.model;

import java.util.List;

/**
 * 参数组视图（含其选项），用于前端渲染单选/多选。
 */
public class PackageParamVO {
    public String key;          // 对应 PackageParam.groupKey
    public String name;
    public String type;         // SINGLE / MULTI
    public boolean required;
    public List<PackageParamOption> options;

    public PackageParamVO() {}
}
