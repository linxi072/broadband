package com.broadband.install.algorithm;

import com.broadband.install.model.Community;
import com.broadband.install.model.WorkerCapacity;

/**
 * 时段容量策略：相邻小区每时段 3-4 单、非相邻小区每时段 1-2 单。
 * 相邻性：同街道 OR 经纬度距离 <= ADJACENT_DISTANCE_METERS（默认 800m）。
 */
public final class CapacityPolicy {
    public static final int ADJACENT_MIN = 3;
    public static final int ADJACENT_MAX = 4;
    public static final int NON_ADJACENT_MIN = 1;
    public static final int NON_ADJACENT_MAX = 2;
    public static final double ADJACENT_DISTANCE_METERS = 800.0;

    private CapacityPolicy() {}

    /** 取某师傅某时段的相邻容量上限（配置优先，缺省回退默认）。 */
    public static int adjacentCapacity(WorkerCapacity cap) {
        return (cap != null && cap.adjacentCap != null) ? cap.adjacentCap : ADJACENT_MAX;
    }

    /** 取某师傅某时段的非相邻容量上限。 */
    public static int nonAdjacentCapacity(WorkerCapacity cap) {
        return (cap != null && cap.nonAdjacentCap != null) ? cap.nonAdjacentCap : NON_ADJACENT_MAX;
    }

    /** 两小区是否相邻（同街道或距离 <= 阈值）。 */
    public static boolean isAdjacent(Community a, Community b) {
        if (a == null || b == null) return false;
        if (a.id.equals(b.id)) return true;
        if (a.street != null && b.street != null && a.street.equals(b.street)) return true;
        return GeoUtils.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                <= ADJACENT_DISTANCE_METERS;
    }
}
