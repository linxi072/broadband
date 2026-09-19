package com.broadband.product.service;

import com.broadband.common.Ids;
import com.broadband.common.Values;
import com.broadband.product.mapper.SupportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线客服 / 帮助中心（C 端，V1.14 运营留存）：FAQ 与咨询工单。
 */
@Service
public class SupportService {

    @Autowired private SupportMapper supportMapper;

    /** FAQ 列表，可按 category 过滤。 */
    public List<Map<String, Object>> faq(String category) {
        return supportMapper.selectFaq(category);
    }

    /**
     * 提交咨询工单：{customerId?, type?, content, contact?}。
     * @throws IllegalArgumentException 咨询内容为空
     */
    public Map<String, Object> ticket(Map<String, Object> body) {
        String content = Values.str(body.get("content"));
        if (Values.isBlank(content)) throw new IllegalArgumentException("咨询内容必填");
        String customerId = Values.str(body.get("customerId"));
        String type = Values.str(body.get("type"), "CONSULT");
        String contact = Values.str(body.get("contact"));

        String customerName = "匿名";
        if (customerId != null) {
            String name = supportMapper.selectCustomerName(customerId);
            if (name != null) customerName = name;
        }

        String id = Ids.next();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("customerId", customerId);
        m.put("customerName", customerName);
        m.put("type", type);
        m.put("content", content);
        m.put("contact", contact);
        m.put("status", "PENDING");
        m.put("createdTime", System.currentTimeMillis());
        supportMapper.insertTicket(m);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("ticketId", id);
        resp.put("status", "PENDING");
        return resp;
    }
}
