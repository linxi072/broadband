package com.broadband.install.algorithm;

import com.broadband.install.model.Community;
import com.broadband.install.model.WorkerCapacity;
import com.broadband.system.service.ConfigCacheService;

/**
 * 时段容量策略：相邻小区每时段 3-4 单、非相邻小区每时段 1-2 单。
 * 相邻性：同街道 OR 经纬度距离 <= ADJACENT_DISTANCE_METERS（默认 800m）。
 *
 * <p>T-05 配置动态刷新（US-2.4）：全局默认容量阈值与相邻距离均从 {@link ConfigCacheService}
 * （sys_config 内存缓存）读取，修改 sys_config 并经 {@link #bind(ConfigCacheService)} 注入后，
 * <b>无需重启</b>即可生效（后台「参数配置」改值 -> reloadConfig -> 下次派单/容量查询即取新值）。
 * 常量保留为兜底默认值，避免配置中心不可用时系统退化为 0。
 */
public final class CapacityPolicy {

    /** 文档化默认值（同时作为配置缺失时的兜底）。 */
    public static final int ADJACENT_MIN = 3;
    public static final int ADJACENT_MAX = 4;
    public static final int NON_ADJACENT_MIN = 1;
    public static final int NON_ADJACENT_MAX = 2;
    public static final double ADJACENT_DISTANCE_METERS = 800.0;

    /** sys_config 键。 */
    public static final String K_ADJ_CAP = "dispatch.default.adjacent.cap";
    public static final String K_ADJ_MIN = "dispatch.default.adjacent.min";
    public static final String K_NADJ_CAP = "dispatch.default.non.adjacent.cap";
    public static final String K_NADJ_MIN = "dispatch.default.non.adjacent.min";
    public static final String K_DISTANCE = "dispatch.adjacent.distance.meters";

    private static volatile ConfigCacheService cache;

    /** 由 {@code InstallConfigBridge} 在 Spring 容器启动时注入。 */
    public static void bind(ConfigCacheService c) {
        cache = c;
    }

    private static int cfgInt(String key, int def) {
        String v = cache == null ? null : cache.getConfigValue(key);
        if (v == null || v.isBlank()) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double cfgDouble(String key, double def) {
        String v = cache == null ? null : cache.getConfigValue(key);
        if (v == null || v.isBlank()) return def;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ------------------------------------------------------- 全局默认（热刷新）

    public static int adjacentMaxDefault() {
        return cfgInt(K_ADJ_CAP, ADJACENT_MAX);
    }

    public static int adjacentMinDefault() {
        return cfgInt(K_ADJ_MIN, ADJACENT_MIN);
    }

    public static int nonAdjacentMaxDefault() {
        return cfgInt(K_NADJ_CAP, NON_ADJACENT_MAX);
    }

    public static int nonAdjacentMinDefault() {
        return cfgInt(K_NADJ_MIN, NON_ADJACENT_MIN);
    }

    public static double adjacentDistanceMeters() {
        return cfgDouble(K_DISTANCE, ADJACENT_DISTANCE_METERS);
    }

    // ------------------------------------------------------- 单师傅容量

    /** 取某师傅某时段的相邻容量上限（worker_capacity 配置优先，缺省回退全局默认）。 */
    public static int adjacentCapacity(WorkerCapacity cap) {
        return (cap != null && cap.adjacentCap != null) ? cap.adjacentCap : adjacentMaxDefault();
    }

    /** 取某师傅某时段的非相邻容量上限。 */
    public static int nonAdjacentCapacity(WorkerCapacity cap) {
        return (cap != null && cap.nonAdjacentCap != null) ? cap.nonAdjacentCap : nonAdjacentMaxDefault();
    }

    /** 两小区是否相邻（同街道或距离 <= 阈值，阈值可热刷新）。 */
    public static boolean isAdjacent(Community a, Community b) {
        if (a == null || b == null) return false;
        if (a.id.equals(b.id)) return true;
        if (a.street != null && b.street != null && a.street.equals(b.street)) return true;
        return GeoUtils.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                <= adjacentDistanceMeters();
    }

    private CapacityPolicy() {}
}
