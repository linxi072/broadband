package com.broadband.product.controller;

import com.broadband.product.service.IntelligenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据智能（V1.15 迭代二 · 数据深化）：客户分群 / 流失预警 / 营销自动化。
 *
 * <p>端点受 {@code intelligence:view} 权限保护（菜单 M150 已同源授权）。
 * 本类只做权限校验与参数绑定，聚合逻辑见 {@link IntelligenceService}。</p>
 *
 * <ul>
 *   <li>客户分群：按「订单量 / 合约到期 / 近 180 天活跃度 / 客户等级」派生 6 类分群并汇总计数。</li>
 *   <li>流失预警：导出高危客户（流失风险 + 临期待续约）含风险分与归因，按风险分倒序。</li>
 *   <li>营销自动化：规则引擎（mkt_campaign）按触发分群对客户自动发券 / 推送活动，全程留痕去重。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    @Autowired private IntelligenceService intelligenceService;

    // ============================================================ 客户分群

    @GetMapping("/segments")
    @PreAuthorize("hasAuthority('intelligence:view')")
    public Map<String, Object> segments() {
        return intelligenceService.segments();
    }

    // ============================================================ 流失预警

    @GetMapping("/churn")
    @PreAuthorize("hasAuthority('intelligence:view')")
    public Map<String, Object> churn(@RequestParam(required = false, defaultValue = "50") Integer limit) {
        return intelligenceService.churn(limit == null ? 50 : limit);
    }

    // ============================================================ 分群客户下钻（US-2.2）

    /** 分群客户明细下钻：点击分群饼图 / 流失柱图某分群，返回该分群下客户列表。 */
    @GetMapping("/segment-customers")
    @PreAuthorize("hasAuthority('intelligence:view')")
    public Map<String, Object> segmentCustomers(@RequestParam String segment,
                                                @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return intelligenceService.segmentCustomers(segment, limit == null ? 50 : limit);
    }

    // ============================================================ 营销自动化规则

    @GetMapping("/campaigns")
    @PreAuthorize("hasAuthority('intelligence:view')")
    public List<Map<String, Object>> campaigns() {
        return intelligenceService.campaigns();
    }

    /** 营销自动化一键触发：遍历启用规则，对命中分群客户执行动作（发券 / 推送），30 天内已执行过的去重。 */
    @PostMapping("/auto-trigger")
    @PreAuthorize("hasAuthority('intelligence:view')")
    public Map<String, Object> autoTrigger(@RequestBody(required = false) Map<String, Object> body) {
        return intelligenceService.autoTrigger(body != null && Boolean.TRUE.equals(body.get("dryRun")));
    }
}
