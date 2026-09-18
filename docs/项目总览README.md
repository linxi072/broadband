# 宽带业务管理系统 · 项目总览 README

> 文档版本：V1.13 ｜ 更新日期：2026-09-16
> 适用对象：研发 / 产品 / 运营 / 实施 / 新成员 onboarding
> 一句话定位：一套面向「家庭宽带办理与装维运营」的三端一体化业务平台（微信小程序客户端 + 微信小程序师傅端 + Vue3 PC 后台 + Spring Boot 单体后端）。

---

## 一、系统简介

宽带业务管理系统覆盖 **客户端（微信小程序）**、**PC 后台管理系统（web-admin，Vue 3）** 与 **后端服务（Spring Boot 单体）** 三端，主线为：

`下单 → 小区可装性校验 → 智能派单 → 装维 SLA/赔付 → 套餐升级与流量监控 → 故障报修 → 评价/退款/发票`

参考中国电信「美好家 / FTTR / 慢必赔」与中国移动「和家庭 / 承诺消费送宽带 / 流量提醒」等真实业务实践细化功能。

### 1.1 三类用户

| 角色 | 核心诉求 | 端载体 |
|------|----------|--------|
| 终端客户（C 端） | 查覆盖、下单、升档、报修、评价、账单 | 微信小程序客户端（蓝 `#2563eb`） |
| 安装师傅 | 接单、施工、完工、测速、报修处理 | 微信小程序师傅端（橙 `#ea580c`） |
| 运营 / 客服 / 财务 | 订单/客户/套餐/派单/SLA/财务/权限 | PC 管理后台 web-admin（靛 `#4f46e5`） |

---

## 二、架构决策（已锁定）

- **单体架构**：后端为单一 Spring Boot 应用，**不引入 Spring Cloud / 微服务 / 服务注册发现 / 分布式网关**。
- **前端形态**：PC 后台为 Vue 3 单页应用，经 Nginx 反向代理直连后端 `:8082`。
- **模块上限**：后端业务模块上限 5 个，当前 4 个（community / install / product / system）。
- **配置策略**：优先使用 Spring Boot 原生（`application.yml` + 环境变量），外部配置中心（含 Apollo）暂不引入。
- **安全**：自研 HS256 JWT（零三方依赖）+ Spring Security 6（`STATELESS`）+ RBAC。

---

## 三、技术栈总览

| 端 | 技术选型 | 状态 |
|---|---|---|
| 微信小程序（客户端 / 师傅端） | 微信原生框架（WXML / WXSS / JS / app.json），utils/api.js 请求封装 | ✅ 客户端 15 页 + 师傅端 8 页（两独立工程） |
| PC 后台（web-admin） | Vue 3.5 + Element Plus（按需）+ Pinia + Vue Router + Vite 5 | ✅ 已落地（28 .vue / 23 路由） |
| 后端服务 | Spring Boot 3.2.1 + MyBatis-Plus（boot3）+ JDK 21 + Maven，单体，端口 8082 | ✅ 4 模块 / 62+ 接口（含 V1.13 报修） |
| 数据库 | MySQL 8.4（31 张表 + 幂等初始化脚本，启动自动建表） | ✅ 已接入 |
| 权限 | Spring Security 6 + 自研 JWT（HS256）+ RBAC | ✅ 5 角色 / 21 权限码 / 26 菜单 |
| 监控 | Actuator + Micrometer-Prometheus | ✅ `/actuator/prometheus` 已开放 |
| 配置 | Spring Boot 原生（yml + 环境变量） | ✅ 采用 |

> 接口数口径：规划基线 62 个，V1.11 交易闭环后扩展为 80+（含退款/发票/评价回填），V1.13 故障报修新增 `repair/*` 与字典/配置后台。

---

## 四、目录结构

```
宽带业务管理系统/
├── broadband-miniapp/            # 微信小程序客户端（蓝 #2563eb，15 页 + 登录态）
│   ├── app.js / app.json / app.wxss
│   ├── utils/api.js · auth.js    # 接口封装（Bearer 注入）+ 登录态校验
│   └── pages/                    # 首页/服务/套餐/订单/我的/报修 等
├── broadband-worker/             # 安装师傅端小程序（橙 #ea580c，8 页，独立工程）
│   ├── utils/api.js · auth.js
│   └── pages/                    # 首页/工单/完工/排班/路线 等
├── web-admin/                    # PC 运营后台（Vue 3 单页应用）
│   ├── vite.config.js            # Element Plus 按需 + /api 代理到 :8082
│   └── src/{api,layout,router,store,views}
├── backend/                      # Spring Boot 后端（单体，:8082）
│   ├── pom.xml
│   ├── src/main/java/com/broadband/{community,install,product,system,common}
│   ├── src/main/resources/{application.yml, db/schema.sql, db/data.sql}
│   └── README.md / SLA.md / PKG_TRAFFIC.md / RBAC.md
├── broadband-design/             # 设计稿 HTML
├── scripts/                      # loop_test.py / k6-load-test.js / prometheus/
└── docs/                        # 本文档所在
```

---

## 五、功能模块清单

### 5.1 后端服务模块（4 个业务模块 + common 公共层）

| 模块 | 职责 | 关键能力 |
|------|------|----------|
| `community` | 小区覆盖、需求登记 | 覆盖查询、需求登记（C 端开放） |
| `install` | 智能派单 + 装维 SLA | 容量规则、并查集聚类、贪心派单、SLA 时限/速率双评估、赔付 |
| `product` | 套餐/订单/客户/升级/流量/评价/发票 | 升级引擎、流量查询、业务闭环、客户 360、营销看板、退款/发票 |
| `system` | RBAC / 平台聚合 / 字典配置 | 自研 JWT、用户/角色/菜单 CRUD、CSV、数据权限行级隔离、数据字典、参数配置 |
| `common` | 公共层 | Ids 生成、全局异常、统一响应 |

### 5.2 三端页面清单（V1.13）

**客户端小程序（蓝 `#2563eb`，15 页 + 登录态）**：首页、服务聚合、套餐详情/列表/升级、流量监控、可装小区查询、安装地址、订单确认/列表/详情、地址管理、移机、续费提速、自助服务、智慧家庭、评价投诉、报修列表/详情/申请、个人中心/设置/登录。

**安装师傅端小程序（橙 `#ea580c`，8 页）**：首页（今日工单）、我的工单 + 详情、完工确认/测速、排班与容量、工单路线、个人中心/登录。

**PC 后台（web-admin，靛 `#4f46e5`，28 .vue / 23 路由）**：数据看板、订单/客户/套餐/套餐升级/小区、工单池/派单调度/容量配置/调度规则/装维 SLA 赔付、流量/评价/销售/财务、用户/角色/菜单/日志/性能监控、数据字典/参数配置。

---

## 六、快速开始

### 6.1 后端启动

```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
MVN=~/Documents/localRepository/devRepository/maven/bin/mvn
$MVN clean package -DskipTests

# 启动（务必用环境变量覆盖数据库凭据）
export DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=broadband \
       DB_USERNAME=broadband DB_PASSWORD=broadband
java -jar target/broadband-backend-1.0.0.jar --server.port=8082
```

> ⚠️ 必须用 `java -jar` + `--server.port=8082`，**勿用** `mvn spring-boot:run`（沙箱会注入随机端口）。

### 6.2 前端（web-admin）构建

```bash
cd web-admin
npm install --registry=https://registry.npmmirror.com
npm run build        # 产出 dist/，由 Nginx 托管
# 开发态：npm run dev  （http://127.0.0.1:5173，Vite 代理 /api → 8082）
```

### 6.3 小程序

用微信开发者工具分别打开 `broadband-miniapp/`（客户端）与 `broadband-worker/`（师傅端），在 `app.js` 的 `globalData.baseUrl` 填入后端地址（`http://<本机IP>:8082`）。客户端 AppID 已回填 `wxcc344ad7f4f849d4`；师傅端为独立 AppID（提审前另注册）。

---

## 七、默认账号

> ⚠️ 生产环境务必首次登录后立即改密，并通过环境变量覆盖 `app.jwt.secret`。

| 账号 | 密码 | 角色 | 可见菜单 | 权限码 |
|---|---|---|---|---|
| admin | admin123 | ADMIN 超级管理员 | 26 | 21 |
| liuwei | liuwei123 | OPERATOR 运营专员 | 19 | 15 |
| zhaomin | zhaomin123 | FINANCE 财务 | 4 | 4 |
| wangfang | wangfang123 | CS 客服 | 4 | 4 |

---

## 八、关键指标

| 维度 | 已完成 | 覆盖率 |
|---|---|---|
| 后端业务模块 | 4（+1 公共层） | 100%（上限 5） |
| 后端 REST 接口 | 80+（全部连库可用） | 100% |
| 数据库表 | 31 张 + 种子数据 | 100% |
| 安全 RBAC | 5 角色 / 26 菜单 / 21 权限码 | 100% |
| PC 后台模块 | 28 .vue / 23 路由 | 100% |
| 客户端小程序页 | 15（含登录） | 100% |
| 师傅端小程序页 | 8（含登录） | 100% |

**端到端验证**：业务闭环 `scripts/loop_test.py` 36 项断言全绿；师傅端隔离 13 项全绿；安全鉴权矩阵（无 token→401 / 越权→403 / 授权→200）实测通过；`JwtUtilTest` 7 条单测全绿。

---

## 九、配套文档索引

| 文档 | 说明 |
|---|---|
| 系统架构说明书 | 整体架构、模块职责、部署拓扑、非功能设计 |
| 系统规划说明书 | 规划路线、建设目标、分阶段演进、风险对策 |
| 数据结构说明书 | 31 张表逐表字段、关系、字典与参数种子 |
| 运维操作说明书 | 环境、构建、部署、配置、监控、故障排查、备份 |
| 项目进度与开发计划 | 里程碑、版本变更、剩余缺口、排期 |
| 系统页面原型图 | PC 后台 / 客户端 / 师傅端 代表性页面线框原型 |
| UI 与交互设计 | 三端同源异色设计系统、令牌、组件规范 |
| 迭代与需求 | 迭代路线图、V1.10~V1.13 变更、客户端差距矩阵 |

---

© 宽带业务管理系统 · 项目总览
