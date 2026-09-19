package com.broadband.system.service;

import com.broadband.common.RedisCacheService;
import com.broadband.system.mapper.SlaDashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 装维 SLA 履约看板服务（业务逻辑层）。数据访问通过 {@link SlaDashboardMapper}（SQL 在 XML）。
 *
 * <p>承载原 {@code AdminPlatformController} 中的 SLA 看板聚合与热力下钻逻辑：
 * 汇总指标、分业务类型达标率、近 14 日超时分布、近 6 月赔付趋势、赔付按类型分布、
 * 超时热力矩阵、最近赔付记录，以及热力单元格下钻明细。</p>
 */
@Service
public class SlaDashboardService {

    /** 业务类型 -> 中文名（看板展示用）。 */
    private static final Map<String, String> ORDER_TYPE_LABELS = Map.of(
            "NEW_INSTALL", "新装宽带",
            "MOVE", "宽带移机",
            "REPAIR", "故障报修",
            "SPEED_UP", "宽带提速",
            "RENEW", "续费",
            "ADDON", "加购");

    @Autowired private SlaDashboardMapper slaDashboardMapper;
    @Autowired private RedisCacheService cache;

    /**
     * SLA 履约看板：汇总指标 + 分类型达标率 + 近 14 日超时分布 + 近 6 月赔付趋势
     * + 赔付按类型分布 + 超时热力 + 最近赔付（短 TTL 缓存，DB 直查退化解）。
     */
    public Map<String, Object> slaDashboard() {
        return cache.get("sla:dash", Duration.ofSeconds(60), this::computeSlaDashboard);
    }

    /** SLA 看板聚合计算（缓存未命中时执行）。 */
    private Map<String, Object> computeSlaDashboard() {
        Map<String, Object> out = new LinkedHashMap<>();

        long total = z(slaDashboardMapper.countSlaTotal());
        long met = z(slaDashboardMapper.countSlaMet());
        long overtime = z(slaDashboardMapper.countSlaOvertime());
        double slaRate = total == 0 ? 100.0 : round1(met * 100.0 / total);
        long pendingComp = z(slaDashboardMapper.countPendingComp());
        long totalComp = z(slaDashboardMapper.sumCompTotal());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("met", met);
        summary.put("overtime", overtime);
        summary.put("slaRate", slaRate);
        summary.put("avgResponseMin", z(slaDashboardMapper.avgResponseMinutes()));
        summary.put("totalCompAmount", round1(totalComp));
        summary.put("pendingCompCount", pendingComp);
        out.put("summary", summary);

        // ---- 分业务类型达标率 ----
        List<Map<String, Object>> byType = new ArrayList<>();
        for (Map<String, Object> r : slaDashboardMapper.byType()) {
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

        // ---- 近 14 日超时 / 达标分布 ----
        List<Map<String, Object>> overtimeByDay = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long from = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long to = d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long ot = z(slaDashboardMapper.countSlaStatusInRange("OVERTIME", from, to));
            long mt = z(slaDashboardMapper.countSlaStatusInRange("MET", from, to));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth()));
            row.put("overtime", ot);
            row.put("met", mt);
            overtimeByDay.add(row);
        }
        out.put("overtimeByDay", overtimeByDay);

        // ---- 近 6 月赔付趋势 ----
        List<Map<String, Object>> compTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate d = today.minusMonths(i);
            long from = d.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long to = d.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            double amt = round1(z(slaDashboardMapper.sumCompInRange(from, to)));
            long cnt = z(slaDashboardMapper.countCompInRange(from, to));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", String.format("%d-%02d", d.getYear(), d.getMonthValue()));
            row.put("compAmount", amt);
            row.put("compCount", cnt);
            compTrend.add(row);
        }
        out.put("compTrend", compTrend);

        // ---- 赔付按业务类型分布（定位高赔付来源）----
        List<Map<String, Object>> compByType = new ArrayList<>();
        for (Map<String, Object> r : slaDashboardMapper.compByType()) {
            String ot = String.valueOf(r.get("orderType"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderType", ot);
            row.put("orderTypeLabel", ORDER_TYPE_LABELS.getOrDefault(ot, ot));
            row.put("count", num(r.get("cnt")));
            row.put("amount", round1(((Number) r.get("amount")).doubleValue()));
            compByType.add(row);
        }
        out.put("compByType", compByType);

        out.put("recentCompensations", slaDashboardMapper.recentCompensations());

        // ---- 超时热力：按 星期 × 小时 统计 OVERTIME 工单分布 ----
        int[][] matrix = new int[7][24]; // 行=周一~周日，列=0~23 时
        for (Map<String, Object> r : slaDashboardMapper.heatmapRows()) {
            Object ts = r.get("ts");
            long ms = ts instanceof Number ? ((Number) ts).longValue() : 0;
            if (ms <= 0) continue;
            LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
            matrix[ldt.getDayOfWeek().getValue() - 1][ldt.getHour()]++;
        }
        List<List<Integer>> values = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            List<Integer> row = new ArrayList<>();
            for (int h = 0; h < 24; h++) row.add(matrix[d][h]);
            values.add(row);
        }
        Map<String, Object> heatmap = new LinkedHashMap<>();
        heatmap.put("days", List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
        heatmap.put("hours", IntStream.range(0, 24).boxed().toList());
        heatmap.put("values", values);
        out.put("heatmap", heatmap);

        return out;
    }

    /**
     * SLA 超时热力下钻：返回指定「星期 × 时段」单元的超时工单明细。
     * 前端点击热力图单元格时调用（dayOfWeek 1..7 周一为 1；hour 0..23）。
     */
    public List<Map<String, Object>> slaOvertimeDetail(int dayOfWeek, int hour) {
        List<Map<String, Object>> rows = slaDashboardMapper.overtimeDetail();
        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Object ts = r.get("completeTime");
            if (ts == null || ((Number) ts).longValue() <= 0) ts = r.get("createdTime");
            long ms = ts instanceof Number ? ((Number) ts).longValue() : 0;
            if (ms <= 0) continue;
            LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
            if (ldt.getDayOfWeek().getValue() == dayOfWeek && ldt.getHour() == hour) {
                r.put("orderTypeLabel", ORDER_TYPE_LABELS.getOrDefault(String.valueOf(r.get("orderType")), String.valueOf(r.get("orderType"))));
                matched.add(r);
            }
        }
        return matched;
    }

    // ==================================================================== 私有工具

    private static long z(Long v) {
        return v == null ? 0 : v;
    }

    private static long num(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
