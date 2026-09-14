package com.broadband.install.model;

import com.broadband.install.model.SlaEnums.OrderType;
import com.broadband.install.model.SlaEnums.SlaStatus;

/**
 * SLA 评估记录（对应 DB 表 sla_record）。每条安装/报修工单在完工时生成一条，由 SlaEngine 评估。
 */
public class SlaRecord {
    public String id;
    public String orderId;             // 关联 work_order.id
    public OrderType orderType;        // 冗余业务类型，便于统计
    public String custName;            // 客户名（赔付工单展示用）
    public String ruleId;              // 命中的 SLA 规则

    // 时限类字段（毫秒时间戳）
    public long acceptTime;            // 受理时间
    public long appointedTime;         // 预约上门时间（可空，取 0）
    public Long completeTime;          // 完工时间（空 = 尚未完工，PENDING）

    // 速率类字段
    public Double speedTestMbps;       // 装机测速结果（Mbps，空 = 未测速，PENDING）

    // 评估结果
    public long promisedTime;          // 承诺完成时间（时限类有效）
    public SlaStatus slaStatus = SlaStatus.PENDING;
    public int overtimeMinutes;        // 超时分钟数（时限类）

    public long createdTime;

    public SlaRecord() {}
}
