package com.broadband.community.service;

import com.broadband.common.Ids;
import com.broadband.community.mapper.CommunityAdminMapper;
import com.broadband.system.service.CurrentUser;
import com.broadband.system.service.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 小区覆盖管理（PC 后台）业务逻辑。
 *
 * <p>列表把「端口余量 / 覆盖状态」作为计算列直接在 SQL 中算出（见 CommunityAdminMapper.xml 的
 * statusExpr），与 {@code CommunityChecker} 的三态口径保持一致（余量 0 → 不可安装；余量 ≤ 8 或
 * 占用 ≥ 80% → 紧张）。</p>
 *
 * <p>Controller 只负责参数绑定、权限注解与 HTTP 响应，查询与校验逻辑全部收敛在本类；
 * 数据访问通过 {@link CommunityAdminMapper}（SQL 在 XML 中）。</p>
 */
@Service
public class AdminCommunityService {

    private static final String RESOURCE = "/api/admin/communities";

    @Autowired private CommunityAdminMapper communityAdminMapper;
    @Autowired private OperLogService operLog;

    /**
     * 小区覆盖列表（含端口余量与覆盖状态计算列）。
     * @param keyword 可选，按小区名 / 区域 / 街道模糊匹配
     */
    public List<Map<String, Object>> list(String keyword) {
        return communityAdminMapper.list(keyword);
    }

    /**
     * 新增或更新小区覆盖信息（按 id upsert）。
     * @return {@code {ok=true, id=...}}
     * @throws IllegalArgumentException 名称为空或已占用端口大于端口总数
     */
    public Map<String, Object> save(Map<String, Object> req) {
        String id = str(req.get("id"));
        String name = str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("小区名称不能为空");
        if (id == null) id = Ids.next();

        int portTotal = intOf(req.get("portTotal"));
        int portUsed = intOf(req.get("portUsed"));
        if (portUsed > portTotal) throw new IllegalArgumentException("已占用端口不能大于端口总数");

        Boolean covered = boolOf(req.get("covered"));
        communityAdminMapper.save(id, name, str(req.get("region")), str(req.get("street")),
                str(req.get("carrier")), dblOf(req.get("lat")), dblOf(req.get("lng")),
                Boolean.FALSE.equals(covered) ? 0 : 1, portTotal, portUsed);

        log("保存小区覆盖");
        return Map.of("ok", true, "id", id);
    }

    // ------------------------------------------------------------------ 辅助

    /** 写操作留痕：沿用后台统一的记录口径，日志失败不影响主流程 */
    private void log(String action) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username,
                me == null ? null : me.user.name,
                action, RESOURCE, "WRITE", "-", "成功", 0);
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

    private static double dblOf(Object v) {
        if (v == null) return 0d;
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    private static Boolean boolOf(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v).trim();
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(s) || "0".equals(s)) return Boolean.FALSE;
        return null;
    }
}
