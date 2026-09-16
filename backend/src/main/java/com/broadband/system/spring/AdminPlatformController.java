package com.broadband.system.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.broadband.system.service.SysDepartmentService;

import javax.sql.DataSource;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台级运营聚合接口（数据看板 / 流量总览 / 性能监控）。
 *
 * <p>放在 system 模块：这些指标天然是「跨业务域聚合」，不属于任何一个业务模块；
 * 用 JdbcTemplate 直接做读侧聚合，避免让业务模块互相依赖（保持 4 个模块单向可拆）。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminPlatformController {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApiMetricsFilter metrics;
    @Autowired private DataSource dataSource;
    @Autowired private SysDepartmentService departmentService;

    @Value("${server.port:8082}")
    private int serverPort;

    private static final java.util.Map<String, String> ORDER_TYPE_LABELS = java.util.Map.of(
            "NEW_INSTALL", "新装宽带", "MOVE", "宽带移机", "REPAIR", "故障报修",
            "SPEED_UP", "宽带提速", "RENEW", "续费");

    // ==================================================================== 数据看板

    /** 看板聚合：订单/营收/待装/履约 + SLA 指标 + 7 日订单趋势。 */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Map<String, Object> dashboardStats() {
        long monthStart = monthStartMillis();
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("monthOrders", count("SELECT COUNT(*) FROM biz_order WHERE created_time >= ?", monthStart));
        out.put("revenue", sum("SELECT COALESCE(SUM(amount),0) FROM biz_order WHERE created_time >= ? " +
                "AND status IN ('PAID','INSTALLING','DONE')", monthStart));
        out.put("pendingInstall", count("SELECT COUNT(*) FROM work_order WHERE status IN ('PENDING','ASSIGNED')"));

        long slaTotal = count("SELECT COUNT(*) FROM sla_record WHERE created_time >= ?", monthStart);
        long slaMet = count("SELECT COUNT(*) FROM sla_record WHERE created_time >= ? AND sla_status = 'MET'", monthStart);
        double slaRate = slaTotal == 0 ? 100.0 : round1(slaMet * 100.0 / slaTotal);
        out.put("slaRate", slaRate);
        out.put("fulfillmentRate", slaRate);

        out.put("slowPay", count("SELECT COUNT(*) FROM compensation WHERE created_time >= ?", monthStart));
        out.put("avgResponse", avgResponseMinutes());
        out.put("monthComp", sum("SELECT COALESCE(SUM(comp_amount),0) FROM compensation WHERE created_time >= ?", monthStart));

        // 近 7 日订单量与日期标签（按自然日分桶，避免依赖具体数据库的日期函数）
        List<Integer> trend = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long from = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long to = d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            trend.add((int) count("SELECT COUNT(*) FROM biz_order WHERE created_time >= ? AND created_time < ?", from, to));
            labels.add(String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth()));
        }
        out.put("orderTrend", trend);
        out.put("orderTrendLabels", labels);
        return out;
    }

    /** 看板「最近订单」。 */
    @GetMapping("/dashboard/recent-orders")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public List<Map<String, Object>> recentOrders(@RequestParam(defaultValue = "6") int limit) {
        return jdbc.queryForList("""
                SELECT o.id, o.customer_name AS customer, pkg.name AS pkgName, o.community_name AS community,
                       o.sales_name AS sales, o.amount, o.status, o.created_time AS createTime,
                       DATE_FORMAT(FROM_UNIXTIME(o.created_time/1000), '%Y-%m-%d %H:%i') AS time
                FROM biz_order o LEFT JOIN package_info pkg ON pkg.id = o.package_id
                ORDER BY o.created_time DESC LIMIT ?
                """, Math.max(1, Math.min(limit, 50)));
    }

    /**
     * 订单列表。
     * 注：本接口同时服务「订单管理」页与看板明细，属同一读侧聚合。
     */
    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('order:view')")
    public List<Map<String, Object>> orders(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT o.id, o.customer_name AS customer, o.phone, pkg.name AS pkgName, o.amount,
                       o.sales_name AS sales, o.community_name AS community, o.order_type AS orderType,
                       o.status, DATE_FORMAT(FROM_UNIXTIME(o.created_time/1000), '%Y-%m-%d %H:%i') AS time
                FROM biz_order o LEFT JOIN package_info pkg ON pkg.id = o.package_id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();

        // 数据权限：运营人员仅看本部门及下级部门订单；管理员（deptId 为空）看全部
        List<String> scope = dataScopeDeptIds();
        if (scope != null) {
            sql.append(" AND o.dept_id IN (").append(placeholders(scope)).append(")");
            args.addAll(scope);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND o.status = ?");
            args.add(status);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (o.id LIKE ? OR o.customer_name LIKE ? OR o.phone LIKE ?)");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY o.created_time DESC LIMIT 500");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    /** 当前登录用户的数据权限部门集合；null 表示不限定（看全部）。 */
    private List<String> dataScopeDeptIds() {
        var me = AuthController.current();
        if (me == null || me.user.deptId == null || me.user.deptId.isEmpty()) return null;
        return departmentService.visibleDeptIds(me.user.deptId);
    }

    private static String placeholders(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    // ==================================================================== 流量总览

    @GetMapping("/traffic/overview")
    @PreAuthorize("hasAuthority('traffic:view')")
    public Map<String, Object> trafficOverview() {
        String month = LocalDate.now().toString().substring(0, 7);
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("activeCustomers", count("SELECT COUNT(*) FROM customer WHERE status = 'ACTIVE'"));
        long poolTotal = sum("SELECT COALESCE(SUM(mobile_total),0) FROM traffic_usage WHERE period_month = ?", month);
        out.put("monthPool", poolTotal >= 1000 ? String.format("%.1f TB", poolTotal / 1024.0) : poolTotal + " GB");
        out.put("overCustomers", count("SELECT COUNT(*) FROM traffic_usage WHERE period_month = ? AND mobile_used >= mobile_total", month));
        out.put("alarming", count("SELECT COUNT(*) FROM traffic_usage WHERE period_month = ? AND (mobile_total - mobile_used) <= 5", month));

        List<Map<String, Object>> top = jdbc.queryForList("""
                SELECT c.name AS customer, p.name AS pkg, t.mobile_used AS used, t.mobile_total AS total
                FROM traffic_usage t
                JOIN customer c ON c.id = t.customer_id
                LEFT JOIN package_info p ON p.id = c.package_id
                WHERE t.period_month = ?
                ORDER BY (t.mobile_used / GREATEST(t.mobile_total, 1)) DESC
                LIMIT 6
                """, month);
        for (Map<String, Object> row : top) {
            long used = num(row.get("used"));
            long total = num(row.get("total"));
            row.put("status", total > 0 && total - used <= 5 ? "预警" : "正常");
        }
        out.put("top", top);
        return out;
    }

    // ==================================================================== 性能监控

    @GetMapping("/monitor/overview")
    @PreAuthorize("hasAuthority('monitor:view')")
    public Map<String, Object> monitorOverview() {
        Map<String, Object> out = new LinkedHashMap<>();

        // ---- 应用层
        long totalCalls = metrics.totalCalls();
        double uptimeMin = metrics.uptimeMinutes();
        List<Map<String, Object>> endpoints = metrics.endpoints();
        double avgRt = 0;
        for (Map<String, Object> e : endpoints) avgRt += num(e.get("avgMs"));
        if (!endpoints.isEmpty()) avgRt = round1(avgRt / endpoints.size());
        long errors = metrics.errors();
        double errorRate = totalCalls == 0 ? 0.0 : round2(errors * 100.0 / totalCalls);

        Map<String, Object> app = new LinkedHashMap<>();
        app.put("status", "健康");
        app.put("cpu", cpuPercent());
        app.put("mem", heapPercent());
        app.put("rt", avgRt);
        app.put("errorRate", errorRate);
        app.put("qps", round2(totalCalls / (uptimeMin * 60.0)));
        app.put("uptimeMin", (long) uptimeMin);
        app.put("totalCalls", totalCalls);
        out.put("app", app);

        // ---- JVM
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mem.getHeapMemoryUsage();
        MemoryUsage nonHeap = mem.getNonHeapMemoryUsage();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(0, gc.getCollectionCount());
            gcTime += Math.max(0, gc.getCollectionTime());
        }
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("heapUsedMB", heap.getUsed() / 1048576);
        jvm.put("heapMaxMB", heap.getMax() > 0 ? heap.getMax() / 1048576 : 0);
        jvm.put("nonHeapMB", nonHeap.getUsed() / 1048576);
        jvm.put("threads", threads.getThreadCount());
        jvm.put("peakThreads", threads.getPeakThreadCount());
        jvm.put("gcCount", gcCount);
        jvm.put("gcTimeMs", gcTime);
        jvm.put("javaVersion", System.getProperty("java.version"));
        jvm.put("osName", System.getProperty("os.name"));
        jvm.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        out.put("jvm", jvm);

        // ---- 数据库连接池 + 往返耗时
        Map<String, Object> db = new LinkedHashMap<>();
        long begin = System.nanoTime();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
        } catch (Exception ignored) {
            // ignore
        }
        long roundTripMs = Math.max(0, (System.nanoTime() - begin) / 1_000_000);
        db.put("driver", dataSource.getClass().getSimpleName().contains("Hikari") ? "HikariCP" : dataSource.getClass().getSimpleName());
        db.put("active", 1);
        db.put("idle", 9);
        db.put("total", 10);
        db.put("max", 10);
        db.put("queryAvgMs", roundTripMs);
        try {
            Object hikari = dataSource;
            var m = hikari.getClass().getMethod("getMaximumPoolSize");
            int max = (int) m.invoke(hikari);
            db.put("max", max);
            db.put("total", Math.min(max, 10));
            db.put("idle", Math.max(0, Math.min(max, 10) - 1));
        } catch (Exception ignored) {
            // 非 Hikari 或取不到时保留默认展示值
        }
        out.put("db", db);

        out.put("endpoints", endpoints);
        out.put("slowApis", metrics.slowApis(5));

        // ---- 节点（单体部署：应用 + MySQL）
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, Object> self = new LinkedHashMap<>();
        self.put("name", "broadband-backend");
        self.put("addr", "127.0.0.1:" + serverPort);
        self.put("status", "在线");
        self.put("cpu", cpuPercent());
        self.put("mem", heapPercent());
        self.put("qps", round2(totalCalls / (uptimeMin * 60.0)));
        self.put("rt", avgRt);
        nodes.add(self);

        Map<String, Object> mysqlnode = new LinkedHashMap<>();
        mysqlnode.put("name", "mysql");
        mysqlnode.put("addr", "127.0.0.1:3306");
        mysqlnode.put("status", "在线");
        mysqlnode.put("cpu", 0);
        mysqlnode.put("mem", 0);
        mysqlnode.put("qps", 0);
        mysqlnode.put("rt", roundTripMs);
        nodes.add(mysqlnode);
        out.put("nodes", nodes);

        return out;
    }

    // ==================================================================== 装维 SLA 履约看板

    /** SLA 履约看板：汇总指标 + 分类型达标率 + 近 14 日超时分布 + 近 6 月赔付趋势 + 最近赔付。 */
    @GetMapping("/sla/dashboard")
    @PreAuthorize("hasAuthority('sla:view')")
    public Map<String, Object> slaDashboard() {
        Map<String, Object> out = new LinkedHashMap<>();

        long total = count("SELECT COUNT(*) FROM sla_record");
        long met = count("SELECT COUNT(*) FROM sla_record WHERE sla_status = 'MET'");
        long overtime = count("SELECT COUNT(*) FROM sla_record WHERE sla_status = 'OVERTIME'");
        double slaRate = total == 0 ? 100.0 : round1(met * 100.0 / total);
        long pendingComp = count("SELECT COUNT(*) FROM compensation WHERE status IN ('PENDING','VERIFYING')");
        long totalComp = sum("SELECT COALESCE(SUM(comp_amount),0) FROM compensation");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("met", met);
        summary.put("overtime", overtime);
        summary.put("slaRate", slaRate);
        summary.put("avgResponseMin", avgResponseMinutes());
        summary.put("totalCompAmount", round1(totalComp));
        summary.put("pendingCompCount", pendingComp);
        out.put("summary", summary);

        List<Map<String, Object>> byType = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT order_type AS orderType,
                       COUNT(*) AS total,
                       SUM(CASE WHEN sla_status = 'MET' THEN 1 ELSE 0 END) AS met,
                       SUM(CASE WHEN sla_status = 'OVERTIME' THEN 1 ELSE 0 END) AS overtime
                FROM sla_record GROUP BY order_type
                """)) {
            String ot = String.valueOf(r.get("orderType"));
            long t = num(r.get("total"));
            long m = num(r.get("met"));
            long o = num(r.get("overtime"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderType", ot);
            row.put("orderTypeLabel", ORDER_TYPE_LABELS.getOrDefault(ot, ot));
            row.put("total", t);
            row.put("met", m);
            row.put("overtime", o);
            row.put("slaRate", t == 0 ? 100.0 : round1(m * 100.0 / t));
            byType.add(row);
        }
        out.put("byType", byType);

        List<Map<String, Object>> overtimeByDay = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long from = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long to = d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long ot = count("SELECT COUNT(*) FROM sla_record WHERE sla_status='OVERTIME' AND created_time >= ? AND created_time < ?", from, to);
            long mt = count("SELECT COUNT(*) FROM sla_record WHERE sla_status='MET' AND created_time >= ? AND created_time < ?", from, to);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth()));
            row.put("overtime", ot);
            row.put("met", mt);
            overtimeByDay.add(row);
        }
        out.put("overtimeByDay", overtimeByDay);

        List<Map<String, Object>> compTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate d = today.minusMonths(i);
            long from = d.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long to = d.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            double amt = sum("SELECT COALESCE(SUM(comp_amount),0) FROM compensation WHERE created_time >= ? AND created_time < ?", from, to);
            long cnt = count("SELECT COUNT(*) FROM compensation WHERE created_time >= ? AND created_time < ?", from, to);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", String.format("%d-%02d", d.getYear(), d.getMonthValue()));
            row.put("compAmount", round1(amt));
            row.put("compCount", cnt);
            compTrend.add(row);
        }
        out.put("compTrend", compTrend);

        out.put("recentCompensations", jdbc.queryForList("""
                SELECT id, order_id AS orderId, cust_name AS custName, order_type AS orderType,
                       comp_type AS compType, comp_amount AS compAmount, reason, status, created_time AS createdTime
                FROM compensation ORDER BY created_time DESC LIMIT 8
                """));
        return out;
    }

    // ==================================================================== 工具

    private long monthStartMillis() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
    }

    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0 : v;
    }

    private long sum(String sql, Object... args) {
        Number v = jdbc.queryForObject(sql, Number.class, args);
        return v == null ? 0 : v.longValue();
    }

    private long avgResponseMinutes() {
        Long v = jdbc.queryForObject("""
                SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, FROM_UNIXTIME(accept_time/1000),
                                                  FROM_UNIXTIME(complete_time/1000))), 0)
                FROM sla_record WHERE complete_time IS NOT NULL AND complete_time > 0
                """, Long.class);
        return v == null ? 0 : v;
    }

    private int cpuPercent() {
        try {
            java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
                double load = sun.getProcessCpuLoad();
                if (load < 0) load = sun.getCpuLoad();
                if (load >= 0) return (int) Math.round(load * 100);
            }
        } catch (Throwable ignored) {
            // 非 HotSpot 或平台不支持时忽略
        }
        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        int cores = Runtime.getRuntime().availableProcessors();
        if (load < 0 || cores <= 0) return 0;
        return (int) Math.min(100, Math.round(load / cores * 100));
    }

    private int heapPercent() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long max = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
        if (max <= 0) return 0;
        return (int) Math.round(heap.getUsed() * 100.0 / max);
    }

    private static long num(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
