# 宽带业务管理系统 · 项目总览

> 文档版本：v1.0 ｜ 更新日期：2026-09-14 ｜ 关联设计稿：broadband-design/index.html（v1.9）

## 一、项目简介

宽带业务管理系统是一套面向「家庭宽带办理与装维运营」的业务平台，覆盖 **客户端（微信小程序）**、**PC 后台管理系统（规划中）** 与 **后端服务（Spring Boot）** 三端。系统以「下单 → 小区可装性校验 → 智能派单 → 装维 SLA/赔付 → 套餐升级与流量监控」为主线，参考中国电信「美好家 / FTTR / 慢必赔」与中国移动「和家庭 / 承诺消费送宽带 / 流量提醒」等真实业务实践细化功能。

---

## 二、技术栈总览

> **架构决策（已锁定）**：采用 **单体架构** —— 后端为 **单一 Spring Boot 应用**，**不引入 Spring Cloud / 微服务 / 服务注册发现 / 分布式网关**；前端 PC 后台为 **Vue 3 单页应用**，经 Nginx 反向代理直连后端 `:8082`。后端业务模块 **上限 5 个**（当前 3 个，已封顶：community / install / product）。配置优先使用 Spring Boot 原生（`application.yml` + 环境变量），外部配置中心（含 Apollo）暂不引入。

| 端 | 技术选型 | 状态 |
|---|---|---|
| 微信小程序（客户端 / 师傅端） | 微信原生框架（WXML / WXSS / JS / app.json），utils/api.js 请求封装 | ✅ 骨架落地（7 页） |
| PC 后台（web-admin） | Vue 3 + Element Plus + Pinia + Vue Router + Vite（单页应用，直连后端） | ⬜ 规划中 |
| 后端服务 | Spring Boot 3.2.1 + MyBatis-Plus 3.5.5 + JDK 21 + Maven，单体应用，端口 8082 | ✅ 3 模块 / 13 接口（已封顶 ≤5） |
| 数据库 | MySQL（建表 SQL 已规划，驱动待接入） | ⬜ 规划中 |
| 权限 | Spring Security 6 + JWT（RBAC，单体内置） | ⬜ 规划中 |
| 配置 | Spring Boot 原生（application.yml + 环境变量） | ✅ 采用 |

---

## 三、目录结构

```
宽带业务管理系统/
├── broadband-design/index.html   # 功能页面设计稿（HTML 线框，v1.9）
├── broadband-miniapp/            # 微信小程序客户端
│   ├── app.js / app.json / app.wxss
│   ├── utils/api.js              # 接口封装（未实现走 mock）
│   └── pages/                    # 7 个页面（见第四节）
├── backend/                      # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/com/broadband/
│   │   ├── community/  install/  product/   # 3 个业务模块（install=派单+装维SLA；product=套餐+升级+流量）
│   ├── README.md / SLA.md / PKG_TRAFFIC.md               # 模块级说明
└── docs/                         # 本目录：4 份说明书 + 本总览
```

---

## 四、三端说明

### 4.1 微信小程序（客户端）— 7 个页面

| 页面 | 路径 | 说明 |
|---|---|---|
| 首页 | pages/index/index | 入口页 |
| 服务聚合 | pages/service/service | 九宫格服务入口（含「套餐升级」「流量监控」） |
| 融合套餐详情 | pages/package/detail/detail | 主图轮播 + 套餐组成 + 动态参数（单选/多选）+ 实时算价 + 立即办理 |
| 套餐升级 | pages/package/upgrade/upgrade | 带宽升档 / 加购多选 / 一次性补差预览 / 提交 |
| 可安装小区查询 | pages/community/query/query | 输入小区名查询是否可上门安装 |
| 安装地址 | pages/order/address/address | 填写安装地址（与小区查询 / 详情页联动） |
| 流量监控 | pages/traffic/traffic | 手机流量条 / 宽带时长 / 7 日趋势柱 / 加购流量包 |

> 设计稿规划客户端共 20 页；当前已落地 7 个核心骨架页，其余页面按设计稿迭代补充。

### 4.2 PC 后台（web-admin）— 规划中

设计稿 v1.9 已规划约 16 个后台模块（权限管理、性能监控、套餐管理、小区覆盖管理、安装工单 / 派单调度、worker 容量配置、流量监控、套餐升级管理等）。工程尚未建立，技术栈定型为 Vue 3 + Element Plus + Pinia。

### 4.3 后端服务 — 3 个模块 / 13 个接口

每个模块遵循「纯 Java 引擎（model + engine，可 `javac`+`java` 独立运行）+ Spring 适配层（Mapper / Service / Controller）」的同构分层。原 5 个模块已于 2026-09-14 合并为 3 个：

| 模块 | 由哪些原模块合并 | Java 文件 | 核心引擎 | REST 接口 | 验证 Demo |
|---|---|---|---|---|---|
| community 可安装小区 | community（独立保留） | 8 | CommunityEngine | `GET /api/community/check`<br>`POST /api/community/demand` | CommunityDemo |
| install 派单 + 装维SLA | dispatch + sla | 36 | CapacityPolicy / AdjacencyCluster / DispatchService / SlaEngine | `POST /api/dispatch/run`<br>`GET /api/dispatch/capacity`<br>`POST /api/sla/evaluate`<br>`POST /api/sla/evaluate-batch`<br>`GET /api/sla/board`<br>`GET /api/sla/rules`<br>`GET /api/sla/compensations` | DispatchDemo / SlaDemo |
| product 套餐 + 升级 + 流量 | pkg + traffic | 28 | PackageEngine / PackageUpgradeEngine / TrafficQueryEngine | `GET /api/package/detail`<br>`GET /api/package/upgrade-options`<br>`POST /api/package/upgrade`<br>`GET /api/traffic/usage` | PackageDemo / PackageUpgradeDemo / TrafficDemo |

> 接口总数：**13 个**（GET 8 / POST 5）。所有模块均带可独立运行的 `*Demo`（共 6 个），无需 Maven / 数据库即可验证业务算法。

---

## 五、配套文档清单

| 文档 | 说明 | 格式 |
|---|---|---|
| 系统架构说明书 | 总体分层架构、技术栈、模块划分、接口总览、数据模型、关键设计决策 | MD + DOCX |
| 功能进度说明书 | 客户端 / 师傅端 / PC 后台 / 后端逐功能状态与里程碑 | MD + DOCX |
| 系统规划说明书 | 愿景目标、Phase 1–4 路线图、技术演进、风险对策、验收标准 | MD + DOCX |
| 运维操作说明书 | 环境、本地验证命令、部署、配置模板、监控、故障排查、建表 SQL | MD + DOCX |
| 项目总览 README（本文档） | 一页式总览，串联上述所有产物 | MD + DOCX |

> 上述 4 份说明书 + 本总览均已生成 `.docx` 正式版，并归档至项目空间「宽带业务管理系统 / 文档」。

---

## 六、当前进度与里程碑

- ✅ **设计稿 v1.9**：覆盖客户端 20 页规划 + PC 后台 16 模块规划，含套餐升级与流量监控专章。
- ✅ **小程序骨架**：7 个核心页面 + 服务聚合入口 + api.js 封装（mock 兜底）。
- ✅ **后端 3 模块**：13 个接口 + 6 个可运行 Demo（已自 5 模块合并：install=dispatch+sla，product=pkg+traffic），已通过 `javac` 编译与 `java` 运行验证。
- ⬜ **数据库接入**：建表 SQL 已规划，待引入 MySQL 驱动并打通 Mapper。
- ⬜ **RBAC / 安全**：Spring Security 6 + JWT 方案已确认，待实现。
- ⬜ **PC 后台工程**：Vue 3 技术栈定型，待建工程并落地设计稿模块。
- ✅ **配置策略**：单体 Spring Boot 原生配置（application.yml + 环境变量 + Profile），不引入 Apollo 等外部配置中心（契合「仅 Spring Boot + Vue3」）。

---

## 七、本地运行与验证

后端各模块的纯 Java 引擎可脱离 Maven / 数据库独立验证（本机已知 JDK 21 可用，Maven 需自行安装）：

```bash
cd backend
# 编译全部纯 Java 类（排除需 MyBatis/Spring 的 mapper/spring 与 TrafficRecord）
find src/main/java/com/broadband -name "*.java" \
  | grep -v '/mapper/' | grep -v '/spring/' | grep -v 'TrafficRecord.java' \
  | tr '\n' '\0' | xargs -0 javac -d /tmp/bwall
# 运行任一模块 Demo（合并后包路径：community / install / product）
java -cp /tmp/bwall com.broadband.community.CommunityDemo
java -cp /tmp/bwall com.broadband.install.DispatchDemo
java -cp /tmp/bwall com.broadband.install.SlaDemo
java -cp /tmp/bwall com.broadband.product.PackageDemo
java -cp /tmp/bwall com.broadband.product.PackageUpgradeDemo
java -cp /tmp/bwall com.broadband.product.TrafficDemo
```

完整构建（含数据库与 Web 启动）与建表 SQL 详见 **《运维操作说明书》**；接口明细、数据模型与分层设计详见 **《系统架构说明书》**。

---

## 八、后续规划（摘要）

1. **数据落地**：引入 MySQL 驱动，打通 `traffic_usage` / `package_upgrade_order` 等表真实聚合与落库。
2. **安全与权限**：落地 Spring Security 6 + JWT，对齐 RBAC 角色（客户 / 销售 / 师傅 / 管理员）。
3. **PC 后台**：建立 Vue 3 工程，按设计稿 v1.9 实现首页 `/dashboard` 与派单 / 容量 / 流量监控等模块。
4. **配置治理**：配置项收口到 `application.yml` + Profile（不引入 Apollo），派单容量 / SLA 规则等常量集中管理。
5. **文档进阶**：为正式文档补充封面 / 页眉页脚 / 页码，输出统一版式。

> 详细路线图与验收标准见 **《系统规划说明书》**。
