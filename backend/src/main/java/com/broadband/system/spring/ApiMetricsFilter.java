package com.broadband.system.spring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接口调用指标采集（内存态，服务重启归零）。
 *
 * <p>为「性能监控」页提供真实数据：按接口统计调用次数、平均耗时、峰值耗时与错误数。
 * 刻意不引入 Micrometer/Prometheus —— 当前阶段只要趋势可见，避免 over-engineering；
 * Phase 4 接入 Prometheus 时可平滑替换本类。</p>
 */
@Component
@Order(1)
public class ApiMetricsFilter extends OncePerRequestFilter {

    /** 每个接口一条统计：{count, totalMs, maxMs, errors} */
    private static final Map<String, long[]> STATS = new ConcurrentHashMap<>();
    private static final AtomicLong TOTAL = new AtomicLong();
    private static final AtomicLong START = new AtomicLong(System.currentTimeMillis());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        long begin = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = Math.max(0, (System.nanoTime() - begin) / 1_000_000);
            String key = request.getMethod() + " " + uri;
            long[] s = STATS.computeIfAbsent(key, k -> new long[4]);
            synchronized (s) {
                s[0]++;
                s[1] += ms;
                s[2] = Math.max(s[2], ms);
                if (response.getStatus() >= 400) s[3]++;
            }
            TOTAL.incrementAndGet();
        }
    }

    /** 全部接口统计（按调用次数倒序）。 */
    public List<Map<String, Object>> endpoints() {
        List<Map<String, Object>> out = new ArrayList<>();
        STATS.forEach((path, s) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("path", path);
            m.put("count", s[0]);
            m.put("avgMs", s[0] == 0 ? 0 : s[1] / s[0]);
            m.put("maxMs", s[2]);
            m.put("errors", s[3]);
            out.add(m);
        });
        out.sort(Comparator.comparingLong((Map<String, Object> m) -> (Long) m.get("count")).reversed());
        return out;
    }

    /** 慢接口 TOP N（按平均耗时至，仅统计被调用过的）。 */
    public List<Map<String, Object>> slowApis(int topN) {
        List<Map<String, Object>> all = endpoints();
        all.sort(Comparator.comparingLong((Map<String, Object> m) -> (Long) m.get("avgMs")).reversed());
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, all.size()); i++) {
            Map<String, Object> src = all.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("api", src.get("path"));
            m.put("calls", src.get("count"));
            m.put("avg", src.get("avgMs"));
            long avg = (Long) src.get("avgMs");
            m.put("status", avg >= 1000 ? "超时" : avg >= 200 ? "偏慢" : "正常");
            out.add(m);
        }
        return out;
    }

    public long totalCalls() {
        return TOTAL.get();
    }

    public long errors() {
        long e = 0;
        for (long[] s : STATS.values()) e += s[3];
        return e;
    }

    /** 自启动以来的采样时长（分钟，至少 1 分钟，避免 QPS 被极小分母放大）。 */
    public double uptimeMinutes() {
        double min = (System.currentTimeMillis() - START.get()) / 60000.0;
        return Math.max(1.0, min);
    }
}
