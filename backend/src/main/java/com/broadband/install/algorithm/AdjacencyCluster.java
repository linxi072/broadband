package com.broadband.install.algorithm;

import com.broadband.install.model.Community;
import com.broadband.install.model.WorkOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于小区相邻性的并查集聚类。
 * 同一时段内，互相相邻的小区工单被聚为同一簇，便于合并路线、批量派单。
 * 簇大小 > 1 表示簇内存在相邻小区（合并路线）；簇大小 == 1 表示孤立小区（非相邻独立派单）。
 */
public final class AdjacencyCluster {

    private AdjacencyCluster() {}

    /** 对同属一个时段的工单做相邻聚类，返回簇列表（按簇大小降序，大簇优先派单）。 */
    public static List<Cluster> cluster(List<WorkOrder> orders, Map<String, Community> communityMap) {
        int n = orders.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        for (int i = 0; i < n; i++) {
            Community ci = communityMap.get(orders.get(i).communityId);
            for (int j = i + 1; j < n; j++) {
                Community cj = communityMap.get(orders.get(j).communityId);
                if (ci != null && cj != null && CapacityPolicy.isAdjacent(ci, cj)) {
                    union(parent, i, j);
                }
            }
        }

        Map<Integer, List<WorkOrder>> groups = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(orders.get(i));
        }

        List<Cluster> clusters = new ArrayList<>();
        int cid = 1;
        for (List<WorkOrder> list : groups.values()) {
            Set<String> communityIds = new LinkedHashSet<>();
            for (WorkOrder o : list) communityIds.add(o.communityId);
            clusters.add(new Cluster("C" + (cid++), list, communityIds));
        }
        // 大簇优先派单，提升合并命中率
        clusters.sort((a, b) -> Integer.compare(b.orders.size(), a.orders.size()));
        return clusters;
    }

    private static int find(int[] p, int x) {
        while (p[x] != x) {
            p[x] = p[p[x]];
            x = p[x];
        }
        return x;
    }

    private static void union(int[] p, int a, int b) {
        int ra = find(p, a);
        int rb = find(p, b);
        if (ra != rb) p[ra] = rb;
    }

    /** 相邻聚类簇。 */
    public static class Cluster {
        public final String clusterId;
        public final List<WorkOrder> orders;
        public final Set<String> communityIds;

        Cluster(String clusterId, List<WorkOrder> orders, Set<String> communityIds) {
            this.clusterId = clusterId;
            this.orders = orders;
            this.communityIds = communityIds;
        }

        /** 簇大小 > 1 即表示簇内存在相邻小区（走合并路线）。 */
        public boolean isAdjacentCluster() {
            return orders.size() > 1;
        }
    }
}
