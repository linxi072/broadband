package com.broadband.product.service;

import com.broadband.common.Values;
import com.broadband.product.mapper.RepairMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 故障报修（C 端，V1.13 全流程）：提交报修 → 生成报修工单(REPAIR) → 派单 → 上门 → 完工 → SLA 评估。
 *
 * <p>与「新装」不同，报修无需支付，提交即受理（biz_order 直接置 PAID 表示已受理），
 * 并同步生成 type=REPAIR 的安装工单进入派单池（被派单算法按小区相邻性统一调度）。</p>
 */
@Service
public class RepairService {

    @Autowired private RepairMapper repairMapper;

    /**
     * 提交报修。入参 {customerId, communityId?, faultCategory, faultDesc, contactPhone?, timeSlot?}。
     * @return {ok, repairId, orderId, status, categoryLabel}
     */
    public Map<String, Object> create(Map<String, Object> body) {
        String customerId = Values.str(body.get("customerId"));
        String faultCategory = Values.str(body.get("faultCategory"));
        String faultDesc = Values.str(body.get("faultDesc"));
        if (Values.isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        if (Values.isBlank(faultCategory)) throw new IllegalArgumentException("faultCategory 必填（故障类型）");
        if (Values.isBlank(faultDesc)) throw new IllegalArgumentException("faultDesc 必填（故障描述）");

        Map<String, Object> cust = repairMapper.selectCustomer(customerId);
        if (cust == null) throw new IllegalArgumentException("客户不存在：" + customerId);

        String communityId = Values.str(body.get("communityId"));
        if (Values.isBlank(communityId)) communityId = Values.str(cust.get("community_id"));
        if (Values.isBlank(communityId)) throw new IllegalArgumentException("无法确定报修小区（客户未绑定小区且未传 communityId）");

        Map<String, Object> com = repairMapper.selectCommunity(communityId);
        if (com == null) throw new IllegalArgumentException("小区不存在：" + communityId);
        String communityName = String.valueOf(com.get("name"));
        String deptId = Values.str(com.get("dept_id"));

        String customerName = Values.str(cust.get("name"), "客户");
        String contactPhone = Values.str(body.get("contactPhone"), Values.str(cust.get("phone")));

        // 故障类型字典标签（用于工单描述，便于师傅一眼看懂）
        String categoryLabel = faultLabel(faultCategory);

        // 预约时段：优先取客户端指定，否则默认次日 AM
        String timeSlot = Values.str(body.get("timeSlot"));
        if (Values.isBlank(timeSlot)) timeSlot = LocalDate.now().plusDays(1).toString() + "#AM";

        String orderId = "B" + System.currentTimeMillis();
        long now = System.currentTimeMillis();
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("id", orderId);
        biz.put("customerId", customerId);
        biz.put("customerName", customerName);
        biz.put("phone", contactPhone);
        biz.put("packageId", null);
        biz.put("packageName", "故障报修");
        biz.put("amount", 0);
        biz.put("communityId", communityId);
        biz.put("communityName", communityName);
        biz.put("orderType", "REPAIR");
        biz.put("status", "PAID");
        biz.put("deptId", deptId);
        biz.put("createdTime", now);
        repairMapper.insertBizOrder(biz);

        String workOrderId = "WO" + System.currentTimeMillis();
        Map<String, Object> wo = new LinkedHashMap<>();
        wo.put("id", workOrderId);
        wo.put("communityId", communityId);
        wo.put("address", communityName);
        wo.put("timeSlot", timeSlot);
        wo.put("customerName", customerName);
        wo.put("packageDesc", "故障报修-" + categoryLabel);
        wo.put("status", "PENDING");
        wo.put("type", "REPAIR");
        wo.put("faultCategory", faultCategory);
        wo.put("faultDesc", faultDesc);
        wo.put("contactPhone", contactPhone);
        wo.put("bizOrderId", orderId);
        wo.put("deptId", deptId);
        repairMapper.insertWorkOrder(wo);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("repairId", workOrderId);
        resp.put("orderId", orderId);
        resp.put("status", "PENDING");
        resp.put("categoryLabel", categoryLabel);
        return resp;
    }

    /** 我的报修列表（按客户查其 REPAIR 业务订单关联的工单）。 */
    public List<Map<String, Object>> my(String customerId) {
        if (Values.isBlank(customerId)) return List.of();
        return repairMapper.selectMy(customerId).stream().map(this::enrich).toList();
    }

    /** 报修详情（含 SLA 评估与进度时间线）。 */
    public Map<String, Object> detail(String id) {
        List<Map<String, Object>> rows = repairMapper.selectById(id);
        if (rows.isEmpty()) return Map.of();
        Map<String, Object> wo = enrich(rows.get(0));

        List<Map<String, Object>> sla = repairMapper.selectSla(id);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("workOrder", wo);
        resp.put("slaRecords", sla);
        resp.put("timeline", buildTimeline(wo));
        return resp;
    }

    /** 撤销报修：仅 PENDING（未派单）可撤，同步置 biz_order 为 CANCELLED。 */
    public Map<String, Object> cancel(String id) {
        Map<String, Object> wo = repairMapper.selectWorkOrderStatus(id);
        if (wo == null) throw new IllegalArgumentException("报修工单不存在：" + id);
        if (!"PENDING".equals(wo.get("status"))) {
            throw new IllegalStateException("仅「待派单」状态可撤销，当前：" + wo.get("status"));
        }
        repairMapper.updateWorkOrderStatus(id, "CANCELLED");
        String bizId = Values.str(wo.get("biz_order_id"));
        if (bizId != null) repairMapper.updateBizOrderStatus(bizId, "CANCELLED");

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("status", "CANCELLED");
        return resp;
    }

    // ---------------------------------------------------------------- 辅助

    private Map<String, Object> enrich(Map<String, Object> r) {
        Map<String, Object> m = new LinkedHashMap<>(r);
        m.put("statusText", statusText(String.valueOf(r.get("status"))));
        m.put("progress", progress(String.valueOf(r.get("status"))));
        m.put("timeSlotText", prettySlot(Values.str(r.get("timeSlot"))));
        m.put("faultCategoryText", faultLabel(Values.str(r.get("faultCategory"))));
        return m;
    }

    /** 故障类型值 -> 字典标签（如 NETWORK_DOWN -> 网络中断）。 */
    private String faultLabel(String value) {
        if (value == null) return "";
        String label = repairMapper.selectFaultLabel(value);
        return label == null ? value : label;
    }

    /** 进度步骤（用于小程序进度条）：0 待派单 / 1 已派单 / 2 维修中 / 3 已完成。 */
    private int progress(String status) {
        return switch (status) {
            case "PENDING" -> 0;
            case "ASSIGNED" -> 1;
            case "INSTALLING" -> 2;
            case "DONE" -> 3;
            case "CANCELLED" -> -1;
            default -> 0;
        };
    }

    private List<Map<String, Object>> buildTimeline(Map<String, Object> wo) {
        String status = String.valueOf(wo.get("status")).equals("null") ? "" : String.valueOf(wo.get("status"));
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step("已提交报修", true, "客户提交故障报修申请"));
        steps.add(step("已派单", !"PENDING".equals(status),
                "ASSIGNED".equals(status) || "INSTALLING".equals(status) || "DONE".equals(status)
                        ? "师傅已接单：" + wo.get("worker") : "等待调度派单"));
        steps.add(step("维修中", "INSTALLING".equals(status) || "DONE".equals(status),
                "INSTALLING".equals(status) ? "师傅正在上门维修" : ("DONE".equals(status) ? "维修已完成" : "尚未开始")));
        steps.add(step("已完成", "DONE".equals(status), "DONE".equals(status) ? "服务已完成，欢迎评价" : "待完成"));
        return steps;
    }

    private Map<String, Object> step(String title, boolean done, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("done", done);
        m.put("desc", desc);
        return m;
    }

    private static String statusText(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "PENDING" -> "待派单";
            case "ASSIGNED" -> "已派单";
            case "INSTALLING" -> "维修中";
            case "DONE" -> "已完成";
            case "CANCELLED" -> "已撤销";
            default -> status;
        };
    }

    private static String prettySlot(String slot) {
        if (slot == null) return "—";
        String[] p = slot.split("#");
        if (p.length < 2) return slot;
        String half = switch (p[1]) {
            case "AM" -> "上午";
            case "PM" -> "下午";
            default -> p[1];
        };
        return p[0] + " " + half;
    }
}
