package com.broadband.install.spring;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.broadband.install.algorithm.CapacityPolicy;
import com.broadband.install.algorithm.DispatchService;
import com.broadband.install.mapper.CommunityMapper;
import com.broadband.install.mapper.WorkOrderMapper;
import com.broadband.install.mapper.WorkerCapacityMapper;
import com.broadband.install.mapper.WorkerMapper;
import com.broadband.install.model.CapacityBoardItem;
import com.broadband.install.model.Community;
import com.broadband.install.model.DispatchException;
import com.broadband.install.model.DispatchPlan;
import com.broadband.install.model.DispatchResult;
import com.broadband.install.model.WorkOrder;
import com.broadband.install.model.WorkOrderStatus;
import com.broadband.install.model.Worker;
import com.broadband.install.model.WorkerCapacity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 派单服务实现。组装 DB 数据 -> 调用核心算法 algorithm.DispatchService -> 回写派单结果。
 * 核心算法不依赖 Spring，可独立单测（见 DispatchDemo）。
 */
@Service
public class DispatchServiceImpl implements DispatchServiceApi {

    @Autowired private WorkOrderMapper workOrderMapper;
    @Autowired private CommunityMapper communityMapper;
    @Autowired private WorkerMapper workerMapper;
    @Autowired private WorkerCapacityMapper capacityMapper;

    @Override
    public DispatchPlan runDispatch() {
        // 1) 取待派工单
        List<WorkOrder> pending = workOrderMapper.selectList(
                new QueryWrapper<WorkOrder>().eq("status", WorkOrderStatus.PENDING.name()));

        // 2) 取小区 / 师傅 / 时段容量配置
        Map<String, Community> communityMap = new LinkedHashMap<>();
        for (Community c : communityMapper.selectList(null)) communityMap.put(c.id, c);
        List<Worker> workers = workerMapper.selectList(null);
        List<WorkerCapacity> capacities = capacityMapper.selectList(null);

        // 3) 调用核心算法
        DispatchService engine = new DispatchService(communityMap, workers, capacities);
        DispatchPlan plan = engine.dispatch(pending);

        // 4) 回写派单结果
        for (DispatchResult r : plan.results) {
            workOrderMapper.update(null, new UpdateWrapper<WorkOrder>()
                    .eq("id", r.workOrderId)
                    .set("worker_id", r.workerId)
                    .set("cluster_id", r.clusterId)
                    .set("adjacent_route", r.adjacentRoute)
                    .set("status", WorkOrderStatus.ASSIGNED.name()));
        }

        // 5) 超容工单：告警给调度员（此处仅打印，实际可发 MQ / 站内信）
        for (DispatchException ex : plan.exceptions) {
            System.err.println("[派单超容] " + ex.workOrderId + " @ " + ex.timeSlot + " : " + ex.reason);
        }
        return plan;
    }

    @Override
    public List<CapacityBoardItem> capacityBoard(String timeSlot) {
        List<Worker> workers = workerMapper.selectList(null);
        List<WorkerCapacity> caps = capacityMapper.selectList(
                new QueryWrapper<WorkerCapacity>().eq("time_slot", timeSlot));

        // 该时段已派/施工中工单
        List<WorkOrder> assigned = workOrderMapper.selectList(new QueryWrapper<WorkOrder>()
                .eq("time_slot", timeSlot)
                .in("status", WorkOrderStatus.ASSIGNED.name(), WorkOrderStatus.INSTALLING.name()));

        List<CapacityBoardItem> board = new ArrayList<>();
        for (Worker w : workers) {
            WorkerCapacity cap = caps.stream()
                    .filter(c -> c.workerId.equals(w.id)).findFirst().orElse(null);
            int adjCap = CapacityPolicy.adjacentCapacity(cap);
            int nonCap = CapacityPolicy.nonAdjacentCapacity(cap);

            int adjUsed = 0, nonUsed = 0;
            for (WorkOrder o : assigned) {
                if (!w.id.equals(o.workerId)) continue;
                if (Boolean.TRUE.equals(o.adjacentRoute)) adjUsed++;
                else nonUsed++;
            }
            boolean full = (adjUsed >= adjCap) && (nonUsed >= nonCap);
            board.add(new CapacityBoardItem(w.id, w.name, timeSlot, adjUsed, adjCap, nonUsed, nonCap, full));
        }
        return board;
    }
}
