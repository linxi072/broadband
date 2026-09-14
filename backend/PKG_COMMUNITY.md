# 套餐详情 & 小区可装校验 模块说明

宽带业务系统后端新增两个与小程序端联动的模块，已落地真实可编译代码（Spring Boot 3.2.1 + MyBatis-Plus + JDK 21）：

| 模块 | 包路径 | 说明 |
|---|---|---|
| 融合套餐详情 | `com.broadband.pkg` | `GET /api/package/detail` 返回主图/轮播图/套餐组成/动态可选参数 |
| 小区可装校验 | `com.broadband.community` | `GET /api/community/check` + `POST /api/community/demand`，复用 dispatch 的小区模型 |

## 一、融合套餐详情（com.broadband.pkg）

### 接口
```
GET /api/package/detail?id=pkg-500
```
返回 `PackageDetailVO`：
```json
{
  "id": "pkg-500", "name": "500M 融合套餐", "category": "融合套餐",
  "monthlyFee": 79, "originalFee": 129, "status": "ON_SHELF",
  "deposit": 200, "deviceRent": 10,
  "penalty": "合约期内提前解约，按剩余月份 30% 赔付违约金",
  "slaInfo": "城区当日装当日修，超时自动赔付",
  "images": [ {"type":"MAIN","url":"https://cdn/x/main.jpg","sortOrder":1}, ... ],
  "converge": [ {"label":"宽带","desc":"500M 高速宽带"}, ... ],
  "params": [
    {"key":"bandwidth","name":"宽带速率","type":"SINGLE","required":true,
     "options":[{"value":"500M","extraFee":0},{"value":"1000M","extraFee":30}]},
    {"key":"contract","name":"合约期","type":"SINGLE","required":true,
     "options":[{"value":"12个月","extraFee":0}]},
    {"key":"addon","name":"增值服务","type":"MULTI","required":false,
     "options":[{"value":"FTTR全屋光纤","extraFee":30}]}
  ]
}
```
> 总价由小程序端按「月租 + 多选加价」实时计算（见 `pages/package/detail`）。

### 代码结构（与 dispatch / sla 同构）
- `model/`：`PackageInfo` / `PackageImage` / `PackageParam` / `PackageParamOption` / `PackageConvergeItem` / `PackageDetailVO` / `PackageParamVO`
- `mapper/`：5 个 MyBatis-Plus Mapper
- `spring/`：`PackageServiceApi` / `PackageServiceImpl`（DB 聚合 → VO）/ `PackageController`
- `PackageDemo`（纯 Java 可运行，验证 VO 组装 + 价格计算）

## 二、小区可装校验（com.broadband.community）

### 接口
```
GET /api/community/check?name=南山科技园
```
返回 `CommunityCheckResult`：
```json
{
  "name": "南山科技园", "region": "南山",
  "installable": true, "portRemaining": 95, "portStatus": "AVAILABLE",
  "message": "可安装（端口余量 95）", "canProceed": true,
  "suggestion": "可立即办理",
  "packages": [ {"id":"pkg-500","name":"500M 融合套餐"} ]
}
```
`portStatus` 三态：`AVAILABLE`（可装）/ `TIGHT`（端口紧张，放行但强提示）/ `UNAVAILABLE`（未覆盖或端口占满 → **拦截下单**）。

```
POST /api/community/demand
{ "name":"火星小区", "contact":"张三", "phone":"138xxxx", "note":"" }
→ { "id":"...", "message":"需求已登记，覆盖后第一时间通知您" }
```

### 判定引擎（纯 Java，可单测）
`engine/CommunityChecker.evaluate(Community, onShelfPackages)`：
- 不在库 / `installable=false` / 端口余量≤0 → `UNAVAILABLE`
- 余量 ≤ `TIGHT_THRESHOLD`(=2) → `TIGHT`
- 否则 → `AVAILABLE`
- `canProceed = portStatus != UNAVAILABLE`（前端据此拦截）

### 代码结构
- `model/`：`CommunityCheckResult`（含 `PackageRef` 内部类）/ `CommunityDemand`
- `engine/CommunityChecker`（纯算法）
- `mapper/CommunityDemandMapper`
- `spring/`：`CommunityServiceApi` / `CommunityServiceImpl`（复用 `dispatch.CommunityMapper` + `pkg.PackageMapper`）/ `CommunityController`
- `CommunityDemo`（纯 Java 可运行，覆盖 4 种场景）

## 三、与小程序联动
- `pages/community/query`：调用 `/api/community/check`，可装则「立即办理」跳 `package/detail` 并预填小区
- `pages/order/address`：填地址时复用 `/api/community/check` 实时校验，不可装则拦截并提示登记/换小区
- `pages/package/detail`：「立即办理」跳 `order/address` 并携带 pkgId + community
- `utils/api.js`：`getPackageDetail` / `checkCommunity` / `registerDemand` / `evaluateSla`
- 后端未就绪时，小程序各页均有本地 mock 兜底，保证骨架可跑

## 四、建表参考（MySQL 风格）
```sql
CREATE TABLE package (
  id VARCHAR(32) PK, name VARCHAR(64), category VARCHAR(32),
  monthly_fee INT, original_fee INT, status VARCHAR(16),
  deposit INT, device_rent INT, penalty VARCHAR(255), sla_info VARCHAR(255)
);
CREATE TABLE package_image (id VARCHAR(32) PK, package_id VARCHAR(32), type VARCHAR(16), url VARCHAR(512), sort_order INT);
CREATE TABLE package_param (id VARCHAR(32) PK, package_id VARCHAR(32), group_key VARCHAR(32), name VARCHAR(64), type VARCHAR(16), required TINYINT, sort_order INT);
CREATE TABLE package_param_option (id VARCHAR(32) PK, param_id VARCHAR(32), value VARCHAR(64), extra_fee INT, sort_order INT);
CREATE TABLE package_converge (id VARCHAR(32) PK, package_id VARCHAR(32), label VARCHAR(64), desc VARCHAR(255), sort_order INT);
CREATE TABLE community_demand (id VARCHAR(32) PK, name VARCHAR(64), contact VARCHAR(32), phone VARCHAR(32), note VARCHAR(255), created_at BIGINT, status VARCHAR(16));
-- community 表见 dispatch 模块（id/name/region/street/latitude/longitude/installable/port_total/port_used）
```

## 五、验证
- 纯 Java 部分已 `javac` 编译 + 运行：`PackageDemo`（VO 组装 + 月费计算 ¥119）、`CommunityDemo`（AVAILABLE/TIGHT/UNAVAILABLE/未收录 四态正确）。
- Spring 适配层依赖 MyBatis-Plus / Spring（需 `mvn` 本地仓库），结构与 dispatch / sla 模块一致，复制进业务工程即可。
