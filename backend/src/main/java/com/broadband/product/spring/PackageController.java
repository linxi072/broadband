package com.broadband.product.spring;

import com.broadband.product.model.PackageDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 套餐 REST 接口。
 *  - GET /api/package/detail?id=xxx   融合套餐详情（主图/轮播图/组成/动态参数）
 */
@RestController
@RequestMapping("/api/package")
public class PackageController {

    @Autowired
    private PackageServiceApi packageService;

    @GetMapping("/detail")
    public PackageDetailVO detail(@RequestParam(required = false) String id) {
        return packageService.getDetail(id);
    }
}
