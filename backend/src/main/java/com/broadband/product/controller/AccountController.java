package com.broadband.product.controller;

import com.broadband.product.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 账户与账单中心（C 端小程序，V1.14 运营留存）。
 *
 * <ul>
 *   <li>GET /api/account/summary —— 账户概览：积分余额 / 本月消费 / 合约到期 / 客户分层</li>
 *   <li>GET /api/account/bills   —— 账单明细：按账期聚合业务订单</li>
 * </ul>
 *
 * <p>本类只做参数绑定与 HTTP 响应，业务逻辑见 {@link AccountService}。</p>
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired private AccountService accountService;

    /** 账户概览。入参 customerId。 */
    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam String customerId) {
        return accountService.summary(customerId);
    }

    /** 账单明细。入参 customerId、可选 period（yyyy-MM 过滤账期）。 */
    @GetMapping("/bills")
    public List<Map<String, Object>> bills(@RequestParam String customerId,
                                           @RequestParam(required = false) String period) {
        return accountService.bills(customerId, period);
    }
}
