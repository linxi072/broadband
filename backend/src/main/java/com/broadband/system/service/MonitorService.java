package com.broadband.system.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.broadband.system.mapper.MonitorMapper;
import com.broadband.system.web.ApiMetricsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 性能监控聚合：应用层指标（QPS / RT / 错误率）+ JVM + 数据库连接池 + 节点状态。
 *
 * <p>连接池指标优先读 Druid 真实运行时数据；非 Druid 数据源时回落到保守展示值，
 * 不再硬编码「active 1 / idle 9 / max 10」这类与实际无关的占位数字。</p>
 */
@Service
public class MonitorService {

    @Autowired private MonitorMapper monitorMapper;
    @Autowired private ApiMetricsFilter metrics;
    @Autowired private DataSource dataSource;

    @Value("${server.port:8082}")
    private int serverPort;

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
        double qps = round2(totalCalls / (uptimeMin * 60.0));

        Map<String, Object> app = new LinkedHashMap<>();
        app.put("status", "健康");
        app.put("cpu", cpuPercent());
        app.put("mem", heapPercent());
        app.put("rt", avgRt);
        app.put("errorRate", errorRate);
        app.put("qps", qps);
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
        long roundTripMs = dbRoundTripMs();
        Map<String, Object> db = new LinkedHashMap<>();
        db.put("driver", poolName());
        db.put("queryAvgMs", roundTripMs);
        fillPoolStats(db);
        out.put("db", db);

        out.put("endpoints", endpoints);
        out.put("slowApis", metrics.slowApis(5));

        // ---- 节点（单体部署：应用 + MySQL）
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, Object> self = new LinkedHashMap<>();
        self.put("name", "broadband-backend");
        self.put("addr", "127.0.0.1:" + serverPort);
        self.put("status", "在线");
        self.put("cpu", app.get("cpu"));
        self.put("mem", app.get("mem"));
        self.put("qps", qps);
        self.put("rt", avgRt);
        nodes.add(self);

        Map<String, Object> mysqlNode = new LinkedHashMap<>();
        mysqlNode.put("name", "mysql");
        mysqlNode.put("addr", jdbcHostPort());
        mysqlNode.put("status", "在线");
        mysqlNode.put("cpu", 0);
        mysqlNode.put("mem", 0);
        mysqlNode.put("qps", 0);
        mysqlNode.put("rt", roundTripMs);
        nodes.add(mysqlNode);
        out.put("nodes", nodes);

        return out;
    }

    // ==================================================================== 连接池

    /** 连接池实现名：Druid / HikariCP / 其他类名。 */
    private String poolName() {
        String simple = dataSource.getClass().getSimpleName();
        if (dataSource instanceof DruidDataSource) return "Druid";
        if (simple.contains("Hikari")) return "HikariCP";
        return simple;
    }

    /** 真实连接池水位（Druid 直接读运行时；其他实现尽力而为，取不到则留空值而非编造）。 */
    private void fillPoolStats(Map<String, Object> db) {
        if (dataSource instanceof DruidDataSource ds) {
            db.put("active", ds.getActiveCount());
            db.put("idle", ds.getPoolingCount());
            db.put("total", ds.getActiveCount() + ds.getPoolingCount());
            db.put("max", ds.getMaxActive());
            db.put("waitCount", ds.getWaitThreadCount());
            db.put("maxWaitMs", ds.getMaxWait());
            return;
        }
        try {
            var m = dataSource.getClass().getMethod("getMaximumPoolSize");
            int max = (int) m.invoke(dataSource);
            db.put("max", max);
        } catch (Exception ignored) {
            db.put("max", null);
        }
        db.put("active", null);
        db.put("idle", null);
        db.put("total", null);
    }

    /**
     * 数据库地址（仅 host:port，剥离账号口令）。
     * 取不到时返回 {@code 127.0.0.1:3306}。
     */
    private String jdbcHostPort() {
        String url = null;
        if (dataSource instanceof DruidDataSource ds) {
            url = ds.getUrl();
        }
        if (url == null || url.isBlank()) return "127.0.0.1:3306";
        try {
            String rest = url.substring(url.indexOf("://") + 3);
            int slash = rest.indexOf('/');
            String authority = slash < 0 ? rest : rest.substring(0, slash);
            int at = authority.indexOf('@');
            String hostPort = at < 0 ? authority : authority.substring(at + 1);
            return hostPort.isBlank() ? "127.0.0.1:3306" : hostPort;
        } catch (Exception e) {
            return "127.0.0.1:3306";
        }
    }

    /** 一次真实往返查询耗时（ms）。 */
    private long dbRoundTripMs() {
        long begin = System.nanoTime();
        try {
            monitorMapper.ping();
        } catch (Exception ignored) {
            // ignore
        }
        return Math.max(0, (System.nanoTime() - begin) / 1_000_000);
    }

    // ==================================================================== 主机指标

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

    // ==================================================================== 工具

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
