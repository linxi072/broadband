package com.broadband.install.service;

import com.broadband.install.mapper.AdminInstallMapper;
import com.broadband.system.service.CurrentUser;
import com.broadband.system.service.OperLogService;
import com.broadband.system.service.SysDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 安装工单与师傅容量管理（PC 后台）业务逻辑。
 *
 * <p>Controller 只负责权限注解、参数绑定与 HTTP 响应；查询 SQL、部门数据权限过滤、
 * 容量批量写入与操作留痕全部收敛在本类；数据访问通过 {@link AdminInstallMapper}
 * （SQL 集中在 AdminInstallMapper.xml）。</p>
 */
@Service
public class AdminInstallService {

    @Autowired private AdminInstallMapper adminInstallMapper;
    @Autowired private OperLogService operLog;
    @Autowired private SysDepartmentService departmentService;

    // ==================================================================== 工单池

    /**
     * 工单池（联表解析小区名 / 师傅名），按登录用户部门做行级数据隔离。
     *
     * @param status  可选状态过滤
     * @param keyword 可选关键字（工单号 / 小区 / 客户名）
     */
    public List<Map<String, Object>> workOrders(String status, String keyword) {
        return adminInstallMapper.selectWorkOrders(status, keyword, dataScopeDeptIds());
    }

    /** 单条工单详情（师傅端工单详情页用）。 */
    public Map<String, Object> workOrder(String id) {
        Map<String, Object> r = adminInstallMapper.selectWorkOrder(id);
        return r == null ? Map.of() : r;
    }

    // ==================================================================== 师傅

    /** 师傅列表（技能等级映射为 初/中/高级）。 */
    public List<Map<String, Object>> workers() {
        return adminInstallMapper.selectWorkers();
    }

    /**
     * 师傅容量批量设置。
     * req: { workerId, day: 'yyyy-MM-dd', slots: [{ slotKey: 'AM', adjacentCap: 4, nonAdjacentCap: 2, enabled: true }] }
     *
     * <p>time_slot 记为 {@code day#slotKey}，与派单算法的时段标识一致；
     * enabled=false 表示删除配置、回落到全局默认容量（相邻 3-4 / 非相邻 1-2）。</p>
     *
     * @return {@code {ok, saved, cleared}}
     * @throws IllegalArgumentException 缺少 workerId / day / slots
     */
    public Map<String, Object> saveCapacity(Map<String, Object> req) {
        String workerId = str(req.get("workerId"));
        String day = str(req.get("day"));
        if (workerId == null || day == null) throw new IllegalArgumentException("缺少 workerId 或 day");

        Object rawSlots = req.get("slots");
        if (!(rawSlots instanceof List<?> slots)) throw new IllegalArgumentException("缺少 slots");

        int saved = 0;
        int cleared = 0;
        for (Object o : slots) {
            if (!(o instanceof Map<?, ?> slot)) continue;
            String slotKey = str(slot.get("slotKey"));
            if (slotKey == null) continue;
            String timeSlot = day + "#" + slotKey;
            boolean enabled = !Boolean.FALSE.equals(slot.get("enabled"));

            if (!enabled) {
                cleared += adminInstallMapper.deleteCapacity(workerId, timeSlot);
                continue;
            }
            int adj = intOf(slot.get("adjacentCap"), 4);
            int non = intOf(slot.get("nonAdjacentCap"), 2);
            adminInstallMapper.upsertCapacity(workerId, timeSlot, adj, non);
            saved++;
        }

        log("设置师傅容量 " + workerId + " " + day + "（" + saved + " 段）",
                "/api/admin/worker-capacity", "POST");
        return Map.of("ok", true, "saved", saved, "cleared", cleared);
    }

    // ==================================================================== 工具

    /** 当前登录用户的数据权限部门集合；null 表示不限定（看全部）。 */
    private List<String> dataScopeDeptIds() {
        var me = CurrentUser.get();
        if (me == null || me.user.deptId == null || me.user.deptId.isEmpty()) return null;
        return departmentService.visibleDeptIds(me.user.deptId);
    }

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static int intOf(Object v, int def) {
        if (v == null) return def;
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
