package com.broadband.product.service;

import com.broadband.common.Ids;
import com.broadband.common.Values;
import com.broadband.product.mapper.PointsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分成长体系（C 端，V1.14 运营留存）：余额、签到、任务、商城、兑换。数据访问经 {@link PointsMapper}。
 */
@Service
public class PointsService {

    private static final int SIGN_BASE = 5;            // 每日基础签到分
    private static final int SIGN_STREAK_BONUS = 20;   // 连续 7 天额外奖励

    @Autowired private PointsMapper pointsMapper;

    /** 积分余额概览（含连续签到天数与今日是否已签）。 */
    public Map<String, Object> balance(String customerId) {
        if (Values.isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        ensureAccount(customerId);
        Map<String, Object> pa = pointsMapper.selectAccount(customerId);
        String today = LocalDate.now().toString();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("customerId", customerId);
        resp.put("balance", pa.get("balance"));
        resp.put("totalEarned", pa.get("totalEarned"));
        resp.put("totalSpent", pa.get("totalSpent"));
        resp.put("signStreak", pa.get("signStreak"));
        resp.put("signedToday", today.equals(Values.str(pa.get("signDate"))));
        return resp;
    }

    /** 每日签到（幂等：同日重复签到不重复加积分）。 */
    public Map<String, Object> sign(String customerId) {
        if (Values.isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        ensureAccount(customerId);
        String today = LocalDate.now().toString();
        Map<String, Object> pa = pointsMapper.selectAccount(customerId);
        String last = Values.str(pa.get("signDate"));
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
                ? Values.intOf(pa.get("signStreak")) + 1 : 1;
        int earned = SIGN_BASE + (streak % 7 == 0 ? SIGN_STREAK_BONUS : 0);

        long now = System.currentTimeMillis();
        pointsMapper.updateSign(customerId, earned, today, streak);
        pointsMapper.insertRecord(Ids.next(), customerId, "SIGN", earned,
                "每日签到(连续" + streak + "天)", null, now);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("alreadySigned", false);
        r.put("earned", earned);
        r.put("streak", streak);
        r.put("balance", Values.intOf(pa.get("balance")) + earned);
        return r;
    }

    /** 积分任务列表（仅启用）。 */
    public List<Map<String, Object>> tasks() {
        return pointsMapper.selectTasks();
    }

    /** 积分商城商品列表（仅上架）。 */
    public List<Map<String, Object>> mall() {
        return pointsMapper.selectMall();
    }

    /** 兑换商品：扣积分 + 减库存 + 生成优惠券。 */
    public Map<String, Object> redeem(String customerId, String itemId) {
        if (Values.isBlank(customerId)) throw new IllegalArgumentException("customerId 必填");
        if (Values.isBlank(itemId)) throw new IllegalArgumentException("itemId 必填");
        ensureAccount(customerId);
        Map<String, Object> item = pointsMapper.selectItem(itemId);
        if (item == null) throw new IllegalArgumentException("商品不存在：" + itemId);
        if (!"ON_SHELF".equals(item.get("status"))) throw new IllegalArgumentException("商品已下架");
        int cost = Values.intOf(item.get("cost"));
        int stock = Values.intOf(item.get("stock"));
        if (stock == 0) throw new IllegalArgumentException("商品已售罄");
        Map<String, Object> pa = pointsMapper.selectAccount(customerId);
        int balance = Values.intOf(pa.get("balance"));
        if (balance < cost) throw new IllegalArgumentException("积分不足（当前 " + balance + "，需 " + cost + "）");

        long now = System.currentTimeMillis();
        String couponCode = "CP" + now + (int) (Math.random() * 9000 + 1000);
        pointsMapper.updateRedeem(customerId, cost);
        if (stock > 0) pointsMapper.updateStock(itemId);
        pointsMapper.insertRecord(Ids.next(), customerId, "REDEEM", -cost,
                "兑换:" + item.get("name"), itemId, now);
        pointsMapper.insertCoupon(Ids.next(), customerId, itemId, couponCode,
                item.get("couponType"), item.get("couponValue"), now);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("couponCode", couponCode);
        r.put("balance", balance - cost);
        return r;
    }

    // ---------------------------------------------------------------- 辅助

    /** 首次访问时自动开户，保证后续查询都能拿到账户行。 */
    private void ensureAccount(String customerId) {
        Integer n = pointsMapper.countAccount(customerId);
        if (n == null || n == 0) {
            pointsMapper.insertAccount(customerId, System.currentTimeMillis());
        }
    }
}
