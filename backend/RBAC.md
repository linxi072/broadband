# 后端模块 · RBAC 权限与平台聚合（system）

> 对应模块：`backend/src/main/java/com/broadband/system/` ｜ 版本：v1.0 ｜ 日期：2026-09-14
> 关联：`SecurityConfig` / `JwtUtil` / `JwtAuthFilter` / `AuthController` / `Sys*Controller` / `AdminPlatformController`

本模块是后端的第 4 个业务模块（上限 5），承载两件事：

1. **认证与授权（RBAC）**：登录签发 JWT、请求鉴权、方法级授权、操作审计；
2. **平台聚合**：后台看板/订单/监控/报表类只读查询（跨业务域，故集中在此，避免污染各业务模块）。

---

## 1. 包结构

```
com.broadband.system/
├── model/          SysUser / SysRole / SysMenu / SysOperLog / SysUserVO / LoginUser
├── mapper/         SysUserMapper / SysRoleMapper / SysMenuMapper / SysOperLogMapper
├── security/       JwtUtil / JwtAuthFilter / SecurityConfig / RestAuthHandlers
└── spring/         AuthController / SysUserController / SysRoleController
                    SysMenuController / SysOperLogController
                    AdminPlatformController / RbacInitializer / OperLogService / ApiMetricsFilter
```

---

## 2. 认证流程

```
POST /api/auth/login
  ├─ 参数校验 → 失败：记录 sys_oper_log(失败·参数缺失) + 401
  ├─ 按用户名查 sys_user → BCrypt 校验密码 → 失败：记录日志 + 401
  ├─ 校验 status=ENABLED → 否：记录日志 + 401（账号已禁用）
  ├─ JwtUtil.issue(username, userId, name, dept) → HS256 token（TTL 默认 120 分钟）
  └─ 记录 sys_oper_log(成功) → 返回 { token, expiresIn, user }

后续请求：Authorization: Bearer <token>
  └─ JwtAuthFilter
       ├─ verify(token)：签名（常量时间比较）+ 过期校验 + 扁平 JSON 声明解析
       ├─ 按 sub 查库载入用户、角色（ROLE_ 前缀）、权限码
       ├─ 写入 SecurityContext（principal = LoginUser）
       └─ 失败：WARN 日志留痕 + clearContext → 由 EntryPoint 统一 401
```

**为什么角色/权限不放进 token**：token 只承载会话标识。角色与权限**每请求实时查库**，
因此后台改授权**立即生效**，无需用户重新登录，也避免"权限固化进 token"的经典难题。

---

## 3. 授权分层（有意设计）

| 层 | 范围 | 规则 |
|---|---|---|
| 开放层 | `/api/package/detail`、`/api/package/upgrade-options`、`/api/package/upgrade`、`/api/community/check`、`/api/community/demand`、`/api/traffic/usage` | 放行（小程序 c 端接口，登录态未落地） |
| 受保护层 | 其余全部（`/api/dispatch/**`、`/api/sla/**`、`/api/admin/**`、`/api/system/**`、`/api/auth/me`） | 必须登录，且按权限码 `@PreAuthorize` 细粒度授权 |

- 开关：`app.security.protect-client-api=true` 即让开放层也强制鉴权（小程序接入 token 后切换）。
- 语义：**401 = 未认证/凭证失效**；**403 = 已认证但无权限**。排查时先看是哪个。

---

## 4. 权限码（21 个）

`dashboard:view`、`order:view`、`customer:view`、`package:view`、`package:edit`、`upgrade:view`、
`community:view`、`community:edit`、`workorder:view`、`dispatch:run`、`capacity:config`、
`sla:view`、`traffic:view`、`review:view`、`sales:view`、`finance:view`、
`system:user`、`system:role`、`system:menu`、`system:log`、`monitor:view`

**三处同源**（改动必须同步）：

| 位置 | 载体 |
|---|---|
| 数据库 | `sys_menu.perm`（权限码字典，菜单树与角色授权均引用它） |
| 后端 | `@PreAuthorize("hasAuthority('xxx')")` |
| 前端 | `web-admin/src/router/routes.js` 的 `meta.perm`（菜单由后端下发，此处用于按钮级 `v-perm`） |

---

## 5. 角色与默认账号

| 角色码 | 名称 | 菜单 | 权限码 | 说明 |
|---|---|---|---|---|
| `ADMIN` | 超级管理员 | 26 | 21 | 全部模块 |
| `OPERATOR` | 运营专员 | 19 | 15 | 全部业务模块（不含系统管理） |
| `FINANCE` | 财务 | 4 | 4 | 看板 / 订单 / 客户 / 财务 |
| `CS` | 客服 | 4 | 4 | 看板 / 订单 / 客户 / 投诉评价 |
| `SALES` | 销售 | — | — | 种子角色（看板 / 订单 / 客户 / 销售） |

默认账号与初始密码见《运维操作说明书》第 7 节。密码由 `RbacInitializer` 在启动时以 **BCrypt** 幂等写入
（`data.sql` 只插账号骨架、不落哈希；已有密码不覆盖，用户改过的密码不会被启动流程重置）。

---

## 6. 接口清单（18 个）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 开放 | 登录签发 JWT |
| GET | `/api/auth/me` | 登录 | 当前用户 + 角色 + 权限码 + 菜单树 |
| POST | `/api/auth/logout` | 登录 | 无状态登出（前端丢弃 token，此处仅留痕） |
| GET | `/api/system/users` | `system:user` | 用户列表（分页 + 关键字） |
| POST | `/api/system/users` | `system:user` | 新增用户（可带角色） |
| PUT | `/api/system/users/{id}/status` | `system:user` | 启用 / 禁用 |
| PUT | `/api/system/users/{id}/roles` | `system:user` | 分配角色 |
| PUT | `/api/system/users/{id}/password` | `system:user` | 重置密码（BCrypt） |
| GET | `/api/system/roles` | `system:role` | 角色列表 |
| GET | `/api/system/roles/{id}/menus` | `system:role` | 角色已授权菜单 |
| PUT | `/api/system/roles/{id}/menus` | `system:role` | 保存角色菜单授权 |
| GET | `/api/system/menus/tree` | `system:menu` | 完整权限树 |
| GET | `/api/system/logs` | `system:log` | 操作日志（分页） |
| GET | `/api/admin/dashboard/stats` | `dashboard:view` | 看板指标聚合 |
| GET | `/api/admin/dashboard/recent-orders` | `dashboard:view` | 看板最近订单 |
| GET | `/api/admin/orders` | `order:view` | 订单列表（状态/关键字） |
| GET | `/api/admin/traffic/overview` | `traffic:view` | 流量总览 |
| GET | `/api/admin/monitor/overview` | `monitor:view` | 运行监控（JVM/内存/接口往返/DB 连通） |

---

## 7. 关键实现说明

### 7.1 自研 JWT（`JwtUtil`）

- **零三方依赖**：仅用 JDK 的 `Mac` + `Base64`，可脱离 Spring 独立编译与单测（契合「算法与框架解耦」原则）。
- token 结构：`base64url(header).base64url(payload).base64url(HMAC-SHA256(前两段))`。
- 校验：签名用**常量时间比较**；随后校验 `exp`；payload 用内置的扁平 JSON 解析器读取。
- ⚠️ **数字字面量解析坑（已修复并加单测）**：曾把数字扫描写成 `"-+.0-9eE".indexOf(c)`，
  那只是「若干单个字符」而非 `0-9` 区间，只放行 `0` 与 `9`，导致 `iat`/`exp` 解析失败。
  现由 `isNumberChar()`（`Character.isDigit` 判定）实现，`JwtUtilTest` 覆盖 `0–9` 全数字与多位数。

### 7.2 实体零 ORM 注解

`SysUser` / `SysRole` / `SysMenu` / `SysOperLog` 均为纯 POJO，不带 `@TableName` / `@TableId`。
**为什么**：`@MapperScan(annotationClass = Mapper.class)` 只扫显式标注 `@Mapper` 的接口；
实体的表名依赖 `table-underline` + `map-underscore-to-camel-case` 推导，主键策略不参与，
因此实体可被纯 Java 编译（`com.broadband.install.model.WorkerCapacity` 启动时有一条
`Not found @TableId annotation` 的 WARN，属预期，不影响运行）。

### 7.3 登录失败信息不泄露账号存在性

用户名不存在与密码错误统一返回「账号或密码错误」；但**审计日志区分**（`失败(账号或密码错误)`），
便于运维排查又不给攻击者提供枚举信息。

### 7.4 操作审计（`OperLogService`）

记录：`username`、`name`、`action`、`target`、`method`、`ip`、`result`、`cost_ms`、`created_time`。
覆盖登录成功/失败、账号禁用、token 无效访问、写操作。IP 由 `RestAuthHandlers.clientIp()` 解析
（优先 `X-Forwarded-For`，兼容 Nginx 反代）。

### 7.5 平台聚合用 JdbcTemplate

看板/报表类查询跨多表且需要别名对齐前端字段，直接写 SQL 比 ORM 更直观可控。
⚠️ **联表查询必须给列加表别名**：曾因 `biz_order o LEFT JOIN package_info pkg` 而 `SELECT id` 未限定，
返回 500（`Column 'id' in field list is ambiguous`）。所有列现均以 `o.` / `pkg.` 前缀限定。

---

## 8. 验证方式

```bash
cd backend
MVN=~/Documents/localRepository/devRepository/maven/bin/mvn
$MVN clean package                 # 含 JwtUtilTest（7 条用例）
```

实测鉴权矩阵：

| 场景 | 期望 | 实测 |
|---|---|---|
| 无 token 访问 `/api/sla/rules` | 401 | ✅ 401 |
| admin 登录 → 22 个受保护接口 | 全 200 | ✅ 全 200 |
| zhaomin（财务）访问 `/api/sla/rules`、`/api/system/users`、`/api/admin/monitor/overview` | 403 | ✅ 403 |
| zhaomin 访问 `/api/admin/finance/report`、`/api/admin/orders`、`/api/admin/dashboard/stats` | 200 | ✅ 200 |
| wangfang（客服）访问 `/api/admin/reviews`；访问 `/api/sla/rules`、`/api/admin/finance/report` | 200 / 403 | ✅ 200 / 403 |
| 各角色菜单数（admin / liuwei / zhaomin / wangfang） | 26 / 19 / 4 / 4 | ✅ 一致 |
