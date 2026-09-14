# 宽带业务管理系统（broadband）

宽带办理小程序 + PC 后端管理系统的工程代码与配套文档。

## 技术架构（已锁定）

- **后端**：单体 **Spring Boot 3.2.1** 应用（JDK 21 / MyBatis-Plus 3.5.5 / Maven，端口 8082），**不引入 Spring Cloud / 微服务 / 服务注册发现 / 分布式网关**。
- **前端 PC 后台**：**Vue 3** 单页应用（Element Plus + Pinia + Vue Router + Vite），经 Nginx 反向代理直连后端。
- **小程序**：微信原生框架（WXML / WXSS / JS）。
- **配置**：Spring Boot 原生（`application.yml` + 环境变量），**不引入 Apollo 等外部配置中心**。
- **后端业务模块（≤5，当前 3）**：`community`（可安装小区）、`install`（派单 + 装维 SLA，含原 dispatch/sla）、`product`（融合套餐 + 升级 + 流量，含原 pkg/traffic）。

## 目录结构

```
backend/               # Spring Boot 后端（3 模块 / 13 接口 / 6 个可运行 Demo）
broadband-miniapp/     # 微信小程序（客户端 + 师傅端，已注册 7 页骨架）
broadband-design/      # 功能页面设计稿（index.html，v1.9）
docs/                  # 系统说明书套件（架构/进度/规划/运维/总览，含 .md 与 .docx）
```

## 后端接口（13 个，GET 8 / POST 5）

- `community`：`GET /api/community/check`、`POST /api/community/demand`
- `dispatch`：`POST /api/dispatch/run`、`GET /api/dispatch/capacity`
- `package`：`GET /api/package/detail`、`GET /api/package/upgrade-options`、`POST /api/package/upgrade`
- `sla`：`POST /api/sla/evaluate`、`POST /api/sla/evaluate-batch`、`GET /api/sla/board`、`GET /api/sla/rules`、`GET /api/sla/compensations`
- `traffic`：`GET /api/traffic/usage`

## 本地验证（无需 Maven / 数据库）

每个模块均带可独立运行的 `*Demo`（纯 Java 引擎，`javac`+`java` 验证）：

```bash
cd backend
find src/main/java/com/broadband -name "*.java" \
  | grep -v '/mapper/' | grep -v '/spring/' | grep -v 'TrafficRecord.java' \
  | tr '\n' '\0' | xargs -0 javac -d /tmp/bwall
java -cp /tmp/bwall com.broadband.community.CommunityDemo
java -cp /tmp/bwall com.broadband.install.DispatchDemo
java -cp /tmp/bwall com.broadband.install.SlaDemo
java -cp /tmp/bwall com.broadband.product.PackageDemo
java -cp /tmp/bwall com.broadband.product.PackageUpgradeDemo
java -cp /tmp/bwall com.broadband.product.TrafficDemo
```

## 文档

详见 `docs/项目总览README.md`，含系统架构 / 功能进度 / 系统规划 / 运维操作说明书与项目总览。

## 提交规范

每次变更必须提交，并在提交信息中**注明修改了哪些内容、为什么改**（遵循 Conventional Commits：`refactor` / `feat` / `docs` / `fix` / `chore` 等，正文写原因）。
