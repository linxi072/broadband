package com.broadband.install.controller;

import com.broadband.install.mapper.SlaRuleMapper;
import com.broadband.install.model.SlaRule;
import com.broadband.install.service.SlaServiceApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 装维 SLA 阈值管理（后台）。权限码 {@code sla:config} 与菜单 M151、角色授权三处同源。
 *
 * <p>SLA 规则存于 sla_rule 表，SlaServiceImpl 每次评估实时读取，故阈值变更<b>天然热生效</b>；
 * 本控制器补充「后台可改」的运营入口，使运维无需直连 DB 即可调整宽限/承诺/测速/赔付等阈值。</p>
 */
@RestController
@RequestMapping("/api/admin/sla")
@PreAuthorize("hasAuthority('sla:config')")
public class SlaAdminController {

    @Autowired private SlaServiceApi slaService;
    @Autowired private SlaRuleMapper ruleMapper;

    /** SLA 规则列表（含当前阈值）。 */
    @GetMapping("/rules")
    public List<SlaRule> rules() {
        return slaService.listRules();
    }

    /** 更新单条 SLA 规则阈值（热生效，无需重启）。 */
    @PutMapping("/rule/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        SlaRule r = ruleMapper.selectById(id);
        if (r == null) throw new IllegalArgumentException("SLA 规则不存在：" + id);
        if (req.containsKey("graceMinutes")) r.graceMinutes = intOf(req.get("graceMinutes"), r.graceMinutes);
        if (req.containsKey("promisedHours")) r.promisedHours = intOf(req.get("promisedHours"), r.promisedHours);
        if (req.containsKey("minSpeedMbps")) r.minSpeedMbps = dblOf(req.get("minSpeedMbps"), r.minSpeedMbps);
        if (req.containsKey("compAmount")) r.compAmount = dblOf(req.get("compAmount"), r.compAmount);
        if (req.containsKey("maxCompAmount")) {
            Object v = req.get("maxCompAmount");
            r.maxCompAmount = (v == null) ? null : dblOf(v, r.maxCompAmount == null ? 0 : r.maxCompAmount);
        }
        if (req.containsKey("enabled")) {
            r.enabled = Boolean.parseBoolean(String.valueOf(req.get("enabled")));
        }
        ruleMapper.updateById(r);
        return Map.of("ok", true, "id", id);
    }

    private static int intOf(Object v, int def) {
        if (v == null) return def;
        try { return Integer.parseInt(String.valueOf(v).trim()); } catch (NumberFormatException e) { return def; }
    }

    private static double dblOf(Object v, double def) {
        if (v == null) return def;
        try { return Double.parseDouble(String.valueOf(v).trim()); } catch (NumberFormatException e) { return def; }
    }
}
