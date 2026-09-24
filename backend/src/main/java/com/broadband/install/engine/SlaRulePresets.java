package com.broadband.install.engine;

import com.broadband.install.model.SlaEnums;
import com.broadband.install.model.SlaRule;

import java.util.ArrayList;
import java.util.List;

/**
 * SLA 默认规则预设（种子数据）。参考运营商实践：
 *  - 电信：城区「当日装当日修」（16 点前受理当日竣工）、慢必赔（超时赔流量）。
 *  - 移动：装机/修复超时赔 20 元、网速不达标赔 20 元。
 * 业务接入时可作为 sla_rule 表初始数据写入。
 */
public final class SlaRulePresets {

    private SlaRulePresets() {}

    public static List<SlaRule> defaults() {
        List<SlaRule> rules = new ArrayList<>();

        // 1) 新装-当日装（时限类，24h 内完工，超时按每小时 5 元、封顶 20）
        SlaRule r1 = new SlaRule();
        r1.id = "R-NEW-DAY";
        r1.orderType = SlaEnums.OrderType.NEW_INSTALL;
        r1.slaName = "新装-当日装";
        r1.evalType = SlaEnums.EvalType.TIME;
        r1.promisedHours = 24;
        r1.graceMinutes = 30;
        r1.compType = SlaEnums.CompType.VOUCHER;
        r1.compAmount = 5.0;
        r1.compUnit = SlaEnums.CompUnit.PER_OVERTIME_HOUR;
        r1.maxCompAmount = 20.0;
        rules.add(r1);

        // 2) 报修-当日修（时限类，同 24h 规则）
        SlaRule r2 = new SlaRule();
        r2.id = "R-REPAIR-DAY";
        r2.orderType = SlaEnums.OrderType.REPAIR;
        r2.slaName = "报修-当日修";
        r2.evalType = SlaEnums.EvalType.TIME;
        r2.promisedHours = 24;
        r2.graceMinutes = 30;
        r2.compType = SlaEnums.CompType.VOUCHER;
        r2.compAmount = 5.0;
        r2.compUnit = SlaEnums.CompUnit.PER_OVERTIME_HOUR;
        r2.maxCompAmount = 20.0;
        rules.add(r2);

        // 3) 装机-网速达标（速率类，千兆/500M 套餐需测速达标，不达标赔 20 元）
        SlaRule r3 = new SlaRule();
        r3.id = "R-SPEED-500";
        r3.orderType = SlaEnums.OrderType.NEW_INSTALL;
        r3.slaName = "装机-网速达标(≥500M)";
        r3.evalType = SlaEnums.EvalType.SPEED;
        r3.minSpeedMbps = 500.0;
        r3.compType = SlaEnums.CompType.CASH;
        r3.compAmount = 20.0;
        r3.compUnit = SlaEnums.CompUnit.PER_ORDER;
        r3.maxCompAmount = 20.0;
        rules.add(r3);

        return rules;
    }

    /** 按业务类型取首条匹配规则（演示用；真实环境按 orderType+slaName 精确匹配）。 */
    public static SlaRule byOrderType(List<SlaRule> rules, SlaEnums.OrderType type) {
        return rules.stream().filter(r -> r.orderType == type).findFirst().orElse(null);
    }
}
