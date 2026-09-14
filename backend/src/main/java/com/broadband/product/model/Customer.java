package com.broadband.product.model;

/**
 * 客户（纯模型，无 Spring/ORM 依赖）。
 * level 为客户分层：NORMAL / SILVER / GOLD / VIP。
 */
public class Customer {
    public String id;
    public String name;
    public String phone;
    public String openid;         // 微信 openid（小程序真实登录绑定，演示态为空）
    public String level = "NORMAL";
    public String packageId;      // 当前套餐
    public String communityId;    // 所属小区
    public String address;
    public String status = "ACTIVE"; // ACTIVE / SUSPENDED / CLOSED
    public long createdTime;

    public Customer() {}
}
