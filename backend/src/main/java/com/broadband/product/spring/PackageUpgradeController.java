package com.broadband.product.spring;

import com.broadband.product.engine.PackageUpgradeEngine;
import com.broadband.product.model.PackageUpgradePreview;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/package")
public class PackageUpgradeController {

    /** 升档选项 + 补差预览（mock 数据，实际应从套餐/客户表组装） */
    @GetMapping("/upgrade-options")
    public Map<String, Object> upgradeOptions(@RequestParam String customerId) {
        Map<String, Object> resp = new HashMap<>();
        Map<String, Object> current = new HashMap<>();
        current.put("name", "500M 融合");
        current.put("fee", 99);
        current.put("contractLeftMonths", 18);
        resp.put("current", current);

        List<Map<String, Object>> options = new ArrayList<>();
        options.add(opt("b500", "bandwidth", "维持 500M", 0));
        options.add(opt("b1000", "bandwidth", "1000M", 40));
        options.add(opt("b2000", "bandwidth", "2000M", 90));
        options.add(opt("fttr", "addon", "FTTR", 30));
        options.add(opt("wifi", "addon", "全屋WiFi", 15));
        options.add(opt("watch", "addon", "看家", 10));
        resp.put("options", options);
        return resp;
    }

    /** 提交升档（mock：返回补差预览，实际应落 package_upgrade_order 并触发生效策略） */
    @PostMapping("/upgrade")
    public PackageUpgradePreview upgrade(@RequestBody Map<String, Object> req) {
        int currentFee = 99;
        int targetExtra = ((Number) req.getOrDefault("targetExtra", 0)).intValue();
        int addonExtra = ((Number) req.getOrDefault("addonExtra", 0)).intValue();
        int leftDays = 18 * 30;
        return PackageUpgradeEngine.compute(currentFee, targetExtra, addonExtra, leftDays);
    }

    private Map<String, Object> opt(String key, String type, String name, int fee) {
        Map<String, Object> m = new HashMap<>();
        m.put("key", key);
        m.put("type", type);
        m.put("name", name);
        m.put("extraFee", fee);
        return m;
    }
}
