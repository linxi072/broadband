package com.broadband.product.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * C 端客户订单接口（开放层，供小程序「我的订单」使用）。
 *
 * <p>与 PC 后台的 {@code /api/admin/orders}（需 {@code order:view} 权限）不同，
 * 这里是按 customerId 查询该客户自己的订单，属于面向用户的开放接口。</p>
 */
@RestController
@RequestMapping("/api/order")
public class ClientOrderController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestParam(required = false) String customerId) {
        return jdbc.queryForList("""
                SELECT id, customer_id AS customerId, customer_name AS customerName, phone,
                       package_id AS packageId, package_name AS packageName, amount,
                       community_name AS communityName,
                       order_type AS orderType, status, created_time AS createdTime
                FROM biz_order
                WHERE customer_id = ?
                ORDER BY created_time DESC
                """, (Object) (customerId == null ? "" : customerId));
    }
}
