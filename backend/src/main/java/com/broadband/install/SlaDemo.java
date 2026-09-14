package com.broadband.install;

import com.broadband.install.engine.SlaEngine;
import com.broadband.install.engine.SlaRulePresets;
import com.broadband.install.model.SlaEnums;
import com.broadband.install.model.SlaEvaluation;
import com.broadband.install.model.SlaRecord;
import com.broadband.install.model.SlaRule;

import java.util.ArrayList;
import java.util.List;

/**
 * SLA 引擎可独立运行验证（无 Spring/DB 依赖）。
 * 运行：javac 编译后 java com.broadband.install.SlaDemo
 */
public class SlaDemo {
    public static void main(String[] args) {
        SlaEngine engine = new SlaEngine();
        List<SlaRule> rules = SlaRulePresets.defaults();

        long now = System.currentTimeMillis();
        final long H = 3_600_000L;

        List<SlaRecord> records = new ArrayList<>();

        // 场景 A：新装-当日装，受理 8h 前、1h 前完工（24h 内）=> 达标
        SlaRecord a = new SlaRecord();
        a.id = "SR-A"; a.orderId = "WO-1001"; a.custName = "陈先生";
        a.orderType = SlaEnums.OrderType.NEW_INSTALL;
        a.acceptTime = now - 8 * H; a.appointedTime = now - 7 * H; a.completeTime = now - 1 * H;
        records.add(a);

        // 场景 B：报修-当日修，受理 38h 前、10h 前完工（超 24h）=> 超时 28h => 赔付封顶 20 元
        SlaRecord b = new SlaRecord();
        b.id = "SR-B"; b.orderId = "WO-1002"; b.custName = "李女士";
        b.orderType = SlaEnums.OrderType.REPAIR;
        b.acceptTime = now - 38 * H; b.appointedTime = now - 37 * H; b.completeTime = now - 10 * H;
        records.add(b);

        // 场景 C：新装-网速不达标，测速 320 < 500 => 触发赔付 20 元（每单固定）
        SlaRecord c = new SlaRecord();
        c.id = "SR-C"; c.orderId = "WO-1003"; c.custName = "王先生";
        c.orderType = SlaEnums.OrderType.NEW_INSTALL;
        c.acceptTime = now - 5 * H; c.speedTestMbps = 320.0;
        records.add(c);

        // 场景 D：移机，受理 30h 前、6h 前完工（24h 内）=> 达标（无匹配规则 -> 跳过）
        SlaRecord d = new SlaRecord();
        d.id = "SR-D"; d.orderId = "WO-1004"; d.custName = "赵先生";
        d.orderType = SlaEnums.OrderType.MOVE;
        d.acceptTime = now - 30 * H; d.appointedTime = now - 29 * H; d.completeTime = now - 6 * H;
        records.add(d);

        System.out.println("===== 装维 SLA 与慢必赔规则引擎 验证 =====\n");
        int triggered = 0;
        for (SlaRecord r : records) {
            // 按业务类型取规则（演示：取首条匹配；速率类单独用 R-SPEED-500）
            SlaRule rule;
            if (r.speedTestMbps != null) {
                rule = rules.stream().filter(x -> x.evalType == SlaEnums.EvalType.SPEED).findFirst().orElse(null);
            } else {
                rule = SlaRulePresets.byOrderType(rules, r.orderType);
            }
            SlaEvaluation ev = engine.evaluate(r, rule);
            System.out.println(ev.summary);
            if (ev.compensation != null) triggered++;
            if (ev.compensation != null) {
                System.out.println("   -> 赔付工单: 类型=" + ev.compensation.compType.label
                        + " 金额=" + ev.compensation.compAmount + " 状态=" + ev.compensation.status.label);
            }
        }

        System.out.println("\n===== 验证结论 =====");
        System.out.println("场景 A 应达标、B 应超时赔付(封顶20)、C 应网速不达标赔付(20)、D 应跳过(无规则)");
        System.out.println("触发赔付工单数 = " + triggered + "（预期 2：B 与 C）");
    }
}
