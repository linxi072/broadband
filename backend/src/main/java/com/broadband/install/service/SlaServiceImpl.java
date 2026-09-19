package com.broadband.install.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.broadband.common.Ids;
import com.broadband.install.engine.SlaEngine;
import com.broadband.install.mapper.CompensationMapper;
import com.broadband.install.mapper.SlaRecordMapper;
import com.broadband.install.mapper.SlaRuleMapper;
import com.broadband.install.model.Compensation;
import com.broadband.install.model.SlaBoard;
import com.broadband.install.model.SlaEnums;
import com.broadband.install.model.SlaEvaluation;
import com.broadband.install.model.SlaRecord;
import com.broadband.install.model.SlaRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 装维 SLA 与赔付服务实现。组装 DB 数据 -> 调用核心引擎 engine.SlaEngine -> 落库评估结果/赔付工单。
 * 核心引擎不依赖 Spring，可独立单测。
 */
@Service
public class SlaServiceImpl implements SlaServiceApi {

    @Autowired private SlaRuleMapper ruleMapper;
    @Autowired private SlaRecordMapper recordMapper;
    @Autowired private CompensationMapper compensationMapper;

    private final SlaEngine engine = new SlaEngine();

    @Override
    public SlaEvaluation evaluate(SlaRecord record) {
        if (record.createdTime == 0) record.createdTime = System.currentTimeMillis();
        // 主键必须在评估前就位：引擎生成的 summary 与赔付工单的 slaRecordId 都会引用它
        if (record.id == null || record.id.isEmpty()) record.id = Ids.next();
        SlaRule rule = pickRule(record);
        SlaEvaluation ev = engine.evaluate(record, rule);
        if (ev.skipped) return ev;

        recordMapper.insert(record);
        if (ev.compensation != null) {
            ev.compensation.slaRecordId = record.id;
            if (ev.compensation.id == null || ev.compensation.id.isEmpty()) {
                ev.compensation.id = Ids.next();
            }
            compensationMapper.insert(ev.compensation);
        }
        return ev;
    }

    @Override
    public List<SlaEvaluation> evaluateBatch(List<SlaRecord> records) {
        List<SlaEvaluation> out = new ArrayList<>();
        for (SlaRecord r : records) out.add(evaluate(r));
        return out;
    }

    @Override
    public List<SlaRule> listRules() {
        return ruleMapper.selectList(null);
    }

    @Override
    public List<Compensation> listCompensations() {
        QueryWrapper<Compensation> q = new QueryWrapper<>();
        q.orderByDesc("created_time").last("LIMIT 50");
        return compensationMapper.selectList(q);
    }

    @Override
    public SlaBoard board() {
        SlaBoard board = new SlaBoard();
        List<SlaRecord> all = recordMapper.selectList(null);
        List<Compensation> comps = compensationMapper.selectList(null);

        long evaluated = all.stream()
                .filter(r -> r.slaStatus != SlaEnums.SlaStatus.PENDING).count();
        long met = all.stream()
                .filter(r -> r.slaStatus == SlaEnums.SlaStatus.MET).count();
        board.slaRate = evaluated == 0 ? 0 : Math.round((met * 1000.0 / evaluated)) / 10.0;

        board.sameDayInstalled = all.stream()
                .filter(r -> r.orderType == SlaEnums.OrderType.NEW_INSTALL && r.slaStatus == SlaEnums.SlaStatus.MET)
                .count();
        board.slowPayCount = comps.stream()
                .filter(c -> c.reason != null && c.reason.contains("慢必赔")).count();
        board.outageWorryCount = comps.stream()
                .filter(c -> c.reason != null && c.reason.contains("断网")).count();
        board.monthCompAmount = comps.stream().mapToDouble(c -> c.compAmount).sum();

        board.recentCompensations = comps.stream()
                .sorted((a, b) -> Long.compare(b.createdTime, a.createdTime))
                .limit(10).collect(Collectors.toList());
        return board;
    }

    /** 按业务类型 + 评估方式（速率优先于时限）选取匹配规则。 */
    private SlaRule pickRule(SlaRecord record) {
        List<SlaRule> rules = ruleMapper.selectList(null);
        if (record.speedTestMbps != null) {
            return rules.stream()
                    .filter(r -> r.evalType == SlaEnums.EvalType.SPEED
                            && r.orderType == record.orderType && r.enabled)
                    .findFirst().orElse(null);
        }
        return rules.stream()
                .filter(r -> r.evalType == SlaEnums.EvalType.TIME
                        && r.orderType == record.orderType && r.enabled)
                .findFirst().orElse(null);
    }
}
