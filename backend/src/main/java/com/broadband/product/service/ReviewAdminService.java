package com.broadband.product.service;

import com.broadband.common.Values;
import com.broadband.product.mapper.ReviewAdminMapper;
import com.broadband.system.service.CurrentUser;
import com.broadband.system.service.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 投诉与评价（PC 后台）业务逻辑：列表查询与闭环操作。数据访问通过 {@link ReviewAdminMapper}（SQL 在 XML）。
 */
@Service
public class ReviewAdminService {

    @Autowired private ReviewAdminMapper reviewAdminMapper;
    @Autowired private OperLogService operLog;

    /**
     * 评价 / 投诉列表。
     * @param type   中文类型（投诉 / 评价）或原始枚举值
     * @param status 中文状态（待处理 / 处理中 / 已回访 / 已闭环）或原始枚举值
     */
    public List<Map<String, Object>> reviews(String type, String status) {
        List<Map<String, Object>> rows = reviewAdminMapper.reviews(
                type == null ? null : type,
                status == null ? null : statusCode(status));
        for (Map<String, Object> row : rows) {
            row.put("tags", Values.splitTags(Values.str(row.get("tags"))));
        }
        return rows;
    }

    /** 闭环投诉 / 评价：置 CLOSED 并留痕。 */
    public Map<String, Object> closeReview(String id) {
        int n = reviewAdminMapper.closeReview(id);
        log("闭环投诉/评价 " + id, "/api/admin/reviews/" + id + "/close", "PUT");
        return Map.of("ok", true, "updated", n);
    }

    // ==================================================================== 工具

    /** 中文状态标签 -> 状态枚举（已是枚举则原样返回）。 */
    private static String statusCode(String label) {
        return switch (label) {
            case "待处理" -> "PENDING";
            case "处理中" -> "PROCESSING";
            case "已回访" -> "VISITED";
            case "已闭环" -> "CLOSED";
            default -> label;
        };
    }

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
