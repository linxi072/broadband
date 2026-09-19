package com.broadband.product.service;

import com.broadband.common.RedisCacheService;
import com.broadband.common.Values;
import com.broadband.product.mapper.RefundAdminMapper;
import com.broadband.system.service.CurrentUser;
import com.broadband.system.service.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 退款 / 对账状态机（PC 后台）：退款单查询与审批、驳回。数据访问通过 {@link RefundAdminMapper}（SQL 在 XML）。
 *
 * <p>审批通过会同步把业务订单置 REFUND（营收扣减），两步写库在同一请求内完成。</p>
 */
@Service
public class RefundAdminService {

    @Autowired private RefundAdminMapper refundAdminMapper;
    @Autowired private OperLogService operLog;
    @Autowired private RedisCacheService cache;

    /** 退款单列表，可选按状态过滤。 */
    public List<Map<String, Object>> refunds(String status) {
        return refundAdminMapper.refunds(status == null ? null : status);
    }

    /**
     * 审批退款：PENDING -> REFUNDED，生成退款流水号，并将业务订单置 REFUND（营收扣减）。
     * @throws IllegalArgumentException 退款单不存在
     * @throws IllegalStateException    非 PENDING 状态
     */
    public Map<String, Object> approveRefund(String id, Map<String, Object> body) {
        Map<String, Object> rf = refundAdminMapper.refundById(id);
        if (rf == null) throw new IllegalArgumentException("退款单不存在：" + id);
        if (!"PENDING".equals(rf.get("status")))
            throw new IllegalStateException("仅 PENDING 退款单可审批，当前：" + rf.get("status"));
        String refundNo = "RN" + System.currentTimeMillis();
        String operator = Values.str(body == null ? null : body.get("operator"), "财务");
        refundAdminMapper.approveRefund(id, refundNo, operator, System.currentTimeMillis());
        refundAdminMapper.markOrderRefunded(Values.str(rf.get("order_id")));
        cache.evict("dash:stats");
        log("审批退款", id, "POST");
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("refundNo", refundNo);
        resp.put("status", "REFUNDED");
        resp.put("orderStatus", "REFUND");
        return resp;
    }

    /**
     * 驳回退款：PENDING -> REJECTED。
     * @throws IllegalArgumentException 退款单不存在
     * @throws IllegalStateException    非 PENDING 状态
     */
    public Map<String, Object> rejectRefund(String id, Map<String, Object> body) {
        Map<String, Object> rf = refundAdminMapper.refundById(id);
        if (rf == null) throw new IllegalArgumentException("退款单不存在：" + id);
        if (!"PENDING".equals(rf.get("status")))
            throw new IllegalStateException("仅 PENDING 退款单可驳回，当前：" + rf.get("status"));
        String operator = Values.str(body == null ? null : body.get("operator"), "财务");
        refundAdminMapper.rejectRefund(id, operator, System.currentTimeMillis());
        log("驳回退款", id, "POST");
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("id", id);
        resp.put("status", "REJECTED");
        return resp;
    }

    // ==================================================================== 工具

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
