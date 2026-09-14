package com.broadband.product.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.util.Date;

/** 流量用量明细（traffic_usage 表，MyBatis-Plus 实体） */
@TableName("traffic_usage")
public class TrafficRecord {
    @TableId("id")
    public String id;

    @TableField("customer_id")
    public String customerId;

    /** 统计周期，如 2026-09 */
    @TableField("period_month")
    public String periodMonth;

    /** 手机流量套餐总量（G） */
    @TableField("mobile_total")
    public int mobileTotal;

    /** 手机流量已用（G） */
    @TableField("mobile_used")
    public int mobileUsed;

    /** 宽带当月使用时长（小时） */
    @TableField("broadband_hours")
    public int broadbandHours;

    /** 宽带峰值速率，如 943M */
    @TableField("broadband_peak")
    public String broadbandPeak;

    /** 近 7 日流量趋势（G），逗号分隔后转 List 注入 VO */
    @TableField("daily_trend")
    public String dailyTrend;

    @TableField("updated_at")
    public Date updatedAt;

    public TrafficRecord() {}
}
