package com.broadband.install;

import com.broadband.install.algorithm.DispatchService;
import com.broadband.install.model.Community;
import com.broadband.install.model.DispatchException;
import com.broadband.install.model.DispatchPlan;
import com.broadband.install.model.DispatchResult;
import com.broadband.install.model.WorkOrder;
import com.broadband.install.model.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 派单算法演示（可独立运行，无外部依赖）。
 * 场景一：正常派单（相邻簇合并 + 非相邻独立，全部派完）。
 * 场景二：超容拦截（单个师傅 + 9 个相邻单，容量 4 耗尽后 5 单拦截）。
 */
public class DispatchDemo {

    public static void main(String[] args) {
        // 小区：A/B 相邻(南山)，C/D 相邻(福田)，E/F 孤立(宝安/罗湖)
        Map<String, Community> cm = new HashMap<>();
        cm.put("A", new Community("A", "南山科技园", "南山", "科技园路", 22.540, 113.950));
        cm.put("B", new Community("B", "南山科技园北", "南山", "科技园路", 22.540, 113.953));
        cm.put("C", new Community("C", "福田中心", "福田", "福华路", 22.540, 114.060));
        cm.put("D", new Community("D", "福田中心南", "福田", "福华路", 22.541, 114.062));
        cm.put("E", new Community("E", "宝安中心", "宝安", "宝安路", 22.620, 113.880));
        cm.put("F", new Community("F", "罗湖广场", "罗湖", "建设路", 22.550, 114.120));

        // 三个师傅，各负责一片区（默认容量：相邻 4 / 非相邻 2）
        List<Worker> workers = new ArrayList<>();
        workers.add(new Worker("w1", "张师傅", "南山"));
        workers.add(new Worker("w2", "李师傅", "福田"));
        workers.add(new Worker("w3", "王师傅", "宝安"));

        // ===== 场景一：正常派单 =====
        List<WorkOrder> orders1 = new ArrayList<>();
        String slotAM = "2026-09-14#AM";
        orders1.add(new WorkOrder("o1", "A", slotAM, "陈先生"));
        orders1.add(new WorkOrder("o2", "A", slotAM, "刘女士"));
        orders1.add(new WorkOrder("o3", "B", slotAM, "黄先生"));  // B 与 A 相邻 -> 同簇
        orders1.add(new WorkOrder("o4", "B", slotAM, "周女士"));
        orders1.add(new WorkOrder("o5", "C", slotAM, "吴先生"));
        orders1.add(new WorkOrder("o6", "C", slotAM, "郑女士"));
        orders1.add(new WorkOrder("o7", "D", slotAM, "孙先生"));  // D 与 C 相邻 -> 同簇
        orders1.add(new WorkOrder("o8", "E", slotAM, "赵先生"));  // E 孤立 -> 非相邻
        orders1.add(new WorkOrder("o9", "F", slotAM, "钱女士"));  // F 孤立 -> 非相邻

        System.out.println("########## 场景一：正常派单 ##########");
        DispatchService svc1 = new DispatchService(cm, workers, null);
        DispatchPlan p1 = svc1.dispatch(orders1);
        printPlan(p1);

        // ===== 场景二：超容拦截（仅 1 个师傅 + 9 个相邻单，容量 4）=====
        List<Worker> oneWorker = new ArrayList<>();
        oneWorker.add(new Worker("w1", "张师傅", "南山"));
        List<WorkOrder> orders2 = new ArrayList<>();
        String slotPM = "2026-09-14#PM";
        for (int i = 0; i < 9; i++) {
            String cid = (i % 2 == 0) ? "A" : "B"; // A/B 相邻
            orders2.add(new WorkOrder("p" + i, cid, slotPM, "客户" + i));
        }

        System.out.println("\n########## 场景二：超容拦截 ##########");
        DispatchService svc2 = new DispatchService(cm, oneWorker, null);
        DispatchPlan p2 = svc2.dispatch(orders2);
        printPlan(p2);
    }

    private static void printPlan(DispatchPlan p) {
        p.logs.forEach(System.out::println);
        System.out.println("--- 派单结果(" + p.results.size() + " 单):");
        for (DispatchResult r : p.results) {
            System.out.println("  工单" + r.workOrderId + " -> 师傅" + r.workerId
                    + " 簇" + r.clusterId
                    + (r.adjacentRoute ? " [相邻路线]" : " [非相邻]")
                    + " | " + r.note);
        }
        if (!p.exceptions.isEmpty()) {
            System.out.println("--- 超容拦截(" + p.exceptions.size() + " 单):");
            for (DispatchException ex : p.exceptions) {
                System.out.println("  [拦截] 工单" + ex.workOrderId + " (" + ex.timeSlot + "): " + ex.reason);
            }
        } else {
            System.out.println("--- 无超容，全部派单成功");
        }
    }
}
