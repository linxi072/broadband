package com.broadband.install.model;

import java.util.List;

/**
 * 装维 SLA 看板聚合 DTO（对应 PC 后台 /sla 看板）。
 * 字段与设计稿 v1.8 的「装维 SLA 与赔付」看板一一对应。
 */
public class SlaBoard {
    public double slaRate;            // SLA 达标率（%）
    public long sameDayInstalled;     // 当日装完成数
    public long slowPayCount;         // 慢必赔单数
    public long outageWorryCount;      // 断网无忧单数
    public long avgResponseMin;       // 平均响应（分钟）
    public double monthCompAmount;    // 本月赔付金额
    public List<Compensation> recentCompensations; // 最近赔付工单（看板表格）
}
