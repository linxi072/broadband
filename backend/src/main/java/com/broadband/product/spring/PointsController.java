package com.broadband.product.spring;

import com.broadband.common.Ids;
import com.broadband.system.security.CustomerPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分成长体系（C 端小程序，V1.14 运营留存）。
 *
 * <ul>
 *   <li>GET  /api/points/balance —— 积分余额 + 连续签到天数 + 今日是否已签</li>
 *   <li>POST /api/points/sign    —— 每日签到（幂等：同日重复签到不重复加积分）</li>
 *   <li>GET  /api/points/tasks   —— 积分任务列表</li>
 *   <li>GET  /api/points/mall    —— 积分商城商品列表</li>
 *   <li>POST /api/points/redeem  —— 兑换商品（生成优惠券、扣减积分）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/points")
public class PointsController {

    @Autowired private JdbcTemplate jdbc;

    private static final int SIGN_BASE = 5;       // 每日基础签到分
    private static final int SIGN_STREAK_BONUS = 20; // 连续 7 天额外奖励

    /** 积分余额概览。入参 customerId。 */
    @GetMapping("/balance")
    public Map<String, Object> balance(@RequestParam String customerId) {
        if (isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        ensureAccount(customerId);
        Map<String, Object> pa = one(
                "SELECT balance, total_earned, total_spent, sign_date, sign_streak "
                        + "FROM points_account WHERE customer_id = ?", customerId);
        String today = LocalDate.now().toString();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("customerId", customerId);
        resp.put("balance", pa.get("balance"));
        resp.put("totalEarned", pa.get("total_earned"));
        resp.put("totalSpent", pa.get("total_spent"));
        resp.put("signStreak", pa.get("sign_streak"));
        resp.put("signedToday", today.equals(str(pa.get("sign_date"))));
        return resp;
    }

    /** 每日签到（幂等）。身份优先取认证主体，未认证（开发态开放层）回退请求参数。 */
    @PostMapping("/sign")
    public Map<String, Object> sign(@RequestParam String customerId,
                                    @AuthenticationPrincipal CustomerPrincipal cp) {
        customerId = resolveCustomerId(cp, customerId);
        if (isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        ensureAccount(customerId);
        String today = LocalDate.now().toString();
        Map<String, Object> pa = one(
                "SELECT balance, sign_date, sign_streak FROM points_account WHERE customer_id = ?", customerId);
        String last = str(pa.get("sign_date"));
        if (today.equals(last)) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ok", true);
            r.put("alreadySigned", true);
            r.put("balance", pa.get("balance"));
            r.put("earned", 0);
            return r;
        }

        // 连续天数：上一天签过则 +1，否则重置为 1
        int streak = (last != null && last.equals(LocalDate.now().minusDays(1).toString()))
                ? ((Number) pa.get("sign_streak")).intValue() + 1 : 1;
        int earned = SIGN_BASE + (streak % 7 == 0 ? SIGN_STREAK_BONUS : 0);

        long now = System.currentTimeMillis();
        jdbc.update("UPDATE points_account SET balance = balance + ?, total_earned = total_earned + ?, "
                        + "sign_date = ?, sign_streak = ? WHERE customer_id = ?",
                earned, earned, today, streak, customerId);
        jdbc.update("INSERT INTO points_record (id, customer_id, type, amount, remark, created_time) "
                        + "VALUES (?,?, 'SIGN', ?, ?, ?)",
                Ids.next(), customerId, earned, "每日签到(连续" + streak + "天)", now);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("alreadySigned", false);
        r.put("earned", earned);
        r.put("streak", streak);
        r.put("balance", ((Number) pa.get("balance")).intValue() + earned);
        return r;
    }

    /** 积分任务列表。 */
    @GetMapping("/tasks")
    public List<Map<String, Object>> tasks(@RequestParam(required = false) String customerId) {
        return jdbc.queryForList(
                "SELECT id, task_key AS taskKey, name, points, description FROM points_task "
                        + "WHERE status='ENABLED' ORDER BY sort_order");
    }

    /** 积分商城商品列表。 */
    @GetMapping("/mall")
    public List<Map<String, Object>> mall() {
        return jdbc.queryForList(
                "SELECT id, name, cost, stock, coupon_type AS couponType, coupon_value AS couponValue, image, status "
                        + "FROM points_mall_item WHERE status='ON_SHELF' ORDER BY cost");
    }

    /** 兑换商品：扣积分 + 生成优惠券。身份优先取认证主体，未认证（开发态开放层）回退请求参数。 */
    @PostMapping("/redeem")
    public Map<String, Object> redeem(@RequestParam(required = false) String customerId, @RequestParam String itemId,
                                      @AuthenticationPrincipal CustomerPrincipal cp) {
        customerId = resolveCustomerId(cp, customerId);
        if (isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        if (isBlank(itemId)) throw new IllegalArgumentException("itemId 必填");
        ensureAccount(customerId);
        Map<String, Object> item = one(
                "SELECT id, name, cost, stock, coupon_type, coupon_value, status "
                        + "FROM points_mall_item WHERE id = ?", itemId);
        if (item == null) throw new IllegalArgumentException("商品不存在：" + itemId);
        if (!"ON_SHELF".equals(item.get("status"))) throw new IllegalArgumentException("商品已下架");
        int cost = ((Number) item.get("cost")).intValue();
        int stock = ((Number) item.get("stock")).intValue();
        if (stock == 0) throw new IllegalArgumentException("商品已售罄");
        Map<String, Object> pa = one("SELECT balance FROM points_account WHERE customer_id = ?", customerId);
        int balance = ((Number) pa.get("balance")).intValue();
        if (balance < cost) throw new IllegalArgumentException("积分不足（当前 " + balance + "，需 " + cost + "）");

        long now = System.currentTimeMillis();
        String couponCode = "CP" + now + (int) (Math.random() * 9000 + 1000);
        jdbc.update("UPDATE points_account SET balance = balance - ?, total_spent = total_spent + ? "
                        + "WHERE customer_id = ?",
                cost, cost, customerId);
        if (stock > 0) jdbc.update("UPDATE points_mall_item SET stock = stock - 1 WHERE id = ?", itemId);
        jdbc.update("INSERT INTO points_record (id, customer_id, type, amount, remark, ref_id, created_time) "
                        + "VALUES (?,?, 'REDEEM', ?, ?, ?, ?)",
                Ids.next(), customerId, -cost, "兑换:" + item.get("name"), itemId, now);
        jdbc.update("INSERT INTO points_coupon (id, customer_id, item_id, coupon_code, coupon_type, "
                        + "coupon_value, status, created_time) VALUES (?,?,?,?,?,?, 'UNUSED', ?)",
                Ids.next(), customerId, itemId, couponCode,
                item.get("coupon_type"), item.get("coupon_value"), now);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("couponCode", couponCode);
        r.put("balance", balance - cost);
        return r;
    }

    // ---------------------------------------------------------------- 辅助

    private void ensureAccount(String customerId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_account WHERE customer_id = ?", Integer.class, customerId);
        if (n == null || n == 0) {
            jdbc.update("INSERT INTO points_account (customer_id, balance, total_earned, total_spent, sign_streak, created_time) "
                            + "VALUES (?,0,0,0,0,?)", customerId, System.currentTimeMillis());
        }
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> l = jdbc.queryForList(sql, args);
        return l.isEmpty() ? null : l.get(0);
    }

    /**
     * 解析操作主体身份：小程序 token 落地（protect-client-api=true）后，优先采用认证主体，
     * 杜绝客户端伪造 customerId 的越权（IDOR）；开发态开放层未认证时回退请求参数。
     */
    private static String resolveCustomerId(CustomerPrincipal cp, String fallback) {
        if (cp != null && cp.id != null && !cp.id.isBlank()) return cp.id;
        return fallback;
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
