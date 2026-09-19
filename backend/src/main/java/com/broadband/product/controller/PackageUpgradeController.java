package com.broadband.product.controller;

import com.broadband.product.model.PackageUpgradePreview;
import com.broadband.product.service.PackageUpgradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 套餐升级 REST 接口（真实数据库版，已去除 mock）。
 *
 * <ul>
 *   <li>GET  /api/package/upgrade-options?customerId=xx
 *       —— 当前套餐 + 剩余合约月数 + 可升档/加购选项</li>
 *   <li>POST /api/package/upgrade
 *       —— 计算补差预览，并落 package_upgrade_order（PC 后台「套餐升级管理」数据源）</li>
 * </ul>
 *
 * <p>本类只做参数绑定与 HTTP 响应，业务与数据访问见 {@link PackageUpgradeService}。</p>
 */
@RestController
@RequestMapping("/api/package")
public class PackageUpgradeController {

    @Autowired private PackageUpgradeService packageUpgradeService;

    /** 升档选项 + 当前套餐信息（从客户/合约/套餐参数组装）。 */
    @GetMapping("/upgrade-options")
    public Map<String, Object> upgradeOptions(@RequestParam String customerId) {
        return packageUpgradeService.upgradeOptions(customerId);
    }

    /** 提交升档：解析选项加价 -> 补差预览 -> 落升级申请单。 */
    @PostMapping("/upgrade")
    public PackageUpgradePreview upgrade(@RequestBody Map<String, Object> req) {
        return packageUpgradeService.upgrade(req);
    }
}
