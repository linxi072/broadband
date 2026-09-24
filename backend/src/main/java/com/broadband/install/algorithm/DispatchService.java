package com.broadband.install.algorithm;

import com.broadband.install.model.Community;
import com.broadband.install.model.DispatchException;
import com.broadband.install.model.DispatchPlan;
import com.broadband.install.model.DispatchResult;
import com.broadband.install.model.WorkOrder;
import com.broadband.install.model.WorkOrderStatus;
import com.broadband.install.model.Worker;
import com.broadband.install.model.WorkerCapacity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 派单核心算法。
 *
 * 流程：按时段分组 -> 并查集聚类（相邻性）-> 时段容量校验 -> 贪心派单 -> 超容拦截。
 *
 * 容量规则（CapacityPolicy）：
 *  - 相邻小区工单：每师傅每时段 3-4 单（默认 4），合并路线批量施工；
 *  - 非相邻小区工单：每师傅每时段 1-2 单（默认 2），独立派单单独排程。
 *
 * 派单优先级：同片区师傅优先；容量不足时整簇拆分塞入多个师傅；全部师傅该时段容量耗尽则超容拦截。
 */
public class DispatchService {

    private final Map<String, Community> communityMap;
    private final List<Worker> workers;
    private final Map<String, WorkerCapacity> capacityMap; // key = workerId + "@" + timeSlot

    public DispatchService(Map<String, Community> communityMap,
                           List<Worker> workers,
                           List<WorkerCapacity> capacities) {
        this.communityMap = communityMap;
        this.workers = workers;
        this.capacityMap = new HashMap<>();
        if (capacities != null) {
            for (WorkerCapacity c : capacities) {
                this.capacityMap.put(c.workerId + "@" + c.timeSlot, c);
            }
        }
    }

    /** 执行派单，返回派单方案（含成功结果与超容拦截异常）。 */
    public DispatchPlan dispatch(List<WorkOrder> pending) {
        DispatchPlan plan = new DispatchPlan();

        List<WorkOrder> todo = pending.stream()
                .filter(o -> o.status == WorkOrderStatus.PENDING)
                .collect(Collectors.toList());

        // 按时段分组
        Map<String, List<WorkOrder>> bySlot = new LinkedHashMap<>();
        for (WorkOrder o : todo) {
            bySlot.computeIfAbsent(o.timeSlot, k -> new ArrayList<>()).add(o);
        }

        for (Map.Entry<String, List<WorkOrder>> e : bySlot.entrySet()) {
            String slot = e.getKey();
            List<WorkOrder> orders = e.getValue();
            plan.logs.add("=== 时段 " + slot + " 待派工单 " + orders.size() + " 单 ===");

            // 初始化各师傅该时段剩余容量: [相邻剩余, 非相邻剩余]
            Map<String, int[]> remain = new LinkedHashMap<>();
            for (Worker w : workers) {
                WorkerCapacity cap = capacityMap.get(w.id + "@" + slot);
                remain.put(w.id, new int[]{
                        CapacityPolicy.adjacentCapacity(cap),
                        CapacityPolicy.nonAdjacentCapacity(cap)
                });
            }

            List<AdjacencyCluster.Cluster> clusters = AdjacencyCluster.cluster(orders, communityMap);
            for (AdjacencyCluster.Cluster c : clusters) {
                if (c.isAdjacentCluster()) {
                    assignAdjacentCluster(plan, remain, c, slot);
                } else {
                    assignNonAdjacent(plan, remain, c, slot);
                }
            }
        }
        return plan;
    }

    /** 相邻簇派单：优先整簇派给同片区且相邻容量充足的师傅；否则拆分塞入多师傅。 */
    private void assignAdjacentCluster(DispatchPlan plan, Map<String, int[]> remain,
                                       AdjacencyCluster.Cluster c, String slot) {
        int need = c.orders.size();
        Worker best = pickWorker(remain, need, true, c);
        if (best != null) {
            for (WorkOrder o : c.orders) {
                plan.results.add(new DispatchResult(o.id, best.id, slot, c.clusterId, true, "相邻合并路线派单"));
                o.status = WorkOrderStatus.ASSIGNED;
            }
            remain.get(best.id)[0] -= need;
            plan.logs.add("簇 " + c.clusterId + "(" + need + "单相邻) -> 师傅 " + best.name
                    + "，剩余相邻容量 " + remain.get(best.id)[0]);
        } else {
            splitAssignAdjacent(plan, remain, c, slot);
        }
    }

    /** 非相邻单（集群大小 == 1）派单：占用非相邻容量。 */
    private void assignNonAdjacent(DispatchPlan plan, Map<String, int[]> remain,
                                   AdjacencyCluster.Cluster c, String slot) {
        WorkOrder o = c.orders.get(0);
        Worker best = pickWorker(remain, 1, false, c);
        if (best != null) {
            plan.results.add(new DispatchResult(o.id, best.id, slot, c.clusterId, false, "非相邻独立派单"));
            o.status = WorkOrderStatus.ASSIGNED;
            remain.get(best.id)[1] -= 1;
            plan.logs.add("单 " + o.id + "(非相邻) -> 师傅 " + best.name
                    + "，剩余非相邻容量 " + remain.get(best.id)[1]);
        } else {
            plan.exceptions.add(new DispatchException(o.id, slot,
                    "时段 " + slot + " 所有师傅非相邻容量已耗尽（每师傅上限 "
                            + CapacityPolicy.NON_ADJACENT_MAX + " 单），建议增派师傅或调整时段"));
            plan.logs.add("⚠ 单 " + o.id + " 非相邻容量不足，已拦截未派");
        }
    }

    /** 相邻容量不足整簇时，贪心将簇内工单逐个塞入有相邻余量的师傅（每师傅最多上限）。 */
    private void splitAssignAdjacent(DispatchPlan plan, Map<String, int[]> remain,
                                     AdjacencyCluster.Cluster c, String slot) {
        List<WorkOrder> remaining = new ArrayList<>(c.orders);
        for (Worker w : workers) {
            int[] r = remain.get(w.id);
            while (r[0] > 0 && !remaining.isEmpty()) {
                WorkOrder o = remaining.remove(0);
                plan.results.add(new DispatchResult(o.id, w.id, slot, c.clusterId, true,
                        "相邻拆分派单(整簇超出单师傅容量)"));
                o.status = WorkOrderStatus.ASSIGNED;
                r[0] -= 1;
            }
            if (remaining.isEmpty()) break;
        }
        for (WorkOrder o : remaining) {
            plan.exceptions.add(new DispatchException(o.id, slot,
                    "时段 " + slot + " 师傅相邻容量已耗尽（每师傅上限 "
                            + CapacityPolicy.ADJACENT_MAX + " 单/簇），簇 " + c.clusterId
                            + " 部分工单超容，建议增派师傅或调整时段"));
            plan.logs.add("⚠ 簇 " + c.clusterId + " 工单 " + o.id + " 相邻容量不足，已拦截未派");
        }
    }

    /**
     * 选师傅：adjacency=true 比较相邻剩余容量，否则比较非相邻剩余容量。
     * 优先返回同片区师傅；无同片区则返回首个满足条件的师傅。
     */
    private Worker pickWorker(Map<String, int[]> remain, int need, boolean adjacency, AdjacencyCluster.Cluster c) {
        Worker fallback = null;
        for (Worker w : workers) {
            int[] r = remain.get(w.id);
            int avail = adjacency ? r[0] : r[1];
            if (avail < need) continue;
            boolean sameRegion = c.orders.stream().anyMatch(o -> {
                Community com = communityMap.get(o.communityId);
                return com != null && com.region != null && com.region.equals(w.region);
            });
            if (sameRegion) return w;
            if (fallback == null) fallback = w;
        }
        return fallback;
    }
}
