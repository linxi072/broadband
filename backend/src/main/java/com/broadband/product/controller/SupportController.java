package com.broadband.product.controller;

import com.broadband.product.service.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 在线客服 / 帮助中心（C 端小程序，V1.14 运营留存）。
 *
 * <ul>
 *   <li>GET  /api/support/faq     —— FAQ 列表（可按 category 过滤）</li>
 *   <li>POST /api/support/ticket  —— 提交工单式咨询</li>
 * </ul>
 *
 * <p>本类只做参数绑定与 HTTP 响应，业务逻辑见 {@link SupportService}。</p>
 */
@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired private SupportService supportService;

    @GetMapping("/faq")
    public List<Map<String, Object>> faq(@RequestParam(required = false) String category) {
        return supportService.faq(category);
    }

    /** 提交咨询工单：{customerId?, type?, content, contact?}。 */
    @PostMapping("/ticket")
    public Map<String, Object> ticket(@RequestBody Map<String, Object> body) {
        return supportService.ticket(body);
    }
}
