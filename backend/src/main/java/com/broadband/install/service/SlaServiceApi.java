package com.broadband.install.service;

import com.broadband.install.model.Compensation;
import com.broadband.install.model.SlaBoard;
import com.broadband.install.model.SlaEvaluation;
import com.broadband.install.model.SlaRecord;
import com.broadband.install.model.SlaRule;

import java.util.List;

/**
 * 装维 SLA 与赔付服务接口。
 */
public interface SlaServiceApi {

    /** 评估单条工单：按业务类型/评估方式匹配规则 -> 计算 SLA -> 超时则生成赔付工单，并落库。 */
    SlaEvaluation evaluate(SlaRecord record);

    /** 批量评估。 */
    List<SlaEvaluation> evaluateBatch(List<SlaRecord> records);

    /** 查询全部 SLA 规则。 */
    List<SlaRule> listRules();

    /** 查询赔付工单列表。 */
    List<Compensation> listCompensations();

    /** 装维 SLA 看板聚合数据（对应 PC /sla 看板）。 */
    SlaBoard board();
}
