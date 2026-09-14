package com.broadband.install.model;

import com.broadband.install.model.SlaEnums.CompType;
import com.broadband.install.model.SlaEnums.CompUnit;
import com.broadband.install.model.SlaEnums.EvalType;
import com.broadband.install.model.SlaEnums.OrderType;

/**
 * SLA 规则配置（对应 DB 表 sla_rule）。
 * 参考电信「当日装当日修 / 慢必赔」、移动「装机/修复超时赔 20 元 / 网速不达标赔 20 元」沉淀。
 */
public class SlaRule {
    public String id;
    public OrderType orderType;          // 适用业务类型
    public String slaName;               // 规则名，如「新装-当日装」「报修-当日修」「装机-网速达标」
    public EvalType evalType;            // 时限类 / 速率类
    public int promisedHours = 24;       // 时限类：承诺完成小时数（accept/appoint + 该值）
    public int graceMinutes = 30;        // 时限类：宽限分钟，避免临界秒级误判
    public double minSpeedMbps = 0;      // 速率类：达标最低速率（Mbps）
    public CompType compType;            // 赔付形式
    public double compAmount;            // 赔付基准金额/额度
    public CompUnit compUnit;            // 计算单位（每单固定 / 每超时小时）
    public Double maxCompAmount;         // 赔付封顶（可选）
    public boolean enabled = true;

    public SlaRule() {}
}
