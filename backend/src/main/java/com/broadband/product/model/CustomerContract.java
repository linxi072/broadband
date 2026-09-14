package com.broadband.product.model;

/**
 * 客户合约（纯模型，无 Spring/ORM 依赖）。
 * 用于套餐升级的「剩余月数」与一次性补差折算。
 * 日期刻意用 yyyy-MM-dd 字符串，避免引入时间类型映射差异。
 */
public class CustomerContract {
    public String id;
    public String customerId;
    public String packageId;
    public int monthlyFee;      // 合约月费（元）
    public String startDate;    // yyyy-MM-dd
    public String endDate;      // yyyy-MM-dd
    public String status = "ACTIVE"; // ACTIVE / EXPIRED / TERMINATED

    public CustomerContract() {}
}
