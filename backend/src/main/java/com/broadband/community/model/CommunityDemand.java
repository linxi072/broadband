package com.broadband.community.model;

/**
 * 安装需求登记（小区未覆盖时客户登记，覆盖后通知）。
 */
public class CommunityDemand {
    public String id;
    public String name;             // 登记的小区名
    public String contact;          // 联系人
    public String phone;            // 联系电话
    public String note;             // 备注
    public long createdAt;          // 登记时间（毫秒）
    public String status = "PENDING"; // PENDING / COVERED / INVALID

    public CommunityDemand() {}
}
