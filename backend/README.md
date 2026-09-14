# 宽带业务系统 · 安装工单派单算法

基于「小区地理相邻性」的智能派单模块：相邻小区合并路线批量施工（容量更高），非相邻小区独立派单（容量更低），
并在派单时实时校验**时段容量**，超出即**超容拦截**，避免师傅过载与跨片区空跑。

## 容量规则（派单调度规则）

| 小区关系 | 同一时间段可接单量 | 调度策略 |
|---|---|---|
| 相邻小区（同街道 / 距离 ≤ 800m） | **3–4 单** | 合并派单、同路线连续施工 |
| 非相邻小区（跨片区 / 需驾车） | **1–2 单** | 独立派单、单独排程 |

规则常量集中在 `algorithm/CapacityPolicy.java`：
- `ADJACENT_MIN=3 / ADJACENT_MAX=4`
- `NON_ADJACENT_MIN=1 / NON_ADJACENT_MAX=2`
- `ADJACENT_DISTANCE_METERS=800`（相邻判定距离阈值，可改）
- 单师傅容量可在 `worker_capacity` 表按 `adjacent_cap / non_adjacent_cap` 微调（覆盖默认）

## 目录结构

```
backend/
├── pom.xml
├── README.md
└── src/main/java/com/broadband/dispatch/
    ├── DispatchDemo.java          # 可独立运行的算法验证（无 Spring 依赖）
    ├── model/                     # 实体：Community / WorkOrder / Worker / WorkerCapacity
    │                              #       DispatchPlan / DispatchResult / DispatchException / CapacityBoardItem
    ├── algorithm/                 # 核心算法（纯 Java，可单测）
    │   ├── GeoUtils.java          # Haversine 距离
    │   ├── CapacityPolicy.java    # 容量策略 + 相邻性判定
    │   ├── AdjacencyCluster.java  # 并查集聚类（相邻小区归同一簇）
    │   └── DispatchService.java   # 派单编排：分组→聚类→容量校验→贪心派单→超容拦截
    ├── mapper/                    # MyBatis-Plus Mapper 接口（4 张表）
    └── spring/                    # Spring Boot 适配层
        ├── DispatchServiceApi.java
        ├── DispatchServiceImpl.java
        └── DispatchController.java
```

## 快速验证（无需 Spring）

```bash
cd backend/src/main/java
javac -d /tmp/dispatch-out \
  com/broadband/dispatch/model/*.java \
  com/broadband/dispatch/algorithm/*.java \
  com/broadband/dispatch/DispatchDemo.java
java -cp /tmp/dispatch-out com.broadband.dispatch.DispatchDemo
```

演示包含两个场景：
- **场景一（正常）**：9 单（相邻簇 4+3、非相邻 2）全部派完，相邻簇合并给同片区的师傅。
- **场景二（超容）**：单师傅 + 9 个相邻单，容量上限 4，派 4 单后 5 单**超容拦截**并给出改派建议。

## 集成到现有 backend（Spring Boot 3.2.1 + MyBatis-Plus）

1. 将 `algorithm/`、`model/`、`mapper/`、`spring/` 复制到业务工程的对应包下。
2. 引入依赖（见 `pom.xml`）：`spring-boot-starter-web` + `mybatis-plus-boot-starter` + 数据库驱动。
3. 建表（见下方 DDL 要点），`worker_capacity` 用于按师傅/时段配置容量上限。
4. 启动后调用：
   - `POST /api/dispatch/run` —— 执行一次派单（自动写回 `work_order.worker_id / cluster_id / adjacent_route / status`）
   - `GET  /api/dispatch/capacity?timeSlot=2026-09-14#AM` —— 师傅时段容量看板

### 关键表字段

- `community`：id, name, region, street, latitude, longitude, installable, port_total, port_used
- `worker`：id, name, region, skill_level
- `worker_capacity`：worker_id, time_slot, adjacent_cap, non_adjacent_cap
- `work_order`：id, community_id, address, time_slot, customer_name, package_desc, status,
  worker_id, cluster_id, adjacent_route

> 相邻性以 `community.region / street / latitude / longitude` 计算距离阈值；派单后实时占用时段容量，防止超派。

## 算法说明

1. **按时段分组**：同一 `time_slot` 的待派工单进入同一派单批次。
2. **相邻性聚类**：对批次内工单两两比较小区是否相邻（同街道或距离 ≤ 阈值），用**并查集**归簇。
   簇大小 > 1 = 相邻合并路线；簇大小 == 1 = 孤立小区（非相邻独立派单）。
3. **时段容量校验 + 贪心派单**：
   - 相邻簇优先整簇派给**同片区且相邻容量充足**的师傅；容量不足则拆分塞入多个师傅。
   - 非相邻单占用非相邻容量（每师傅 1–2 单）。
4. **超容拦截**：某时段所有师傅对应容量耗尽时，剩余工单进入 `DispatchPlan.exceptions`，
   返回改派 / 增派 / 调整时段建议，不静默丢弃。
