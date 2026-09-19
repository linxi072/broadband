package com.broadband.product.service;

import com.broadband.common.RedisCacheService;
import com.broadband.common.Values;
import com.broadband.product.mapper.CustomerAdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户列表与客户 360 全景（PC 后台读侧聚合）。
 *
 * <p>Controller 只负责权限注解、参数绑定与 HTTP 响应；全部查询与派生计算（生命周期阶段、
 * 触达时间线、分群判定）收敛在本类，数据访问通过 {@link CustomerAdminMapper}（SQL 在 XML）。</p>
 *
 * <p>生命周期分群复用 CustomerAdminMapper.xml 中的共享 CTE 片段，与「数据智能」模块口径一致。</p>
 */
@Service
public class CustomerAdminService {

    @Autowired private CustomerAdminMapper customerAdminMapper;
    @Autowired private RedisCacheService cache;

    // ==================================================================== 客户

    /** 客户列表（含套餐 / 合约 / 小区 / 标签计算列）。 */
    public List<Map<String, Object>> customers(String keyword) {
        List<Map<String, Object>> rows = customerAdminMapper.selectCustomers(keyword);
        for (Map<String, Object> row : rows) {
            row.put("tags", Values.splitTags(Values.str(row.get("tagStr"))));
            row.remove("tagStr");
        }
        return rows;
    }

    // ==================================================================== 客户 360 视图

    /**
     * 客户 360 全景：档案 + 业务订单 + 合约 + 流量 + 投诉评价 + 安装工单 + 升级申请 + 汇总指标。
     * 单一人口、服务端按 customerId 聚合，避免前端多接口拼装与越权风险。
     */
    public Map<String, Object> customer360(String id) {
        return cache.get("cust:360:" + id, Duration.ofSeconds(30), () -> load360(id));
    }

    /** 客户 360 聚合计算（缓存未命中时执行）。 */
    private Map<String, Object> load360(String id) {
        Map<String, Object> result = new LinkedHashMap<>();

        // ---- 档案 ----
        Map<String, Object> profile = customerAdminMapper.selectProfile(id);
        if (profile != null) {
            profile.put("tags", Values.splitTags(Values.str(profile.get("tagStr"))));
            profile.remove("tagStr");
        }
        result.put("profile", profile);

        if (profile == null) {
            result.put("orders", List.of());
            result.put("contracts", List.of());
            result.put("traffic", null);
            result.put("reviews", List.of());
            result.put("workOrders", List.of());
            result.put("upgradeOrders", List.of());
            result.put("summary", Map.of());
            result.put("lifecycle", Map.of());
            result.put("touchRecords", List.of());
            return result;
        }

        String name = Values.str(profile.get("name"));

        // ---- 业务订单 ----
        result.put("orders", customerAdminMapper.selectOrders(id));

        // ---- 合约 ----
        result.put("contracts", customerAdminMapper.selectContracts(id));

        // ---- 流量（取最近一个周期） ----
        Map<String, Object> traffic = customerAdminMapper.selectTraffic(id);
        if (traffic != null) {
            String trend = Values.str(traffic.get("dailyTrend"));
            List<Integer> arr = new ArrayList<>();
            if (trend != null) for (String s : trend.split(",")) {
                try { arr.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
            }
            traffic.put("dailyTrend", arr);
        }
        result.put("traffic", traffic);

        // ---- 投诉与评价 ----
        result.put("reviews", customerAdminMapper.selectReviews(name));

        // ---- 安装工单（经 biz_order 关联 或 按客户姓名） ----
        result.put("workOrders", customerAdminMapper.selectWorkOrders(id, name));

        // ---- 升级申请单 ----
        result.put("upgradeOrders", customerAdminMapper.selectUpgradeOrders(id));

        // ---- 汇总指标 ----
        Map<String, Object> summary = customerAdminMapper.selectSummary(id, name);
        if (summary == null) summary = Map.of();
        result.put("summary", summary);

        // ---- 生命周期阶段（统一分群口径）----
        result.put("lifecycle",
                computeLifecycle(id, profile, summary,
                        (List<Map<String, Object>>) result.get("orders"),
                        (List<Map<String, Object>>) result.get("reviews"),
                        (List<Map<String, Object>>) result.get("workOrders"),
                        (List<Map<String, Object>>) result.get("upgradeOrders"),
                        (List<Map<String, Object>>) result.get("contracts")));

        // ---- 触达 / 互动记录时间线 ----
        result.put("touchRecords",
                buildTouchRecords(profile,
                        (List<Map<String, Object>>) result.get("orders"),
                        (List<Map<String, Object>>) result.get("reviews"),
                        (List<Map<String, Object>>) result.get("workOrders"),
                        (List<Map<String, Object>>) result.get("upgradeOrders"),
                        (List<Map<String, Object>>) result.get("contracts")));

        return result;
    }

    // ==================================================================== 客户 360 派生

    private Map<String, Object> computeLifecycle(String id, Map<String, Object> profile, Map<String, Object> summary,
            List<Map<String, Object>> orders, List<Map<String, Object>> reviews,
            List<Map<String, Object>> workOrders, List<Map<String, Object>> upgradeOrders,
            List<Map<String, Object>> contracts) {
        Map<String, Object> lc = new LinkedHashMap<>();
        // 统一分群口径：复用 selectCustomerSegment（与数据智能模块一致）
        Map<String, Object> seg = customerSegment(id);
        String stage = Values.str(seg.get("segment"));
        long lastActive = seg.get("lastActive") == null ? 0 : ((Number) seg.get("lastActive")).longValue();
        long contractDaysLeft = seg.get("contractDaysLeft") == null ? 9999 : ((Number) seg.get("contractDaysLeft")).longValue();
        long daysSince = lastActive == 0 ? 9999 : (System.currentTimeMillis() - lastActive) / 86_400_000L;

        lc.put("segment", stage);
        lc.put("stage", stage);
        lc.put("stageLabel", segLabel(stage));
        lc.put("color", segColor(stage));
        lc.put("daysSince", daysSince);
        lc.put("lastActive", lastActive);
        lc.put("churnRisk", "CHURN_RISK".equals(stage));
        lc.put("riskScore", segRisk(stage, daysSince, contractDaysLeft));

        List<String> reasons = new ArrayList<>();
        if (lastActive > 0) reasons.add("最近互动：" + fmtTs(lastActive));
        reasons.addAll(segReasons(stage, daysSince, contractDaysLeft));
        lc.put("reasons", reasons);
        return lc;
    }

    /** 单客户分群判定：与数据智能模块共用口径，确保两页一致。 */
    private Map<String, Object> customerSegment(String id) {
        Map<String, Object> seg = customerAdminMapper.selectCustomerSegment(id);
        return seg == null ? new LinkedHashMap<>(Map.of("segment", "LEAD")) : seg;
    }

    private List<Map<String, Object>> buildTouchRecords(Map<String, Object> profile,
            List<Map<String, Object>> orders, List<Map<String, Object>> reviews,
            List<Map<String, Object>> workOrders, List<Map<String, Object>> upgradeOrders,
            List<Map<String, Object>> contracts) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> o : orders) {
            list.add(touch("order", "下单 · " + Values.str(o.get("packageName")),
                    moneyStr(o.get("amount")) + " · " + Values.str(o.get("statusLabel")),
                    Values.str(o.get("createdAt"))));
        }
        for (Map<String, Object> r : reviews) {
            String type = "投诉".equals(Values.str(r.get("typeLabel"))) ? "complaint" : "review";
            list.add(touch(type, Values.str(r.get("typeLabel")) + " · " + scoreStr(r.get("score")) + "★",
                    Values.str(r.get("content")), Values.str(r.get("createdAt"))));
        }
        for (Map<String, Object> w : workOrders) {
            String ct = Values.str(w.get("completeTime"));
            if (ct == null) continue;
            list.add(touch("install", "安装完工 · " + Values.str(w.get("packageDesc")),
                    "师傅 " + Values.str(w.get("workerId")), ct));
        }
        for (Map<String, Object> u : upgradeOrders) {
            list.add(touch("upgrade", "升级申请 · " + Values.str(u.get("targetBandKey")),
                    Values.str(u.get("statusLabel")), Values.str(u.get("createdAt"))));
        }
        for (Map<String, Object> c : contracts) {
            list.add(touch("contract", "签约合约",
                    moneyStr(c.get("monthlyFee")) + " · 到期 " + Values.str(c.get("endDate")),
                    Values.str(c.get("startDate"))));
        }
        list.sort((a, b) -> Long.compare(tsOf(b), tsOf(a)));
        if (list.size() > 30) list = new ArrayList<>(list.subList(0, 30));
        return list;
    }

    private Map<String, Object> touch(String type, String title, String detail, String timeText) {
        long t = parseTs(timeText);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("title", title);
        m.put("detail", detail);
        m.put("time", t);
        m.put("timeText", timeText);
        return m;
    }

    private long tsOf(Map<String, Object> m) {
        Object t = m.get("time");
        return t instanceof Number n ? n.longValue() : 0;
    }

    // ==================================================================== 分群通用口径

    static String segLabel(String seg) {
        return Map.of("LEAD", "潜在客户", "RENEW", "临期待续约", "CHURN_RISK", "流失预警",
                "HIGH_VALUE", "高价值活跃", "GROWING", "成长期", "STABLE", "稳定期")
                .getOrDefault(seg, seg);
    }

    static String segColor(String seg) {
        return Map.of("LEAD", "info", "GROWING", "success", "STABLE", "success",
                "HIGH_VALUE", "danger", "RENEW", "warning", "CHURN_RISK", "danger")
                .getOrDefault(seg, "info");
    }

    static int segRisk(String seg, long daysSince, long contractDaysLeft) {
        if ("CHURN_RISK".equals(seg)) return (int) Math.min(99, 70 + Math.max(0, daysSince - 180));
        if ("RENEW".equals(seg)) return (int) Math.min(95, 50 + Math.max(0, 90 - contractDaysLeft));
        if (daysSince > 120) return (int) Math.min(69, 40 + (daysSince - 120) / 10);
        return 20;
    }

    static List<String> segReasons(String seg, long daysSince, long contractDaysLeft) {
        List<String> r = new ArrayList<>();
        if ("CHURN_RISK".equals(seg)) {
            r.add(daysSince >= 9999 ? "无业务互动记录" : "近 " + daysSince + " 天无互动，存在流失风险");
        } else if ("RENEW".equals(seg)) {
            r.add(contractDaysLeft >= 9999 ? "合约临期" : "合约将于 " + contractDaysLeft + " 天后到期");
        } else if (daysSince > 120) {
            r.add("近 " + daysSince + " 天互动较少");
        }
        return r;
    }

    // ==================================================================== 工具

    private static long parseTs(String s) {
        if (s == null || s.isEmpty()) return 0;
        String[] pats = {"yyyy-MM-dd HH:mm", "yyyy-MM-dd"};
        for (String p : pats) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p);
                f.setLenient(false);
                return f.parse(s).getTime();
            } catch (Exception ignored) { }
        }
        return 0;
    }

    private String fmtTs(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(ms));
    }

    private String moneyStr(Object v) {
        if (v == null) return "—";
        try { return "¥" + ((Number) v).longValue(); } catch (Exception e) { return String.valueOf(v); }
    }

    private String scoreStr(Object v) {
        if (v == null) return "";
        try { return String.valueOf(((Number) v).intValue()); } catch (Exception e) { return String.valueOf(v); }
    }
}
