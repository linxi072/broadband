package com.broadband.community.controller;

import com.broadband.community.service.AdminCommunityService;
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
 * 小区覆盖管理（PC 后台）REST 接口。
 *
 * <p>本类只做权限校验、参数绑定与 HTTP 响应，查询 SQL 与业务校验见 {@link AdminCommunityService}。</p>
 */
@RestController
@RequestMapping("/api/admin/communities")
public class AdminCommunityController {

    @Autowired private AdminCommunityService adminCommunityService;

    @GetMapping
    @PreAuthorize("hasAuthority('community:view')")
    public List<Map<String, Object>> list(@RequestParam(required = false) String keyword) {
        return adminCommunityService.list(keyword);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('community:edit')")
    public Map<String, Object> save(@RequestBody Map<String, Object> req) {
        return adminCommunityService.save(req);
    }
}
