package com.broadband.install.engine;

import com.broadband.install.model.Compensation;
import com.broadband.install.model.SlaEnums;
import com.broadband.install.model.SlaEvaluation;
import com.broadband.install.model.SlaRecord;
import com.broadband.install.model.SlaRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 装维 SLA 计算 + 慢必赔规则引擎（纯 Java，无 Spring 依赖，可独立单测，见 SlaDemo）。
 *
 * 两类评估：
 *  1) 时限类（EvalType.TIME）：用以受理/预约为基准 + 承诺小时数计算承诺完成时间；
 *     完工时间 <= 承诺时间 => 达标；否则 => 超时，按超时小时触发赔付。
 *  2) 速率类（EvalType.SPEED）：装机测速结果 >= 规则最低速率 => 达标；否则 => 不达标触发赔付。
 *
 * 赔付金额：
 *  - PER_ORDER    ：固定 compAmount（受 maxCompAmount 封顶）
 *  - PER_OVERTIME_HOUR：compAmount × 向上取整(超时小时)，受 maxCompAmount 封顶
 */
public class SlaEngine {

    /** 评估单条记录（规则须与记录业务类型匹配）。 */
    public SlaEvaluation evaluate(SlaRecord rec, SlaRule rule) {
        SlaEvaluation ev = new SlaEvaluation();
        ev.record = rec;
        if (rule == null || !rule.enabled) {
            ev.skipped = true;
            ev.summary = "规则未启用/不存在，跳过评估";
            return ev;
        }
        rec.ruleId = rule.id;
        if (rec.orderType == null) rec.orderType = rule.orderType;

        if (rule.evalType == SlaEnums.EvalType.TIME) {
            evaluateTime(rec, rule);
        } else {
            evaluateSpeed(rec, rule);
        }

        if (rec.slaStatus == SlaEnums.SlaStatus.OVERTIME) {
            ev.compensation = buildCompensation(rec, rule);
        }
        ev.summary = ev.describe();
        return ev;
    }

    /** 批量评估（同一规则或按记录自带 rule 映射）。 */
    public List<SlaEvaluation> evaluateBatch(List<SlaRecord> records, SlaRule rule) {
        List<SlaEvaluation> out = new ArrayList<>();
        for (SlaRecord r : records) out.add(evaluate(r, rule));
        return out;
    }

    private void evaluateTime(SlaRecord rec, SlaRule rule) {
        long base = (rec.appointedTime > 0) ? rec.appointedTime : rec.acceptTime;
        long promised = base + (long) rule.promisedHours * 3_600_000L - (long) rule.graceMinutes * 60_000L;
        rec.promisedTime = promised;

        if (rec.completeTime == null) {
            rec.slaStatus = SlaEnums.SlaStatus.PENDING;
            return;
        }
        if (rec.completeTime <= promised) {
            rec.slaStatus = SlaEnums.SlaStatus.MET;
        } else {
            rec.slaStatus = SlaEnums.SlaStatus.OVERTIME;
            long overMs = rec.completeTime - promised;
            rec.overtimeMinutes = (int) Math.ceil(overMs / 60_000.0);
        }
    }

    private void evaluateSpeed(SlaRecord rec, SlaRule rule) {
        rec.promisedTime = 0;
        if (rec.speedTestMbps == null) {
            rec.slaStatus = SlaEnums.SlaStatus.PENDING;
            return;
        }
        if (rec.speedTestMbps >= rule.minSpeedMbps) {
            rec.slaStatus = SlaEnums.SlaStatus.MET;
        } else {
            rec.slaStatus = SlaEnums.SlaStatus.OVERTIME;
            rec.overtimeMinutes = 0;
        }
    }

    private Compensation buildCompensation(SlaRecord rec, SlaRule rule) {
        Compensation c = new Compensation();
        c.slaRecordId = rec.id;
        c.orderId = rec.orderId;
        c.custName = rec.custName;
        c.orderType = rec.orderType;
        c.compType = rule.compType;

        double amt;
        if (rule.compUnit == SlaEnums.CompUnit.PER_OVERTIME_HOUR) {
            int hours = Math.max(1, (int) Math.ceil(rec.overtimeMinutes / 60.0));
            amt = rule.compAmount * hours;
        } else {
            amt = rule.compAmount;
        }
        if (rule.maxCompAmount != null && amt > rule.maxCompAmount) amt = rule.maxCompAmount;
        c.compAmount = amt;

        c.reason = buildReason(rec, rule);
        c.status = SlaEnums.CompStatus.PENDING;
        c.createdTime = System.currentTimeMillis();
        return c;
    }

    private String buildReason(SlaRecord rec, SlaRule rule) {
        if (rule.evalType == SlaEnums.EvalType.SPEED) {
            return String.format("%s 装机测速 %.0fMbps 未达 %.0fMbps，触发赔付",
                    rule.slaName, rec.speedTestMbps, rule.minSpeedMbps);
        }
        return String.format("%s 超时 %d 分钟触发慢必赔", rule.slaName, rec.overtimeMinutes);
    }
}
