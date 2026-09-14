# 宽带业务管理系统 · 项目总览

> 文档版本：v1.1 ｜ 更新日期：2026-09-14 ｜ 关联设计稿：broadband-design/index.html（v1.9）
>
> **v1.1 变更**：PC 后台 `web-admin`（Vue 3）与本后端安全体系（Spring Security 6 + JWT + RBAC）落地；
> 后端模块 3→4（新增 `system`）、接口 13→45、表 17→25。

## 一、项目简介

宽带业务管理系统是一套面向「家庭宽带办理与装维运营」的业务平台，覆盖 **客户端（微信小程序）**、**PC 后台管理系统（web-admin，Vue 3，已落地）** 与 **后端服务（Spring Boot 单体）** 三端。系统以「下单 → 小区可装性校验 → 智能派单 → 装维 SLA/赔付 → 套餐升级与流量监控」为主线，参考中国电信「美好家 / FTTR / 慢必赔」与中国移动「和家庭 / 承诺消费送宽带 / 流量提醒」等真实业务实践细化功能。

---

## 二、技术栈总览

> **架构决策（已锁定）**：采用 **单体架构** —— 后端为 **单一 Spring Boot 应用**，**不引入 Spring Cloud / 微服务 / 服务注册发现 / 分布式网关**；前端 PC 后台为 **Vue 3 单页应用**，经 Nginx 反向代理直连后端 `:8082`。后端业务模块 **上限 5 个**（当前 4 个：community / install / product / system）。配置优先使用 Spring Boot 原生（`application.yml` + 环境变量），外部配置中心（含 Apollo）暂不引入。

| 端 | 技术选型 | 状态 |
|---|---|---|
| 微信小程序（客户端 / 师傅端） | 微信原生框架（WXML / WXSS / JS / app.json），utils/api.js 请求封装 | 🟡 客户端骨架落地（7 页）；师傅端待建 |
| PC 后台（web-admin） | Vue 3 + Element Plus（按需引入）+ Pinia + Vue Router + Vite | ✅ 已落地（22 业务页 / 22 路由） |
| 后端服务 | Spring Boot 3.2.1 + MyBatis-Plus（boot3 starter）+ JDK 21 + Maven，单体应用，端口 8082 | ✅ 4 模块 / 45 接口 |
| 数据库 | MySQL 8.4（25 张表 + 幂等初始化脚本，启动自动建表） | ✅ 已接入 |
| 权限 | Spring Security 6 + 自研 JWT（HS256，零三方依赖）+ RBAC | ✅ 已落地（5 角色 / 21 权限码） |
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
├── web-admin/                    # ★ PC 运营后台（Vue 3 单页应用）
│   ├── vite.config.js            # 按需引入 Element Plus + /api 代理到 :8082
│   ├── .env.development / .env.production
│   └── src/
│       ├── api/                  # request.js（token 注入 + 401 处理）+ auth/business/system
│       ├── layout/               # 侧边栏（后端菜单树驱动）/ 顶栏 / 面包屑
│       ├── router/               # routes.js（22 条路由，meta.perm 与后端权限码同源）
│       ├── store/                # user（token + profile + menus）/ app
│       └── views/                # 25 个 .vue 页面
├── backend/                      # Spring Boot 后端（单体）
│   ├── pom.xml
│   ├── src/main/java/com/broadband/
│   │   ├── community/  install/  product/  system/   # 4 个业务模块 + common 公共层
│   ├── src/main/resources/
│   │   ├── application.yml        # 数据源/端口（环境变量注入，密码不落盘）
│   │   └── db/schema.sql + data.sql  # 25 张表（幂等，启动自动执行）
│   └── README.md / SLA.md / PKG_TRAFFIC.md / RBAC.md   # 模块级说明
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

### 4.2 PC 后台（web-admin）— 已落地

Vue 3 + Element Plus（按需引入，未全量引样式）+ Pinia + Vue Router（history 模式）+ Vite。设计稿 v1.9 规划的
16 个后台模块已全部落地为 **22 个业务页面 / 22 条路由**（另含登录页与 403 / 404）：

| 分组 | 页面 |
|---|---|
| 经营总览 | 数据看板 `/dashboard` |
| 业务办理 | 订单 `/order`、客户 `/customer`（含分层与升档抽屉）、套餐 `/package`、套餐编辑 `/package/edit`、套餐升级 `/package/upgrade`、小区 `/community`、小区编辑 `/community/edit` |
| 装维履约 | 工单池 `/workorder/pool`、派单调度 `/workorder/dispatch`、容量配置 `/workorder/capacity`、调度规则 `/workorder/rules`、装维 SLA 与赔付 `/sla` |
| 运营支撑 | 流量监控 `/traffic`、投诉与评价 `/review`、销售管理 `/sales`、财务管理 `/finance` |
| 系统管理 | 用户 `/system/user`、角色 `/system/role`、菜单 `/system/menu`、操作日志 `/system/log`、性能监控 `/monitor` |

关键设计：① **菜单树由后端驱动**——侧边栏不写死，登录后取 `/api/auth/me` 的 `menus` 渲染，与 `@PreAuthorize` 权限码同源；
② **路由守卫 + `v-perm` 指令**——无 token 跳登录，越权菜单不渲染；
③ **演示兜底**——接口不可达时降级为内置演示数据并标注「演示数据」，保证演示不中断。

### 4.3 后端服务 — 4 个模块 / 45 个接口

每个模块遵循「纯 Java 引擎（model + engine，可 `javac`+`java` 独立运行）+ Spring 适配层（Mapper / Service / Controller）」的同构分层。

| 模块 | 职责 | Java 文件 | 核心引擎 | 接口数 |
|---|---|---|---|---|
| community | 可安装小区与需求登记 | 9 | CommunityChecker | 4 |
| install | 派单调度 + 装维 SLA 与赔付 | 37 | CapacityPolicy / AdjacencyCluster / DispatchService / SlaEngine | 11 |
| product | 套餐 / 升级 / 流量 / 客户 | 35 | PackageEngine / PackageUpgradeEngine / TrafficQueryEngine | 12 |
| system | RBAC 权限 + 后台聚合与监控 | 23 | —（JWT + Spring Security + JdbcTemplate 聚合） | 18 |
| common | 主键生成 / 全局异常处理 | 2 | — | — |

> 接口总数 **45 个**（GET 28 / POST 12 / PUT 5），全部读写真实 MySQL 数据。
> 核心算法模块仍保留可独立运行的 `*Demo`（共 6 个），无需 Maven / 数据库即可验证业务算法。

### 4.4 数据库与安全

- **25 张表**：业务域（community / work_order / worker / worker_capacity / sla_rule / sla_record / compensation /
  customer / customer_contract / package_info / package_image / package_converge_item / package_param /
  package_param_option / traffic_usage / package_upgrade_order / biz_order / review / community_demand）+
  权限域（sys_user / sys_role / sys_menu / sys_user_role / sys_role_menu / sys_oper_log）。
- **初始化幂等**：`CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`，随启动执行，不覆盖运行期数据。
- **鉴权**：Spring Security 6 无状态 JWT。**角色与权限码每请求实时查库**（不固化在 token 内），
  因此后台改角色/授权可即时生效。开放层（小程序 c 端接口）默认放行，可用 `app.security.protect-client-api=true` 收口。

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
- ✅ **小程序骨架**：7 个核心页面 + 服务聚合入口 + api.js 封装（后端不可达时 mock 兜底）。
- ✅ **后端 4 模块 / 45 接口**：community / install / product / system，核心算法保留 6 个可独立运行的 Demo。
- ✅ **数据库接入**：MySQL 8.4，25 张表 + 幂等 `schema.sql` / `data.sql`，启动自动初始化。
- ✅ **PC 后台**：`web-admin`（Vue 3 + Element Plus）22 业务页 / 22 路由，全量对接真实接口，侧边栏由后端菜单树驱动。
- ✅ **RBAC / 安全**：Spring Security 6 + 自研 JWT（HS256），5 角色 / 26 菜单 / 21 权限码；
  实测鉴权矩阵 —— 无 token → 401、越权 → 403、授权内 → 200；含 7 条 JWT 回归单测。
- ⬜ **小程序补页**：客户端剩余约 13 页 + 师傅端 8 页。
- ✅ **配置策略**：单体 Spring Boot 原生配置（application.yml + 环境变量），不引入 Apollo 等外部配置中心。

---

## 七、本地运行与验证

### 7.1 后端（Maven + MySQL，完整链路）

```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
# 本机 Maven 不在 PATH 时使用绝对路径
MVN=~/Documents/localRepository/devRepository/maven/bin/mvn
$MVN clean package            # 含 7 条 JWT 回归单测

DB_USERNAME=broadband DB_PASSWORD=****** \
  java -jar target/broadband-backend-1.0.0.jar --server.port=8082
# 启动即自动建库建表（幂等），无需手工执行 SQL
```

冒烟：

```bash
TOK=$(curl -s -X POST http://127.0.0.1:8082/api/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"username":"admin","password":"admin123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
curl -s -H "Authorization: Bearer $TOK" http://127.0.0.1:8082/api/auth/me | head -c 300
```

### 7.2 PC 后台（Vite）

```bash
cd web-admin
npm install
npm run dev      # http://127.0.0.1:5173 ，/api 自动代理到 :8082
npm run build    # 产物 dist/，生产交 Nginx 反代（见《运维操作说明书》）
```

### 7.3 纯算法验证（无需 Maven / 数据库）

后端核心引擎可脱离框架独立编译运行：

```bash
cd backend
find src/main/java/com/broadband -name "*.java" \
  | grep -v '/mapper/' | grep -v '/spring/' | grep -v 'TrafficRecord.java' \
  | tr '\n' '\0' | xargs -0 javac -d /tmp/bwall
java -cp /tmp/bwall com.broadband.install.DispatchDemo
java -cp /tmp/bwall com.broadband.install.SlaDemo
java -cp /tmp/bwall com.broadband.product.PackageUpgradeDemo
```

建表 SQL、配置模板、Nginx 与故障排查详见 **《运维操作说明书》**；接口明细、数据模型与分层设计详见 **《系统架构说明书》**。

---

## 八、后续规划（摘要）

1. **小程序补页**：客户端剩余约 13 页（新装/移机/续费/提速/报修/智慧家庭/自助服务/评价投诉）+ 师傅端 8 页（工单/排班容量/路线/完工/测速/评价）。
2. **小程序登录态**：接入微信 session 换取 token 后，将 `app.security.protect-client-api` 置 `true`，把 c 端开放接口一并纳入鉴权。
3. **业务增强**：工单状态机流转与改派、SLA 规则在线编辑、套餐上下架审批流。
4. **交付增强**：性能监控指标细化（QPS/耗时分位）、操作日志归档策略、CI 流水线。
5. **文档进阶**：为正式文档补充封面 / 页眉页脚 / 页码，输出统一版式。

> 详细路线图与验收标准见 **《系统规划说明书》**。
