package com.broadband.install.spring;

import com.broadband.install.model.SlaEnums;
import com.broadband.install.model.SlaRecord;
import com.broadband.system.security.WorkerPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 师傅端接口（数据按登录师傅隔离）。
 *
 * <ul>
 *   <li>GET /api/worker/work-orders              —— 当前登录师傅的工单（服务端按 token 取 workerId，
 *       不信任客户端传参，杜绝「改 workerId 看别人工单」）</li>
 *   <li>GET /api/worker/work-orders/{id}         —— 工单详情；师傅只能看自己的，越权返回空</li>
 *   <li>GET /api/worker/summary                  —— 首页统计 + 今日工单</li>
 *   <li>GET /api/worker/capacity                 —— 本人各时段容量占用</li>
 *   <li>GET /api/worker/schedule                 —— 未来 7 天排班（按已派工单聚合）</li>
 *   <li>POST /api/worker/work-orders/{id}/complete —— 完工提交（测速/签名/服务项落库，置 DONE）</li>
 * </ul>
 *
 * <p>后台 ADMIN / OPERATOR 亦可调用，并可用 {@code ?workerId=} 指定查看某位师傅的工单（不传则全部）。</p>
 */
@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private SlaServiceApi slaService;

    private static final String ORDER_SELECT = """
            SELECT w.id,
                   w.customer_name AS customer,
                   w.package_desc  AS pkgDesc,
                   c.name          AS community,
                   w.address, w.time_slot AS timeSlot,
                   COALESCE(k.name, '—') AS worker,
                   w.status, w.worker_id AS workerId, w.cluster_id AS clusterId,
                   w.adjacent_route AS adjacent,
                   w.down_speed AS downSpeed, w.up_speed AS upSpeed,
                   w.sign_name AS signName, w.complete_time AS completeTime
            FROM work_order w
            LEFT JOIN community c ON c.id = w.community_id
            LEFT JOIN worker    k ON k.id = w.worker_id
            """;

    // ---------------------------------------------------------------- 工单

    @GetMapping("/work-orders")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public List<Map<String, Object>> myWorkOrders(@RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String workerId) {
        String scoped = currentWorkerId();
        // 师傅端强制按自身 id 过滤，忽略客户端传的 workerId
        String effective = scoped != null ? scoped : workerId;

        StringBuilder sql = new StringBuilder(ORDER_SELECT).append(" WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (effective != null && !effective.isBlank()) {
            sql.append(" AND w.worker_id = ?");
            args.add(effective);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND w.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY w.time_slot, w.id");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    @GetMapping("/work-orders/{id}")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> myWorkOrder(@PathVariable String id) {
        Map<String, Object> row = queryScoped(id);
        return row == null ? Map.of() : row;
    }

    // ------------------------------------------------------- 首页统计（含今日工单）

    /** 首页：{@code {stats:[{n,l}], today:[{id,cust,addr,pkg,time,statusText}]}}。 */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> summary(@RequestParam(required = false) String date,
                                       @RequestParam(required = false) String workerId) {
        String scoped = currentWorkerId();
        String effective = scoped != null ? scoped : workerId;
        String day = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;

        String prefix = day + "#%";
        List<Object> base = new ArrayList<>();
        StringBuilder where = new StringBuilder(" FROM work_order w WHERE w.time_slot LIKE ? ");
        base.add(prefix);
        if (effective != null && !effective.isBlank()) {
            where.append(" AND w.worker_id = ?");
            base.add(effective);
        }

        int pending = count("SELECT COUNT(*) " + where + " AND w.status IN ('ASSIGNED','INSTALLING')", base);

        // 今日已完成：按完工时间统计（工单时段可能是未来日期，不能按 time_slot 算）
        List<Object> doneArgs = new ArrayList<>();
        StringBuilder doneSql = new StringBuilder(
                "SELECT COUNT(*) FROM work_order w WHERE w.status = 'DONE' AND DATE(w.complete_time) = ? ");
        doneArgs.add(day);
        if (effective != null && !effective.isBlank()) {
            doneSql.append(" AND w.worker_id = ?");
            doneArgs.add(effective);
        }
        int done = count(doneSql.toString(), doneArgs);

        List<Object> totalArgs = new ArrayList<>();
        StringBuilder totalSql = new StringBuilder("SELECT COUNT(*) FROM work_order w WHERE 1 = 1");
        if (effective != null && !effective.isBlank()) {
            totalSql.append(" AND w.worker_id = ?");
            totalArgs.add(effective);
        }
        int total = count(totalSql.toString(), totalArgs);

        List<Map<String, Object>> mine = myWorkOrders(null, workerId);
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
     * 本人某天的时段容量：{@code {date, slots:[{timeSlot,time,used,max,pct}]}}。
     * 容量取自 worker_capacity.adjacent_cap；无配置时回退默认 4（相邻）/2（非相邻）。
     */
    @GetMapping("/capacity")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> myCapacity(@RequestParam(required = false) String date,
                                          @RequestParam(required = false) String workerId) {
        String scoped = currentWorkerId();
        String effective = scoped != null ? scoped : workerId;
        String day = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;

        List<Map<String, Object>> caps = effective == null || effective.isBlank()
                ? List.of()
                : jdbc.queryForList(
                        "SELECT time_slot AS timeSlot, adjacent_cap AS adjacentCap, "
                      + "non_adjacent_cap AS nonAdjacentCap FROM worker_capacity "
                      + "WHERE worker_id = ? AND time_slot LIKE ? ORDER BY time_slot",
                        effective, day + "#%");

        if (caps.isEmpty()) {
            caps = List.of(Map.of("timeSlot", day + "#AM", "adjacentCap", 4, "nonAdjacentCap", 2),
                           Map.of("timeSlot", day + "#PM", "adjacentCap", 4, "nonAdjacentCap", 2));
        }

        List<Object> args = new ArrayList<>();
        StringBuilder usedSql = new StringBuilder(
                "SELECT w.time_slot AS timeSlot, COUNT(*) AS c FROM work_order w WHERE w.time_slot LIKE ? ");
        args.add(day + "#%");
        if (effective != null && !effective.isBlank()) {
            usedSql.append(" AND w.worker_id = ?");
            args.add(effective);
        }
        usedSql.append(" GROUP BY w.time_slot");
        Map<String, Integer> usedMap = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(usedSql.toString(), args.toArray())) {
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

    /** 未来 7 天排班：{@code {days:[{date,day,am,pm,rest,count}]}}，按已派工单聚合。 */
    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> mySchedule(@RequestParam(required = false) String workerId) {
        String scoped = currentWorkerId();
        String effective = scoped != null ? scoped : workerId;
        LocalDate from = LocalDate.now();

        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT w.time_slot AS timeSlot, COUNT(*) AS c FROM work_order w WHERE w.time_slot >= ? ");
        args.add(from.toString());
        if (effective != null && !effective.isBlank()) {
            sql.append(" AND w.worker_id = ?");
            args.add(effective);
        }
        sql.append(" GROUP BY w.time_slot");
        Map<String, Integer> bySlot = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(sql.toString(), args.toArray())) {
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
     * 完工提交：校验归属 → 落测速/签名/服务项 → 置 DONE。
     * 入参 {@code {down, up, sign, services}}，返回 {@code {ok, id, status, completeTime}}。
     */
    @PostMapping("/work-orders/{id}/complete")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> complete(@PathVariable String id,
                                        @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> order = queryScoped(id);
        if (order == null) {
            throw new IllegalArgumentException("工单不存在或无权操作");
        }
        Map<String, Object> b = body == null ? Map.of() : body;

        Integer down = toInt(b.get("down"));
        Integer up = toInt(b.get("up"));
        String sign = b.get("sign") == null ? null : String.valueOf(b.get("sign"));
        String services = b.get("services") == null ? null : String.valueOf(b.get("services"));

        jdbc.update("UPDATE work_order SET status = 'DONE', down_speed = ?, up_speed = ?, "
                + "sign_name = ?, service_items = ?, complete_time = NOW() WHERE id = ?",
                down, up, sign, services, id);

        // 业务闭环：完工自动触发 SLA 评估（时限类 + 速率类），并同步业务订单为 DONE
        evaluateSlaOnComplete(id);
        String bizId = jdbc.queryForObject("SELECT biz_order_id FROM work_order WHERE id = ?", String.class, id);
        if (bizId != null) {
            jdbc.update("UPDATE biz_order SET status = 'DONE' WHERE id = ?", bizId);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("status", "DONE");
        resp.put("downSpeed", down);
        resp.put("upSpeed", up);
        resp.put("signName", sign);
        resp.put("completeTime", jdbc.queryForObject(
                "SELECT complete_time FROM work_order WHERE id = ?", String.class, id));
        return resp;
    }

    /**
     * 开始施工：ASSIGNED -> INSTALLING，并同步业务订单状态。
     * 入参无；返回 {@code {ok, id, status}}。
     */
    @PostMapping("/work-orders/{id}/start")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN','OPERATOR')")
    public Map<String, Object> start(@PathVariable String id) {
        Map<String, Object> order = queryScoped(id);
        if (order == null) {
            throw new IllegalArgumentException("工单不存在或无权操作");
        }
        if (!"ASSIGNED".equals(order.get("status"))) {
            throw new IllegalStateException("仅「待上门(ASSIGNED)」工单可开始施工，当前：" + order.get("status"));
        }
        jdbc.update("UPDATE work_order SET status = 'INSTALLING' WHERE id = ?", id);
        String bizId = jdbc.queryForObject("SELECT biz_order_id FROM work_order WHERE id = ?", String.class, id);
        if (bizId != null) {
            jdbc.update("UPDATE biz_order SET status = 'INSTALLING' WHERE id = ?", bizId);
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
        Map<String, Object> wo = one("SELECT biz_order_id, customer_name, time_slot, complete_time "
                + "FROM work_order WHERE id = ?", workOrderId);
        if (wo == null) return;
        String bizId = (String) wo.get("biz_order_id");
        if (isBlank(bizId)) return;
        Map<String, Object> bo = one("SELECT order_type, created_time FROM biz_order WHERE id = ?", bizId);
        if (bo == null) return;

        SlaEnums.OrderType orderType = parseOrderType(String.valueOf(bo.get("order_type")));
        long acceptTime = ((Number) bo.get("created_time")).longValue();
        long appointed = parseSlot(String.valueOf(wo.get("time_slot")));
        long completeTime = wo.get("complete_time") instanceof java.sql.Timestamp ts
                ? ts.getTime() : System.currentTimeMillis();
        String custName = String.valueOf(wo.get("customer_name"));
        Integer down = jdbc.queryForObject("SELECT down_speed FROM work_order WHERE id = ?", Integer.class, workOrderId);

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

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> l = jdbc.queryForList(sql, args);
        return l.isEmpty() ? null : l.get(0);
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
            java.time.LocalDate d = java.time.LocalDate.parse(p[0]);
            int hour = switch (p[1]) {
                case "AM" -> 12;
                case "PM" -> 18;
                default -> 12;
            };
            return d.atTime(hour, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- 内部工具

    private Map<String, Object> queryScoped(String id) {
        String scoped = currentWorkerId();
        StringBuilder sql = new StringBuilder(ORDER_SELECT).append(" WHERE w.id = ? ");
        List<Object> args = new ArrayList<>();
        args.add(id);
        if (scoped != null) {
            sql.append(" AND w.worker_id = ?");   // 师傅越权查看直接查不到
            args.add(scoped);
        }
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private int count(String sql, List<Object> args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args.toArray());
        return n == null ? 0 : n;
    }

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

    /** 当前登录师傅 id；后台用户（LoginUser）返回 null，表示不限范围。 */
    private String currentWorkerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        return p instanceof WorkerPrincipal wp ? wp.id : null;
    }
}
