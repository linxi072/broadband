# 套餐升级与流量监控后端模块

两个相对独立又常被前端联动使用的功能模块，遵循与派单（`dispatch`）/ 装维 SLA（`sla`）/
套餐详情（`pkg`）一致的分层：**核心算法纯 Java 可独立运行验证**，Spring 适配层负责组装 DB
数据与落库。

- **套餐升级（com.broadband.pkg）**：带宽升档 + 增值服务加购 + 一次性补差计算。
- **流量监控（com.broadband.traffic）**：手机流量 + 宽带时长 + 7 日趋势 + 阈值提醒。

参考中国电信（美好家 / FTTR / 慢必赔）、中国移动（和家庭 / 承诺消费送宽带 / 流量查询与超出提醒）。

## 1. 模块结构

```
backend/src/main/java/com/broadband/
├── pkg/
│   ├── PackageUpgradeDemo.java            # 升级补差引擎验证（无 Spring 依赖）
│   ├── engine/PackageUpgradeEngine.java   # 补差计算（纯 Java）
│   ├── model/PackageUpgradePreview.java   # 升级预览 VO
│   └── spring/PackageUpgradeController.java# GET /upgrade-options · POST /upgrade
└── traffic/
    ├── TrafficDemo.java                   # 流量用量引擎验证（无 Spring 依赖）
    ├── engine/TrafficQueryEngine.java     # 用量计算（纯 Java）
    ├── model/TrafficUsageVO.java          # 用量视图
    ├── model/TrafficRecord.java           # traffic_usage 表实体
    ├── mapper/TrafficUsageMapper.java     # MyBatis-Plus Mapper
    └── spring/                            # TrafficServiceApi / TrafficServiceImpl / TrafficController
```

## 2. 套餐升级：补差计算（PackageUpgradeEngine）

升档不改变"已付合约"，只对**合约剩余期**做一次性补差，月费自次月起按新标准。

```
monthDiff    = 目标带宽加价(targetExtra) + 增值服务加价合计(addonExtra)
newFee       = 当前月费(currentFee) + monthDiff
oneTimeDiff  = monthDiff × (合约剩余天数 / 30)        // 按 30 天一月的整数月折算
```

| 场景 | 入参 | 结果（月补差 / 一次性补差） |
|---|---|---|
| 500M(99)→1000M(+40)+FTTR(+30)，剩 540 天(18月) | (99,40,30,540) | 70 / 1260 |
| 500M(99)→2000M(+90)，剩 30 天 | (99,90,0,30) | 90 / 90 |
| 仅加购全屋WiFi(+15)，无合约剩余 | (99,0,15,0) | 15 / 0 |

生效策略（`PackageUpgradePreview.effectType`）预留两种：
`immediate`（立即生效，当月按补差计）/ `nextMonth`（次月生效）。接口层目前透传，落库时由业务决定。

## 3. 流量监控：用量计算（TrafficQueryEngine）

`compute(customerId)` 当前返回 mock 聚合（接 DB 后改为读取 `traffic_usage` 当期记录再算）：

- `pct = mobileUsed × 100 / mobileTotal`
- `remaining = mobileTotal − mobileUsed`
- `warn = remaining ≤ 5`（建议加购流量包）
- `trend`：近 7 日手机流量（G），供小程序柱状图渲染

## 4. 接口

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 套餐升级 | GET | `/api/package/upgrade-options?customerId=` | 当前套餐 + 升档/加购选项与加价 |
| 套餐升级 | POST | `/api/package/upgrade` | 提交升档，返回补差预览（currentFee/monthDiff/newFee/oneTimeDiff） |
| 流量监控 | GET | `/api/traffic/usage?customerId=` | 手机流量 + 宽带时长 + 7 日趋势 + 阈值提醒 |

前端（`broadband-miniapp/utils/api.js`）已对齐：`getUpgradeOptions` / `submitUpgrade` / `getTrafficUsage`。

## 5. DB 表（建议字段）

```sql
CREATE TABLE traffic_usage (
  id              VARCHAR(32) PRIMARY KEY,
  customer_id     VARCHAR(32) NOT NULL,
  period_month    VARCHAR(7)  NOT NULL,   -- 2026-09
  mobile_total    INT          NOT NULL,   -- 套餐总量(G)
  mobile_used     INT          NOT NULL,   -- 已用(G)
  broadband_hours INT          NOT NULL,   -- 当月宽带时长(h)
  broadband_peak  VARCHAR(16),             -- 峰值速率 943M
  daily_trend     VARCHAR(64),             -- 7 日趋势, 逗号分隔
  updated_at      DATETIME
);

-- 套餐升级订单（提交升档时落库）
CREATE TABLE package_upgrade_order (
  id              VARCHAR(32) PRIMARY KEY,
  customer_id     VARCHAR(32) NOT NULL,
  from_pkg        VARCHAR(32),
  to_bandwidth    VARCHAR(16),
  addons          VARCHAR(128),
  month_diff      INT,
  one_time_diff   INT,
  effect_type     VARCHAR(16),             -- immediate / nextMonth
  status          VARCHAR(16),             -- PENDING / EFFECTIVE / REJECTED
  created_at      DATETIME
);
```

## 6. 本地验证（不依赖 Spring）

```bash
cd backend
javac -d /tmp/bwdemo \
  src/main/java/com/broadband/pkg/engine/PackageUpgradeEngine.java \
  src/main/java/com/broadband/pkg/model/PackageUpgradePreview.java \
  src/main/java/com/broadband/pkg/PackageUpgradeDemo.java \
  src/main/java/com/broadband/traffic/engine/TrafficQueryEngine.java \
  src/main/java/com/broadband/traffic/model/TrafficUsageVO.java \
  src/main/java/com/broadband/traffic/TrafficDemo.java
java -cp /tmp/bwdemo com.broadband.pkg.PackageUpgradeDemo
java -cp /tmp/bwdemo com.broadband.traffic.TrafficDemo
```
