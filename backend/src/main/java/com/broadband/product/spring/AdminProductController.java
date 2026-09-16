package com.broadband.product.spring;

import com.broadband.common.Ids;
import com.broadband.system.spring.AuthController;
import com.broadband.system.spring.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户 / 套餐 / 升级单 / 投诉评价 / 销售 / 财务 的后台读侧聚合（PC 后台）。
 *
 * <p>全部为真实表查询。聚合口径说明：</p>
 * <ul>
 *   <li>销售「完成率」= 该销售已完成订单 / 总订单（不用臆造的转化率——没有线索数据支撑）。</li>
 *   <li>财务「应收」= 该月未支付订单金额；赔付金额取自 compensation 表（真实成本）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private OperLogService operLog;

    // ==================================================================== 客户

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('customer:view')")
    public List<Map<String, Object>> customers(@RequestParam(required = false) String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.name, c.phone, c.level,
                       CASE c.level WHEN 'VIP' THEN '五星' WHEN 'GOLD' THEN '四星'
                                    WHEN 'SILVER' THEN '三星' ELSE '普通' END AS levelLabel,
                       p.name AS pkgName,
                       COALESCE(ct.monthly_fee, p.monthly_fee, 0) AS monthlyFee,
                       ct.end_date AS contractEnd, c.address,
                       com.name AS communityName,
                       CONCAT_WS(',',
                         IF(p.monthly_fee >= 199, '千兆', NULL),
                         IF(ct.id IS NOT NULL, '合约中', NULL),
                         IF(ct.end_date IS NOT NULL AND ct.end_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY), '临期', NULL)
                       ) AS tagStr,
                       CASE WHEN ct.end_date IS NOT NULL AND ct.end_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)
                            THEN '待续约' ELSE '在用' END AS status
                FROM customer c
                LEFT JOIN package_info p ON p.id = c.package_id
                LEFT JOIN customer_contract ct ON ct.customer_id = c.id AND ct.status = 'ACTIVE'
                LEFT JOIN community com ON com.id = c.community_id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (c.name LIKE ? OR c.phone LIKE ?)");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY c.id");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        for (Map<String, Object> row : rows) {
            row.put("tags", splitTags(str(row.get("tagStr"))));
            row.remove("tagStr");
        }
        return rows;
    }

    // ==================================================================== 客户 360 视图

    /**
     * 客户 360 全景：档案 + 业务订单 + 合约 + 流量 + 投诉评价 + 安装工单 + 升级申请 + 汇总指标。
     * 单一人口、服务端按 customerId 聚合，避免前端多接口拼装与越权风险。
     */
    @GetMapping("/customer/{id}/360")
    @PreAuthorize("hasAuthority('customer:view')")
    public Map<String, Object> customer360(@PathVariable String id) {
        Map<String, Object> result = new LinkedHashMap<>();

        // ---- 档案 ----
        Map<String, Object> profile = jdbc.queryForList("""
                SELECT c.id, c.name, c.phone, c.level,
                       CASE c.level WHEN 'VIP' THEN '五星' WHEN 'GOLD' THEN '四星'
                                    WHEN 'SILVER' THEN '三星' ELSE '普通' END AS levelLabel,
                       p.name AS pkgName, p.monthly_fee AS pkgMonthlyFee,
                       c.package_id AS packageId, c.community_id AS communityId,
                       com.name AS communityName, c.address,
                       CASE c.status WHEN 'ACTIVE' THEN '在用' WHEN 'SUSPENDED' THEN '暂停'
                                    WHEN 'CLOSED' THEN '已销户' ELSE c.status END AS statusLabel,
                       ct.id AS contractId, ct.start_date AS contractStart, ct.end_date AS contractEnd,
                       CASE ct.status WHEN 'ACTIVE' THEN '生效中' WHEN 'EXPIRED' THEN '已到期'
                                      WHEN 'TERMINATED' THEN '已终止' ELSE ct.status END AS contractStatus,
                       ct.monthly_fee AS contractMonthlyFee,
                       CONCAT_WS(',',
                         IF(p.monthly_fee >= 199, '千兆', NULL),
                         IF(ct.id IS NOT NULL, '合约中', NULL),
                         IF(ct.end_date IS NOT NULL AND ct.end_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY), '临期', NULL)
                       ) AS tagStr
                FROM customer c
                LEFT JOIN package_info p ON p.id = c.package_id
                LEFT JOIN customer_contract ct ON ct.customer_id = c.id AND ct.status = 'ACTIVE'
                LEFT JOIN community com ON com.id = c.community_id
                WHERE c.id = ?
                """, id).stream().findFirst().orElse(null);
        if (profile != null) {
            profile.put("tags", splitTags(str(profile.get("tagStr"))));
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
            return result;
        }

        String name = str(profile.get("name"));

        // ---- 业务订单 ----
        result.put("orders", jdbc.queryForList("""
                SELECT id, package_name AS packageName, amount,
                       order_type AS orderType,
                       CASE order_type WHEN 'NEW_INSTALL' THEN '新装' WHEN 'MOVE' THEN '移机'
                                        WHEN 'RENEW' THEN '续费' WHEN 'SPEED_UP' THEN '提速'
                                        WHEN 'REPAIR' THEN '报修' WHEN 'ADDON' THEN '加购' ELSE order_type END AS orderTypeLabel,
                       CASE status WHEN 'PENDING' THEN '待支付' WHEN 'PAID' THEN '已支付'
                                  WHEN 'INSTALLING' THEN '安装中' WHEN 'DONE' THEN '已完成'
                                  WHEN 'CANCELLED' THEN '已取消' ELSE status END AS statusLabel,
                       DATE_FORMAT(FROM_UNIXTIME(created_time/1000), '%Y-%m-%d %H:%i') AS createdAt
                FROM biz_order WHERE customer_id = ? ORDER BY created_time DESC LIMIT 50
                """, id));

        // ---- 合约 ----
        result.put("contracts", jdbc.queryForList("""
                SELECT id, package_id AS packageId, monthly_fee AS monthlyFee,
                       start_date AS startDate, end_date AS endDate,
                       CASE status WHEN 'ACTIVE' THEN '生效中' WHEN 'EXPIRED' THEN '已到期'
                                   WHEN 'TERMINATED' THEN '已终止' ELSE status END AS statusLabel
                FROM customer_contract WHERE customer_id = ? ORDER BY start_date DESC LIMIT 20
                """, id));

        // ---- 流量（取最近一个周期） ----
        Map<String, Object> traffic = jdbc.queryForList("""
                SELECT period_month AS period, mobile_total AS mobileTotal, mobile_used AS mobileUsed,
                       broadband_hours AS broadbandHours, broadband_peak AS broadbandPeak,
                       daily_trend AS dailyTrend
                FROM traffic_usage WHERE customer_id = ? ORDER BY period_month DESC LIMIT 1
                """, id).stream().findFirst().orElse(null);
        if (traffic != null) {
            String trend = str(traffic.get("dailyTrend"));
            List<Integer> arr = new ArrayList<>();
            if (trend != null) for (String s : trend.split(",")) {
                try { arr.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
            }
            traffic.put("dailyTrend", arr);
        }
        result.put("traffic", traffic);

        // ---- 投诉与评价 ----
        result.put("reviews", jdbc.queryForList("""
                SELECT id, order_id AS orderId, score, tags, type, content,
                       CASE type WHEN 'COMPLAINT' THEN '投诉' ELSE '评价' END AS typeLabel,
                       CASE status WHEN 'PENDING' THEN '待处理' WHEN 'PROCESSING' THEN '处理中'
                                   WHEN 'VISITED' THEN '已回访' WHEN 'CLOSED' THEN '已闭环'
                                   ELSE status END AS statusLabel,
                       DATE_FORMAT(FROM_UNIXTIME(created_time/1000), '%Y-%m-%d %H:%i') AS createdAt
                FROM review WHERE customer_name = ? ORDER BY created_time DESC LIMIT 50
                """, name == null ? "" : name));

        // ---- 安装工单（经 biz_order 关联 或 按客户姓名） ----
        result.put("workOrders", jdbc.queryForList("""
                SELECT wo.id, wo.status, wo.time_slot AS timeSlot, wo.customer_name AS customerName,
                       wo.package_desc AS packageDesc, wo.worker_id AS workerId,
                       wo.down_speed AS downSpeed, wo.up_speed AS upSpeed,
                       wo.sign_name AS signName, wo.complete_time AS completeTime,
                       bo.id AS bizOrderId, bo.status AS bizStatus,
                       CASE wo.status WHEN 'PENDING' THEN '待派单' WHEN 'ASSIGNED' THEN '已派单'
                                      WHEN 'INSTALLING' THEN '安装中' WHEN 'DONE' THEN '已完成'
                                      WHEN 'CANCELLED' THEN '已取消' ELSE wo.status END AS statusLabel
                FROM work_order wo
                LEFT JOIN biz_order bo ON bo.id = wo.biz_order_id
                WHERE bo.customer_id = ? OR wo.customer_name = ?
                ORDER BY wo.complete_time DESC, wo.id DESC LIMIT 50
                """, id, name == null ? "" : name));

        // ---- 升级申请单 ----
        result.put("upgradeOrders", jdbc.queryForList("""
                SELECT id, from_package_id AS fromPackageId, target_band_key AS targetBandKey,
                       addon_keys AS addonKeys, month_diff AS monthDiff, one_time_diff AS oneTimeDiff,
                       current_fee AS currentFee, new_fee AS newFee, effect_type AS effectType,
                       CASE status WHEN 'SUBMITTED' THEN '待审核' WHEN 'EFFECTIVE' THEN '已生效'
                                   WHEN 'REJECTED' THEN '已驳回' ELSE status END AS statusLabel,
                       DATE_FORMAT(FROM_UNIXTIME(created_time/1000), '%Y-%m-%d %H:%i') AS createdAt
                FROM package_upgrade_order WHERE customer_id = ? ORDER BY created_time DESC LIMIT 20
                """, id));

        // ---- 汇总指标 ----
        Map<String, Object> summary = jdbc.queryForList("""
                SELECT
                  (SELECT COUNT(*) FROM biz_order WHERE customer_id = ?) AS orderCount,
                  (SELECT COALESCE(SUM(amount),0) FROM biz_order WHERE customer_id = ? AND status IN ('PAID','INSTALLING','DONE')) AS paidAmount,
                  (SELECT COUNT(*) FROM work_order wo LEFT JOIN biz_order bo ON bo.id = wo.biz_order_id WHERE bo.customer_id = ? OR wo.customer_name = ?) AS workOrderCount,
                  (SELECT COUNT(*) FROM review WHERE customer_name = ?) AS reviewCount,
                  (SELECT COALESCE(ROUND(AVG(score),1),0) FROM review WHERE customer_name = ?) AS avgScore,
                  (SELECT COUNT(*) FROM package_upgrade_order WHERE customer_id = ?) AS upgradeCount
                """, id, id, id, name == null ? "" : name, name == null ? "" : name, name == null ? "" : name, id)
                .stream().findFirst().orElse(Map.of());
        result.put("summary", summary);

        return result;
    }

    // ==================================================================== 套餐

    @GetMapping("/packages")
    @PreAuthorize("hasAuthority('package:view')")
    public List<Map<String, Object>> packages() {
        return jdbc.queryForList("""
                SELECT p.id, p.name, p.category,
                       p.monthly_fee AS monthlyFee, p.original_fee AS originalFee,
                       p.deposit, p.device_rent AS deviceRent, p.penalty, p.sla_info AS slaInfo,
                       CASE WHEN p.status = 'ON_SHELF' THEN '上架' ELSE '下架' END AS status,
                       CASE WHEN p.status = 'ON_SHELF' THEN TRUE ELSE FALSE END AS online,
                       (SELECT GROUP_CONCAT(o.option_value ORDER BY o.sort_order SEPARATOR ' / ')
                          FROM package_param pp JOIN package_param_option o ON o.param_id = pp.id
                         WHERE pp.package_id = p.id AND pp.group_key = 'contract') AS contract,
                       (SELECT COUNT(*) FROM package_image i
                         WHERE i.package_id = p.id AND i.type = 'MAIN') AS mainImageCount,
                       (SELECT i.url FROM package_image i
                         WHERE i.package_id = p.id AND i.type = 'MAIN' ORDER BY i.sort_order LIMIT 1) AS mainImage,
                       (SELECT COUNT(*) FROM package_image i
                         WHERE i.package_id = p.id AND i.type = 'CAROUSEL') AS images,
                       (SELECT COUNT(*) FROM package_param pp WHERE pp.package_id = p.id) AS params
                FROM package_info p ORDER BY p.id
                """);
    }

    /** 套餐保存：主表 upsert + 轮播图重建 + 动态参数重建（未变更的选项保留原加价）。 */
    @PostMapping("/packages")
    @PreAuthorize("hasAuthority('package:edit')")
    public Map<String, Object> savePackage(@RequestBody Map<String, Object> req) {
        String id = str(req.get("id"));
        String name = str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("套餐名称不能为空");
        if (id == null) id = Ids.next();

        String status = "上架".equals(str(req.get("status"))) || Boolean.TRUE.equals(req.get("online"))
                ? "ON_SHELF" : "OFF_SHELF";

        jdbc.update("""
                INSERT INTO package_info (id, name, category, monthly_fee, original_fee, status,
                                          deposit, device_rent, penalty, sla_info)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  name = VALUES(name), category = VALUES(category), monthly_fee = VALUES(monthly_fee),
                  original_fee = VALUES(original_fee), status = VALUES(status), deposit = VALUES(deposit),
                  device_rent = VALUES(device_rent), penalty = VALUES(penalty), sla_info = VALUES(sla_info)
                """, id, name, str(req.get("category")), intOf(req.get("monthlyFee")),
                intOf(req.get("originalFee")), status, intOf(req.get("deposit")),
                intOf(req.get("deviceRent")), str(req.get("penalty")), str(req.get("slaInfo")));

        int images = rebuildImages(id, req.get("images"), str(req.get("mainImage")));
        int params = rebuildParams(id, req.get("params"));

        log("保存套餐 " + name + "（轮播 " + images + " / 参数组 " + params + "）",
                "/api/admin/packages", "POST");
        return Map.of("ok", true, "id", id, "images", images, "params", params);
    }

    private int rebuildImages(String packageId, Object raw, String mainImage) {
        if (raw == null && mainImage == null) return -1;
        jdbc.update("DELETE FROM package_image WHERE package_id = ?", packageId);
        int n = 0;
        if (mainImage != null) {
            jdbc.update("INSERT INTO package_image (id, package_id, type, url, sort_order) VALUES (?,?,?,?,?)",
                    Ids.next(), packageId, "MAIN", mainImage, 1);
            n++;
        }
        if (raw instanceof List<?> list) {
            int i = 1;
            for (Object o : list) {
                String url = str(o);
                if (url == null) continue;
                jdbc.update("INSERT INTO package_image (id, package_id, type, url, sort_order) VALUES (?,?,?,?,?)",
                        Ids.next(), packageId, "CAROUSEL", url, i++);
                n++;
            }
        }
        return n;
    }

    private int rebuildParams(String packageId, Object raw) {
        if (!(raw instanceof List<?> list)) return -1;
        jdbc.update("DELETE FROM package_param_option WHERE param_id IN (SELECT id FROM package_param WHERE package_id = ?)",
                packageId);
        jdbc.update("DELETE FROM package_param WHERE package_id = ?", packageId);

        int groups = 0;
        int sort = 1;
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> p)) continue;
            String paramId = Ids.next();
            String groupKey = str(p.get("groupKey"));
            String pname = str(p.get("name"));
            if (pname == null) continue;
            jdbc.update("""
                    INSERT INTO package_param (id, package_id, group_key, name, type, required, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, paramId, packageId, groupKey == null ? "addon" : groupKey, pname,
                    "SINGLE".equals(str(p.get("type"))) ? "SINGLE" : "MULTI",
                    Boolean.TRUE.equals(p.get("required")) ? 1 : 0, sort++);
            groups++;

            Object opts = p.get("options");
            if (opts instanceof List<?> optionList) {
                int os = 1;
                for (Object ov : optionList) {
                    String value = str(ov);
                    if (value == null) continue;
                    // 保留该套餐下同名选项的历史加价，避免编辑时把资费抹成 0
                    jdbc.update("""
                            INSERT INTO package_param_option (id, param_id, option_value, extra_fee, sort_order)
                            VALUES (?, ?, ?, COALESCE((
                                SELECT o2.extra_fee FROM package_param_option o2
                                JOIN package_param p2 ON p2.id = o2.param_id
                                WHERE p2.package_id = ? AND o2.option_value = ? LIMIT 1), 0), ?)
                            """, Ids.next(), paramId, value, packageId, value, os++);
                }
            }
        }
        return groups;
    }

    // ==================================================================== 套餐升级单

    @GetMapping("/upgrade-orders")
    @PreAuthorize("hasAuthority('upgrade:view')")
    public List<Map<String, Object>> upgradeOrders() {
        return jdbc.queryForList("""
                SELECT u.id, c.name AS customer,
                       fp.name AS fromPkg,
                       COALESCE(bo.option_value, u.target_band_key) AS toPkg,
                       u.addon_keys AS addonKeys,
                       u.month_diff AS monthDiff, u.one_time_diff AS oneTimeDiff,
                       u.current_fee AS currentFee, u.new_fee AS newFee,
                       u.effect_type AS effectType, u.created_time AS createdTime,
                       CASE u.status WHEN 'SUBMITTED' THEN '待审核' WHEN 'EFFECTIVE' THEN '已生效'
                                     WHEN 'REJECTED' THEN '已驳回' ELSE u.status END AS status,
                       DATE_FORMAT(FROM_UNIXTIME(u.created_time/1000), '%Y-%m-%d %H:%i') AS time
                FROM package_upgrade_order u
                LEFT JOIN customer c ON c.id = u.customer_id
                LEFT JOIN package_info fp ON fp.id = u.from_package_id
                LEFT JOIN package_param_option bo ON bo.id = u.target_band_key
                ORDER BY u.created_time DESC LIMIT 200
                """);
    }

    // ==================================================================== 投诉与评价

    @GetMapping("/reviews")
    @PreAuthorize("hasAuthority('review:view')")
    public List<Map<String, Object>> reviews(@RequestParam(required = false) String type,
                                             @RequestParam(required = false) String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, order_id AS orderId, customer_name AS customer, worker_name AS worker, score,
                       tags, content, created_time AS createdTime,
                       CASE type WHEN 'COMPLAINT' THEN '投诉' ELSE '评价' END AS type,
                       CASE status WHEN 'PENDING' THEN '待处理' WHEN 'PROCESSING' THEN '处理中'
                                   WHEN 'VISITED' THEN '已回访' WHEN 'CLOSED' THEN '已闭环'
                                   ELSE status END AS status,
                       DATE_FORMAT(FROM_UNIXTIME(created_time/1000), '%Y-%m-%d %H:%i') AS time
                FROM review WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            args.add("投诉".equals(type) ? "COMPLAINT" : "REVIEW");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(statusCode(status));
        }
        sql.append(" ORDER BY created_time DESC LIMIT 300");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        for (Map<String, Object> row : rows) {
            row.put("tags", splitTags(str(row.get("tags"))));
        }
        return rows;
    }

    @PutMapping("/reviews/{id}/close")
    @PreAuthorize("hasAuthority('review:view')")
    public Map<String, Object> closeReview(@PathVariable String id) {
        int n = jdbc.update("UPDATE review SET status = 'CLOSED' WHERE id = ?", id);
        log("闭环投诉/评价 " + id, "/api/admin/reviews/" + id + "/close", "PUT");
        return Map.of("ok", true, "updated", n);
    }

    // ==================================================================== 销售 / 财务

    @GetMapping("/sales/report")
    @PreAuthorize("hasAuthority('sales:view')")
    public List<Map<String, Object>> salesReport() {
        return jdbc.queryForList("""
                SELECT sales_name AS name,
                       MAX(region) AS region,
                       DATE_FORMAT(FROM_UNIXTIME(created_time/1000), '%Y-%m') AS month,
                       COUNT(*) AS orders,
                       SUM(amount) AS amount,
                       ROUND(SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS doneRate
                FROM biz_order
                WHERE sales_name IS NOT NULL AND sales_name <> ''
                GROUP BY sales_name, month
                ORDER BY month DESC, amount DESC
                """);
    }

    @GetMapping("/finance/report")
    @PreAuthorize("hasAuthority('finance:view')")
    public List<Map<String, Object>> financeReport() {
        String currentMonth = LocalDate.now().toString().substring(0, 7);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT DATE_FORMAT(FROM_UNIXTIME(created_time/1000), '%Y-%m') AS month,
                       SUM(CASE WHEN status IN ('PAID','INSTALLING','DONE') THEN amount ELSE 0 END) AS revenue,
                       SUM(CASE WHEN status = 'CANCELLED' THEN amount ELSE 0 END) AS refund,
                       SUM(CASE WHEN status = 'PENDING' THEN amount ELSE 0 END) AS receivable,
                       COUNT(*) AS orders
                FROM biz_order
                GROUP BY month
                ORDER BY month DESC
                """);
        Map<String, Long> compByMonth = new LinkedHashMap<>();
        for (Map<String, Object> c : jdbc.queryForList("""
                SELECT DATE_FORMAT(FROM_UNIXTIME(created_time/1000), '%Y-%m') AS month,
                       COALESCE(SUM(comp_amount), 0) AS comp
                FROM compensation GROUP BY month
                """)) {
            compByMonth.put(String.valueOf(c.get("month")), ((Number) c.get("comp")).longValue());
        }
        for (Map<String, Object> row : rows) {
            String month = String.valueOf(row.get("month"));
            row.put("compensation", compByMonth.getOrDefault(month, 0L));
            row.put("status", month.equals(currentMonth) ? "对账中" : "已结账");
        }
        return rows;
    }

    // ==================================================================== 套餐营销看板

    /**
     * 套餐营销看板：汇总指标 + 套餐销量排行 + 业务类型分布 + 升级单状态分布 + 客户分层分布 + 近 6 月营收趋势。
     * 平台级读侧聚合，复用 package:view 权限（与「套餐列表」同源，避免新增 RBAC 面）。
     */
    @GetMapping("/product/marketing")
    @PreAuthorize("hasAuthority('package:view')")
    public Map<String, Object> productMarketing() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ---- 汇总指标 ----
        Map<String, Object> summary = jdbc.queryForList("""
                SELECT
                  (SELECT COUNT(*) FROM biz_order) AS totalOrders,
                  (SELECT COALESCE(SUM(amount),0) FROM biz_order WHERE status <> 'CANCELLED') AS totalRevenue,
                  (SELECT COUNT(*) FROM biz_order WHERE status = 'DONE') AS doneOrders,
                  (SELECT COALESCE(ROUND(AVG(amount),0),0) FROM biz_order WHERE status <> 'CANCELLED') AS avgOrderAmount,
                  (SELECT COUNT(*) FROM customer) AS customerCount,
                  (SELECT COUNT(*) FROM package_upgrade_order) AS upgradeCount,
                  (SELECT COUNT(*) FROM package_upgrade_order WHERE status = 'EFFECTIVE') AS effectiveUpgrades
                """).stream().findFirst().orElse(new LinkedHashMap<>());
        long upgradeCount = ((Number) summary.getOrDefault("upgradeCount", 0)).longValue();
        long effectiveUpgrades = ((Number) summary.getOrDefault("effectiveUpgrades", 0)).longValue();
        double upgradeRate = upgradeCount == 0 ? 0 : Math.round(effectiveUpgrades * 1000.0 / upgradeCount) / 10.0;
        summary.put("upgradeRate", upgradeRate);
        result.put("summary", summary);

        // ---- 套餐销量排行 ----
        long totalRevenue = ((Number) summary.getOrDefault("totalRevenue", 0)).longValue();
        List<Map<String, Object>> ranking = jdbc.queryForList("""
                SELECT package_name AS name, COUNT(*) AS orders, COALESCE(SUM(amount),0) AS revenue
                FROM biz_order WHERE package_name IS NOT NULL AND package_name <> ''
                GROUP BY package_name ORDER BY revenue DESC
                """);
        for (Map<String, Object> r : ranking) {
            long rev = ((Number) r.getOrDefault("revenue", 0)).longValue();
            r.put("ratio", totalRevenue == 0 ? 0 : Math.round(rev * 1000.0 / totalRevenue) / 10.0);
        }
        result.put("packageRanking", ranking);

        // ---- 业务类型分布 ----
        Map<String, String> typeLabels = Map.of(
                "NEW_INSTALL", "新装宽带", "MOVE", "宽带移机", "RENEW", "续费",
                "SPEED_UP", "宽带提速", "REPAIR", "故障报修", "ADDON", "加购");
        List<Map<String, Object>> typeDist = jdbc.queryForList("""
                SELECT order_type AS type, COUNT(*) AS orders, COALESCE(SUM(amount),0) AS revenue
                FROM biz_order GROUP BY order_type
                """);
        for (Map<String, Object> t : typeDist) {
            String type = String.valueOf(t.get("type"));
            t.put("typeLabel", typeLabels.getOrDefault(type, type));
        }
        result.put("orderTypeDist", typeDist);

        // ---- 升级单状态分布 ----
        Map<String, String> upLabels = Map.of("SUBMITTED", "待审核", "EFFECTIVE", "已生效", "REJECTED", "已驳回");
        List<Map<String, Object>> upgradeDist = jdbc.queryForList("""
                SELECT status, COUNT(*) AS count FROM package_upgrade_order GROUP BY status
                """);
        for (Map<String, Object> u : upgradeDist) {
            String s = String.valueOf(u.get("status"));
            u.put("statusLabel", upLabels.getOrDefault(s, s));
        }
        result.put("upgradeByStatus", upgradeDist);

        // ---- 客户分层分布 ----
        Map<String, String> levelLabels = Map.of("VIP", "五星", "GOLD", "四星", "SILVER", "三星", "NORMAL", "普通");
        List<Map<String, Object>> levelDist = jdbc.queryForList("""
                SELECT level, COUNT(*) AS count FROM customer GROUP BY level
                """);
        for (Map<String, Object> l : levelDist) {
            String lv = String.valueOf(l.get("level"));
            l.put("levelLabel", levelLabels.getOrDefault(lv, lv));
        }
        result.put("customerLevelDist", levelDist);

        // ---- 近 6 月营收趋势 ----
        long threshold = LocalDate.now().minusMonths(5).withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        List<Map<String, Object>> trend = jdbc.queryForList("""
                SELECT DATE_FORMAT(FROM_UNIXTIME(created_time/1000), '%Y-%m') AS month,
                       COALESCE(SUM(CASE WHEN status IN ('PAID','INSTALLING','DONE') THEN amount ELSE 0 END),0) AS revenue,
                       COUNT(*) AS orders
                FROM biz_order WHERE created_time >= ?
                GROUP BY month ORDER BY month ASC
                """, threshold);
        result.put("revenueTrend", trend);

        return result;
    }

    // ==================================================================== 工具

    private void log(String action, String target, String method) {
        var me = AuthController.current();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }

    private static String statusCode(String label) {
        return switch (label) {
            case "待处理" -> "PENDING";
            case "处理中" -> "PROCESSING";
            case "已回访" -> "VISITED";
            case "已闭环" -> "CLOSED";
            default -> label;
        };
    }

    private static List<String> splitTags(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static int intOf(Object v) {
        if (v == null) return 0;
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
