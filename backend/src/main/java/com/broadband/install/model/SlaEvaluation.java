package com.broadband.install.model;

import com.broadband.install.model.SlaEnums.SlaStatus;

/**
 * 单次评估返回：含评估后的 SlaRecord 与可能产生的赔付工单（仅超时/不达标时非空）。
 */
public class SlaEvaluation {
    public SlaRecord record;
    public Compensation compensation;
    public boolean skipped = false;        // 规则未启用时跳过
    public String summary;

    public SlaEvaluation() {}

    public String describe() {
        if (skipped) return "[跳过] 规则未启用: " + (record != null ? record.orderId : "?");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] 工单 %s 业务 %s -> %s",
                record.id, record.orderId,
                record.orderType != null ? record.orderType.label : "-",
                record.slaStatus == SlaStatus.MET ? "达标" : "超时"));
        if (record.slaStatus == SlaStatus.OVERTIME) {
            if (record.overtimeMinutes > 0) sb.append(" 超时 ").append(record.overtimeMinutes).append(" 分钟");
            if (compensation != null)
                sb.append(" | 触发赔付: ").append(compensation.compAmount).append("元/")
                  .append(compensation.compType.label).append(" | ").append(compensation.reason);
        }
        return sb.toString();
    }
}
