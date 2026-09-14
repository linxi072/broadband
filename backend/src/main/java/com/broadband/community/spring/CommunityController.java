package com.broadband.community.spring;

import com.broadband.community.model.CommunityCheckResult;
import com.broadband.community.model.CommunityDemand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小区覆盖 REST 接口。
 *  - GET  /api/community/check?name=xxx   查询是否可上门安装（前端下单/查询页复用）
 *  - POST /api/community/demand          登记安装需求（未覆盖小区）
 */
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Autowired
    private CommunityServiceApi communityService;

    @GetMapping("/check")
    public CommunityCheckResult check(@RequestParam String name) {
        return communityService.check(name);
    }

    @PostMapping("/demand")
    public java.util.Map<String, String> demand(@RequestBody CommunityDemand demand) {
        String id = communityService.registerDemand(demand);
        java.util.Map<String, String> resp = new java.util.HashMap<>();
        resp.put("id", id);
        resp.put("message", "需求已登记，覆盖后第一时间通知您");
        return resp;
    }
}
