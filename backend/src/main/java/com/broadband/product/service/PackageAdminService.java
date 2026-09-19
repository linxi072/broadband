package com.broadband.product.service;

import com.broadband.common.Ids;
import com.broadband.common.RedisCacheService;
import com.broadband.common.Values;
import com.broadband.product.mapper.PackageAdminMapper;
import com.broadband.system.service.CurrentUser;
import com.broadband.system.service.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 套餐与套餐升级单（PC 后台）业务逻辑。
 *
 * <p>套餐保存涉及三张表（package_info 主表 + package_image 轮播 + package_param/option 动态参数），
 * 本类负责事务外的编排，数据访问通过 {@link PackageAdminMapper}（SQL 在 XML）。</p>
 */
@Service
public class PackageAdminService {

    @Autowired private PackageAdminMapper packageAdminMapper;
    @Autowired private OperLogService operLog;
    @Autowired private RedisCacheService cache;

    // ==================================================================== 套餐

    /** 套餐列表（含主图 / 轮播数 / 参数组数等统计列）。 */
    public List<Map<String, Object>> packages() {
        return packageAdminMapper.packages();
    }

    /**
     * 套餐保存：主表 upsert + 轮播图重建 + 动态参数重建（未变更的选项保留原加价）。
     * @throws IllegalArgumentException 套餐名称为空
     */
    public Map<String, Object> savePackage(Map<String, Object> req) {
        String id = Values.str(req.get("id"));
        String name = Values.str(req.get("name"));
        if (name == null) throw new IllegalArgumentException("套餐名称不能为空");
        if (id == null) id = Ids.next();

        String status = "上架".equals(Values.str(req.get("status"))) || Boolean.TRUE.equals(req.get("online"))
                ? "ON_SHELF" : "OFF_SHELF";

        packageAdminMapper.upsertPackage(id, name, Values.str(req.get("category")), Values.intOf(req.get("monthlyFee")),
                Values.intOf(req.get("originalFee")), status, Values.intOf(req.get("deposit")),
                Values.intOf(req.get("deviceRent")), Values.str(req.get("penalty")), Values.str(req.get("slaInfo")));

        int images = rebuildImages(id, req.get("images"), Values.str(req.get("mainImage")));
        int params = rebuildParams(id, req.get("params"));

        cache.evict("mkt:board");
        log("保存套餐 " + name + "（轮播 " + images + " / 参数组 " + params + "）",
                "/api/admin/packages", "POST");
        return Map.of("ok", true, "id", id, "images", images, "params", params);
    }

    // ==================================================================== 套餐升级单

    /** 套餐升级申请单列表（PC 后台「套餐升级管理」数据源）。 */
    public List<Map<String, Object>> upgradeOrders() {
        return packageAdminMapper.upgradeOrders();
    }

    // ==================================================================== 重建子表

    /** 重建轮播图与主图；入参与原主图都为空时返回 -1 表示「未变更」。 */
    private int rebuildImages(String packageId, Object raw, String mainImage) {
        if (raw == null && mainImage == null) return -1;
        packageAdminMapper.deleteImages(packageId);
        int n = 0;
        if (mainImage != null) {
            packageAdminMapper.insertImage(Ids.next(), packageId, "MAIN", mainImage, 1);
            n++;
        }
        if (raw instanceof List<?> list) {
            int i = 1;
            for (Object o : list) {
                String url = Values.str(o);
                if (url == null) continue;
                packageAdminMapper.insertImage(Ids.next(), packageId, "CAROUSEL", url, i++);
                n++;
            }
        }
        return n;
    }

    /** 重建动态参数组与选项；入参非列表时返回 -1 表示「未变更」。 */
    private int rebuildParams(String packageId, Object raw) {
        if (!(raw instanceof List<?> list)) return -1;
        packageAdminMapper.deleteParamOptions(packageId);
        packageAdminMapper.deleteParams(packageId);

        int groups = 0;
        int sort = 1;
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> p)) continue;
            String paramId = Ids.next();
            String groupKey = Values.str(p.get("groupKey"));
            String pname = Values.str(p.get("name"));
            if (pname == null) continue;
            packageAdminMapper.insertParam(paramId, packageId, groupKey == null ? "addon" : groupKey, pname,
                    "SINGLE".equals(Values.str(p.get("type"))) ? "SINGLE" : "MULTI",
                    Boolean.TRUE.equals(p.get("required")) ? 1 : 0, sort++);
            groups++;

            Object opts = p.get("options");
            if (opts instanceof List<?> optionList) {
                int os = 1;
                for (Object ov : optionList) {
                    String value = Values.str(ov);
                    if (value == null) continue;
                    packageAdminMapper.insertParamOption(Ids.next(), paramId, value, packageId, os++);
                }
            }
        }
        return groups;
    }

    // ==================================================================== 工具

    private void log(String action, String target, String method) {
        var me = CurrentUser.get();
        operLog.record(me == null ? null : me.user.username, me == null ? null : me.user.name,
                action, target, method, "-", "成功", 0);
    }
}
