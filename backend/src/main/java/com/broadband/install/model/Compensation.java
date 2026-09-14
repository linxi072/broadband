package com.broadband.install.model;

import com.broadband.install.model.SlaEnums.CompStatus;
import com.broadband.install.model.SlaEnums.CompType;
import com.broadband.install.model.SlaEnums.OrderType;

/**
 * 赔付工单（对应 DB 表 compensation）。由 SlaEngine 在 SLA 超时/不达标时生成，状态机：
 * PENDING(待赔付) -> VERIFYING(待核实) -> PAID(已赔付) / REJECTED(已驳回)。
 * 对应电信「慢必赔」、移动「装机/修复超时赔、网速不达标赔」。
 */
public class Compensation {
    public String id;
    public String slaRecordId;        // 关联 sla_record.id
    public String orderId;
    public String custName;
    public OrderType orderType;
    public CompType compType;
    public double compAmount;
    public String reason;             // 触发原因，如「装机超时 3.2h 触发慢必赔」
    public CompStatus status = CompStatus.PENDING;
    public long createdTime;

    public Compensation() {}
}
