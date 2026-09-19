package com.broadband.install.controller;

import com.broadband.install.model.Compensation;
import com.broadband.install.model.SlaBoard;
import com.broadband.install.model.SlaEvaluation;
import com.broadband.install.model.SlaRecord;
import com.broadband.install.model.SlaRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.broadband.install.service.SlaServiceApi;

/**
 * 装维 SLA 与赔付 REST 接口。
 *  - POST /api/sla/evaluate        评估单条工单（完工时调用）
 *  - POST /api/sla/evaluate-batch  批量评估
 *  - GET  /api/sla/board           看板聚合数据
 *  - GET  /api/sla/rules           SLA 规则列表
 *  - GET  /api/sla/compensations   赔付工单列表
 */
@RestController
@RequestMapping("/api/sla")
@PreAuthorize("hasAuthority('sla:view')")
public class SlaController {

    @Autowired
    private SlaServiceApi slaService;

    @PostMapping("/evaluate")
    public SlaEvaluation evaluate(@RequestBody SlaRecord record) {
        return slaService.evaluate(record);
    }

    @PostMapping("/evaluate-batch")
    public List<SlaEvaluation> evaluateBatch(@RequestBody List<SlaRecord> records) {
        return slaService.evaluateBatch(records);
    }

    @GetMapping("/board")
    public SlaBoard board() {
        return slaService.board();
    }

    @GetMapping("/rules")
    public List<SlaRule> rules() {
        return slaService.listRules();
    }

    @GetMapping("/compensations")
    public List<Compensation> compensations() {
        return slaService.listCompensations();
    }
}
