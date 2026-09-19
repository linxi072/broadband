package com.broadband.install.service;

import com.broadband.install.mapper.WorkerQueryMapper;
import com.broadband.install.model.SlaEnums;
import com.broadband.install.model.SlaRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 师傅端业务逻辑（数据按登录师傅隔离）。
 *
 * <p>Controller 只负责从 {@code SecurityContext} 解析登录主体、绑定参数与返回 HTTP 响应；
 * 本类承载全部查询编排、状态流转与 SLA 评估；数据访问通过 {@link WorkerQueryMapper}
 * （SQL 集中在 WorkerQueryMapper.xml）。</p>
 *
 * <p>「师傅只能看自己的工单」的隔离由 {@code workerId} 参数体现：师傅端传入登录师傅 id
 * （忽略客户端传参），后台 ADMIN / OPERATOR 传 {@code null} 表示不限。</p>
 */
@Service
public class WorkerService {

    @Autowired private WorkerQueryMapper workerQueryMapper;
    @Autowired private SlaServiceApi slaService;

    // ---------------------------------------------------------------- 工单

    /**
     * 工单列表。
     * @param workerId 生效的师傅 id（师傅端为登录师傅，后台可为 null 表示全部）
     * @param status   可选状态过滤
     */
    public List<Map<String, Object>> workOrders(String workerId, String status) {
        return workerQueryMapper.selectOrders(workerId, status);
    }

    /**
     * 工单详情；师傅只看自己的，越权返回 {@code null}。
     * @param workerId 登录师傅 id（后台为 null 表示不限）
     */
    public Map<String, Object> workOrder(String id, String workerId) {
        return workerQueryMapper.selectOrderById(id, workerId);
    }

    // ------------------------------------------------------- 首页统计（含今日工单）

    /** 首页：{@code {date, stats:[{n,l}], today:[{id,cust,addr,pkg,time,statusText}]}}。 */
    public Map<String, Object> summary(String date, String workerId) {
        String day = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;

        int pending = workerQueryMapper.countPending(day + "#%", workerId);
        int done = workerQueryMapper.countDoneOnDate(day, workerId);
        int total = workerQueryMapper.countTotal(workerId);

        List<Map<String, Object>> mine = workOrders(workerId, null);
        List<Map<String, Object>> listed = mine.stream()
                .filter(r -> String.valueOf(r.get("timeSlot")).startsWith(day))
                .toList();
        // 当日无排单时回退展示「待上门」，避免师傅端首页空白
        if (listed.isEmpty()) {
            listed = mine.stream()
                    .filter(r -> "ASSIGNED".equals(r.get("status")) || "INSTALLING".equals(r.get("status")))
                    .toList();
        }

        List<Map<String, Object>> today = listed.stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>(r);
                    m.put("cust", r.get("customer"));
                    m.put("pkg", r.get("pkgDesc"));
                    m.put("addr", String.valueOf(r.getOrDefault("community", "")) + " "
                            + r.getOrDefault("address", ""));
                    m.put("time", prettySlot(String.valueOf(r.get("timeSlot"))));
                    m.put("statusText", statusText(String.valueOf(r.get("status"))));
                    return m;
                })
                .toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("date", day);
        resp.put("stats", List.of(
                Map.of("n", pending, "l", "待上门"),
                Map.of("n", done, "l", "今日已完成"),
                Map.of("n", total, "l", "累计工单")));
        resp.put("today", today);
        return resp;
    }

    // ---------------------------------------------------------------- 容量

    /**
     * 某天的时段容量：{@code {date, slots:[{timeSlot,time,used,max,pct}]}}。
     * 容量取自 worker_capacity.adjacent_cap；无配置时回退默认 4（相邻）/2（非相邻）。
     */
    public Map<String, Object> capacity(String date, String workerId) {
        String day = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;

        List<Map<String, Object>> caps = (workerId == null || workerId.isBlank())
                ? List.of()
                : workerQueryMapper.selectCapacity(workerId, day + "#%");

        if (caps.isEmpty()) {
            caps = List.of(Map.of("timeSlot", day + "#AM", "adjacentCap", 4, "nonAdjacentCap", 2),
                           Map.of("timeSlot", day + "#PM", "adjacentCap", 4, "nonAdjacentCap", 2));
        }

        Map<String, Integer> usedMap = new LinkedHashMap<>();
        for (Map<String, Object> r : workerQueryMapper.selectUsedBySlot(day + "#%", workerId)) {
            usedMap.put(String.valueOf(r.get("timeSlot")), ((Number) r.get("c")).intValue());
        }

        List<Map<String, Object>> slots = new ArrayList<>();
        for (Map<String, Object> cap : caps) {
            String slot = String.valueOf(cap.get("timeSlot"));
            int max = cap.get("adjacentCap") == null ? 4 : ((Number) cap.get("adjacentCap")).intValue();
            int used = usedMap.getOrDefault(slot, 0);
            int pct = max <= 0 ? 0 : Math.min(100, used * 100 / max);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("timeSlot", slot);
            m.put("time", prettySlot(slot));
            m.put("used", used);
            m.put("max", max);
            m.put("pct", pct);
            slots.add(m);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("date", day);
        resp.put("slots", slots);
        return resp;
    }

    // ---------------------------------------------------------------- 排班

    /** 未来 7 天排班：{@code {from, shifts:[{date,day,am,pm,rest,count}]}}，按已派工单聚合。 */
    public Map<String, Object> schedule(String workerId) {
        LocalDate from = LocalDate.now();

        Map<String, Integer> bySlot = new LinkedHashMap<>();
        for (Map<String, Object> r : workerQueryMapper.selectSchedule(from.toString(), workerId)) {
            bySlot.put(String.valueOf(r.get("timeSlot")), ((Number) r.get("c")).intValue());
        }

        String[] week = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = from.plusDays(i);
            String key = d.toString();
            int am = bySlot.getOrDefault(key + "#AM", 0);
            int pm = bySlot.getOrDefault(key + "#PM", 0);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", key);
            m.put("day", (i == 0 ? "今天" : week[d.getDayOfWeek().getValue() % 7]));
            m.put("am", am > 0 ? "上午 " + am + " 单" : "—");
            m.put("pm", pm > 0 ? "下午 " + pm + " 单" : "—");
            m.put("rest", am == 0 && pm == 0);
            m.put("count", am + pm);
            days.add(m);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("from", from.toString());
        resp.put("shifts", days);
        return resp;
    }

    // ---------------------------------------------------------------- 完工

    /**
     * 完工提交：校验归属 → 落测速/签名/服务项 → 置 DONE → 触发 SLA 评估 → 同步业务订单 → 生成待评价。
     * 入参 {@code {down, up, sign, services}}，返回 {@code {ok, id, status, ...}}。
     * @throws IllegalArgumentException 工单不存在或无权操作
     */
    public Map<String, Object> complete(String id, String workerId, Map<String, Object> body) {
        Map<String, Object> order = workOrder(id, workerId);
        if (order == null) {
            throw new IllegalArgumentException("工单不存在或无权操作");
        }
        Map<String, Object> b = body == null ? Map.of() : body;

        Integer down = toInt(b.get("down"));
        Integer up = toInt(b.get("up"));
        String sign = b.get("sign") == null ? null : String.valueOf(b.get("sign"));
        String services = b.get("services") == null ? null : String.valueOf(b.get("services"));

        workerQueryMapper.updateComplete(id, down, up, sign, services);

        // 业务闭环：完工自动触发 SLA 评估（时限类 + 速率类），并同步业务订单为 DONE
        evaluateSlaOnComplete(id);
        String bizId = workerQueryMapper.selectBizOrderId(id);
        if (bizId != null) {
            workerQueryMapper.updateBizOrderStatus(bizId, "DONE");
        }

        // 评价闭环增强：完工后自动生成「服务评价」推送（TO_EVALUATE），引导客户对本次服务评分。
        if (bizId != null) {
            String existing = workerQueryMapper.selectReviewId(bizId);
            if (existing == null) {
                String workerName = order.get("worker") == null ? null : String.valueOf(order.get("worker"));
                String custName = order.get("customer") == null ? null : String.valueOf(order.get("customer"));
                String reviewId = "RV" + System.currentTimeMillis();
                workerQueryMapper.insertReview(reviewId, bizId, custName, workerName, System.currentTimeMillis());
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("status", "DONE");
        resp.put("downSpeed", down);
        resp.put("upSpeed", up);
        resp.put("signName", sign);
        resp.put("completeTime", workerQueryMapper.selectCompleteTime(id));
        return resp;
    }

    /**
     * 开始施工：ASSIGNED -> INSTALLING，并同步业务订单状态。
     * @throws IllegalArgumentException 工单不存在或无权操作
     * @throws IllegalStateException    当前状态不是 ASSIGNED
     */
    public Map<String, Object> start(String id, String workerId) {
        Map<String, Object> order = workOrder(id, workerId);
        if (order == null) {
            throw new IllegalArgumentException("工单不存在或无权操作");
        }
        if (!"ASSIGNED".equals(order.get("status"))) {
            throw new IllegalStateException("仅「待上门(ASSIGNED)」工单可开始施工，当前：" + order.get("status"));
        }
        workerQueryMapper.updateStart(id);
        String bizId = workerQueryMapper.selectBizOrderId(id);
        if (bizId != null) {
            workerQueryMapper.updateBizOrderStatus(bizId, "INSTALLING");
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("status", "INSTALLING");
        return resp;
    }

    // ---------------------------------------------------------------- SLA 自动评估

    /**
     * 完工时按工单生成 SLA 评估记录：
     * <ul>
     *   <li>时限类：受理时间（业务订单创建时间）+ 承诺小时 - 宽限 → 与完工时间比，判超时（慢必赔）；</li>
     *   <li>速率类：装机测速下行速率 ≥ 规则最低速率 → 达标，否则触发赔付。</li>
     * </ul>
     * 评估结果由 {@link SlaServiceApi#evaluate(SlaRecord)} 落库 sla_record / compensation。
     */
    private void evaluateSlaOnComplete(String workOrderId) {
        Map<String, Object> wo = workerQueryMapper.selectWorkOrderForSla(workOrderId);
        if (wo == null) return;
        String bizId = (String) wo.get("biz_order_id");
        if (isBlank(bizId)) return;
        Map<String, Object> bo = workerQueryMapper.selectBizOrder(bizId);
        if (bo == null) return;

        SlaEnums.OrderType orderType = parseOrderType(String.valueOf(bo.get("order_type")));
        long acceptTime = ((Number) bo.get("created_time")).longValue();
        long appointed = parseSlot(String.valueOf(wo.get("time_slot")));
        long completeTime = wo.get("complete_time") instanceof Timestamp ts
                ? ts.getTime() : System.currentTimeMillis();
        String custName = String.valueOf(wo.get("customer_name"));
        Integer down = workerQueryMapper.selectDownSpeed(workOrderId);

        SlaRecord t = new SlaRecord();
        t.orderId = workOrderId;
        t.orderType = orderType;
        t.custName = custName;
        t.acceptTime = acceptTime;
        t.appointedTime = appointed;
        t.completeTime = completeTime;
        slaService.evaluate(t);

        if (down != null) {
            SlaRecord s = new SlaRecord();
            s.orderId = workOrderId;
            s.orderType = orderType;
            s.custName = custName;
            s.acceptTime = acceptTime;
            s.appointedTime = appointed;
            s.completeTime = completeTime;
            s.speedTestMbps = down.doubleValue();
            slaService.evaluate(s);
        }
    }

    // ---------------------------------------------------------------- 内部工具

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static SlaEnums.OrderType parseOrderType(String s) {
        try {
            return SlaEnums.OrderType.valueOf(s);
        } catch (Exception e) {
            return SlaEnums.OrderType.NEW_INSTALL;
        }
    }

    /** {@code 2026-09-15#AM -> 当天 12:00 的毫秒时间戳}（PM -> 18:00）。 */
    private static long parseSlot(String slot) {
        if (slot == null) return 0;
        String[] p = slot.split("#");
        if (p.length < 2) return 0;
        try {
            LocalDate d = LocalDate.parse(p[0]);
            int hour = switch (p[1]) {
                case "AM" -> 12;
                case "PM" -> 18;
                default -> 12;
            };
            return d.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String statusText(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "PENDING" -> "待派单";
            case "ASSIGNED" -> "待上门";
            case "INSTALLING" -> "安装中";
            case "DONE" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** {@code 2026-09-15#AM -> 2026-09-15 上午} */
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
