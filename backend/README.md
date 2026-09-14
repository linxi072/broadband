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

---

## 运行（Maven + MySQL，M4 已落地）

> 后端已从「纯 Java 演示 + mock」升级为**可运行的 Spring Boot 单体服务**，直接读写 MySQL。

### 1. 前置
- JDK 21、Maven 3.9+
- MySQL 8.x 已启动，并具备一个可建表的账号（示例用 `broadband / broadband`）

### 2. 建库
无需手工建表——`spring.datasource.url` 带 `createDatabaseIfNotExist=true`，
且启动时会自动执行 `src/main/resources/db/schema.sql`（建表）与 `db/data.sql`（种子数据）。
两者均为幂等写法（`CREATE TABLE IF NOT EXISTS` / `INSERT IGNORE`），可反复启动。

> 如需手工执行：`mysql -ubroadband -p broadband < src/main/resources/db/schema.sql`，
> 再执行 `db/data.sql`。

### 3. 启动
```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn -DskipTests clean package

# 连接信息通过环境变量注入（不落库、不进仓库）
DB_USERNAME=broadband DB_PASSWORD=broadband \
DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=broadband \
java -jar target/broadband-backend-1.0.0.jar --server.port=8082
```
可用环境变量：`DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD`（均有默认值，密码默认空）。

### 4. 冒烟验证
```bash
curl -G 'http://127.0.0.1:8082/api/community/check' --data-urlencode 'name=科技园'
curl -G 'http://127.0.0.1:8082/api/package/detail' --data-urlencode 'id=pkg500'
curl -G 'http://127.0.0.1:8082/api/traffic/usage'  --data-urlencode 'customerId=demo'
curl -G 'http://127.0.0.1:8082/api/package/upgrade-options' --data-urlencode 'customerId=demo'
curl -s -X POST http://127.0.0.1:8082/api/dispatch/run -H 'Content-Type: application/json' -d '{}'
curl -s -G http://127.0.0.1:8082/api/dispatch/capacity --data-urlencode 'timeSlot=2026-09-15#AM'
```

> `POST /api/dispatch/run` 会把待派工单置为 `ASSIGNED`，因此**重复演示前**需重置：
> `UPDATE work_order SET status='PENDING', worker_id=NULL, cluster_id=NULL, adjacent_route=NULL;`

### 5. 目录结构（重构后 3 模块）
```
backend/src/main/java/com/broadband/
├── BroadbandApplication.java     # 启动类（@MapperScan 只扫 @Mapper 注解接口）
├── common/Ids.java               # 主键生成（纯 Java，零框架依赖）
├── community/                    # 模块一：小区覆盖（check / demand）
├── install/                      # 模块二：派单 dispatch + 装维 SLA 赔付 sla
└── product/                      # 模块三：套餐 pkg + 流量 traffic
    ├── engine/  algorithm/       # 纯 Java 核心算法（可独立编译运行验证）
    ├── mapper/                   # MyBatis-Plus Mapper
    └── spring/                   # ServiceApi / ServiceImpl / Controller
```

### 6. 关键设计：模型零 ORM 注解
`model/` 下的实体**不引入任何 MyBatis-Plus 注解**，保持纯 Java（核心算法可脱离框架独立验证）。
由此带来两点约定：

- 主键由 `common/Ids.next()` 显式生成，不依赖 ORM 主键策略；
- 两个与 SQL 关键字/歧义冲突的列做了「列名 ↔ 接口字段」桥接（见对应 Mapper 的 `@Select`）：

| 表列名 | 接口字段 | 原因 |
|---|---|---|
| `package_converge_item.description` | `desc` | `DESC` 是 MySQL 保留字 |
| `package_param_option.option_value` | `value` | 避免与 SQL 关键字歧义 |

### 7. 数据库表（17 张）
`community` / `worker` / `worker_capacity` / `work_order` / `community_demand` /
`sla_rule` / `sla_record` / `compensation` /
`package_info` / `package_image` / `package_converge_item` / `package_param` / `package_param_option` /
`traffic_usage` / `customer` / `customer_contract` / `package_upgrade_order`

> 依赖说明：Spring Boot 3 必须使用 `mybatis-plus-spring-boot3-starter`。
> 若误用 `mybatis-plus-boot-starter`，会引入 `mybatis-spring 2.x`，启动即报
> `Invalid value type for attribute 'factoryBeanObjectType': java.lang.String`。
