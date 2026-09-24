# 装维 SLA 与赔付后端模块（com.broadband.sla）

参考中国电信（「当日装当日修」/ 慢必赔）、中国移动（装机·修复超时赔、网速不达标赔）的装维承诺，
落地 **SLA 计算 + 慢必赔规则引擎**，与派单模块（`com.broadband.dispatch`）同构：核心算法纯 Java 可独立运行验证，
Spring 适配层负责组装 DB 数据与落库。

## 1. 模块结构

```
backend/src/main/java/com/broadband/sla/
├── SlaDemo.java                # 可独立运行的引擎验证（无 Spring 依赖）
├── model/
│   ├── SlaEnums.java           # 业务类型/评估方式/状态/赔付类型·单位·状态 枚举
│   ├── SlaRule.java            # SLA 规则配置（sla_rule）
│   ├── SlaRecord.java          # SLA 评估记录（sla_record）
│   ├── Compensation.java       # 赔付工单（compensation）
│   ├── SlaEvaluation.java      # 单次评估返回（record + 可能产生的 compensation）
│   └── SlaBoard.java           # 看板聚合 DTO
├── engine/
│   ├── SlaEngine.java          # 核心：时限类/速率类 SLA 计算 + 赔付金额规则引擎
│   └── SlaRulePresets.java     # 默认规则种子（新装当日装/报修当日修/网速达标）
├── mapper/                     # 3 个 MyBatis-Plus Mapper
└── spring/                     # SlaServiceApi / SlaServiceImpl / SlaController
```

## 2. 评估逻辑（SlaEngine）

两类评估方式（`SlaRule.evalType`）：

- **时限类（TIME）**：承诺完成时间 = max(预约时间, 受理时间) + 承诺小时数 − 宽限分钟。
  - 完工时间 ≤ 承诺时间 → **达标(MET)**；否则 → **超时(OVERTIME)**，记录超时分钟。
- **速率类（SPEED）**：装机测速 `speedTestMbps ≥ 规则最低速率` → 达标；否则 → 不达标（超时口径，触发赔付）。

赔付金额（`Compensation.compAmount`）：

| 计算单位 | 公式 | 封顶 |
|---|---|---|
| PER_ORDER（每单固定） | `compAmount` | `maxCompAmount` |
| PER_OVERTIME_HOUR（每超时小时） | `compAmount × ⌈超时小时⌉` | `maxCompAmount` |

仅在 **超时/不达标** 时生成赔付工单，状态默认 `PENDING(待赔付)`，状态机：
`PENDING → VERIFYING(待核实) → PAID(已赔付) / REJECTED(已驳回)`。

## 3. 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/sla/evaluate` | 评估单条工单（完工时调用），自动落库 record + compensation |
| POST | `/api/sla/evaluate-batch` | 批量评估 |
| GET | `/api/sla/board` | 看板聚合：达标率/当日装/慢必赔单/断网无忧/平均响应/本月赔付 |
| GET | `/api/sla/rules` | SLA 规则列表 |
| GET | `/api/sla/compensations` | 赔付工单列表 |

## 4. DB 表（建议字段）

```sql
CREATE TABLE sla_rule (
  id VARCHAR(32) PRIMARY KEY,
  order_type VARCHAR(20),      -- NEW_INSTALL/MOVE/REPAIR/SPEED_UP/RENEW
  sla_name VARCHAR(64),
  eval_type VARCHAR(10),       -- TIME/SPEED
  promised_hours INT,
  grace_minutes INT,
  min_speed_mbps DOUBLE,
  comp_type VARCHAR(20),       -- VOUCHER/CASH/FEE_WAIVE
  comp_amount DOUBLE,
  comp_unit VARCHAR(20),       -- PER_ORDER/PER_OVERTIME_HOUR
  max_comp_amount DOUBLE,
  enabled TINYINT
);

CREATE TABLE sla_record (
  id VARCHAR(32) PRIMARY KEY,
  order_id VARCHAR(32),
  order_type VARCHAR(20),
  cust_name VARCHAR(64),
  rule_id VARCHAR(32),
  accept_time BIGINT,
  appointed_time BIGINT,
  complete_time BIGINT,
  speed_test_mbps DOUBLE,
  promised_time BIGINT,
  sla_status VARCHAR(10),      -- PENDING/MET/OVERTIME
  overtime_minutes INT,
  created_time BIGINT
);

CREATE TABLE compensation (
  id VARCHAR(32) PRIMARY KEY,
  sla_record_id VARCHAR(32),
  order_id VARCHAR(32),
  cust_name VARCHAR(64),
  order_type VARCHAR(20),
  comp_type VARCHAR(20),
  comp_amount DOUBLE,
  reason VARCHAR(255),
  status VARCHAR(12),          -- PENDING/VERIFYING/PAID/REJECTED
  created_time BIGINT
);
```

## 5. 运行验证

```bash
cd backend/src/main/java
javac -d /tmp/sla-out \
  com/broadband/sla/model/*.java \
  com/broadband/sla/engine/*.java \
  com/broadband/sla/SlaDemo.java
java -cp /tmp/sla-out com.broadband.sla.SlaDemo
```

预期：场景 A 达标；场景 B 报修超时 210 分钟 → 赔付封顶 20；场景 C 网速 320<500 → 赔付 20；
场景 D（移机，无匹配规则）跳过。触发赔付 2 单。

## 6. 接入业务工程

1. 复制 `model/ engine/ mapper/ spring/` 到业务工程对应包（保持 `com.broadband.sla` 包名）。
2. 建表（见上）。
3. `SlaRulePresets.defaults()` 写入 `sla_rule` 作为初始规则（或后台「赔付规则」页维护）。
4. 安装工单完工/报修修复时调用 `POST /api/sla/evaluate` 完成 SLA 考核与慢必赔自动生成。
