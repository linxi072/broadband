# 监控与告警部署指南（R8 收口 · 阶段 5 M15 上线保障）

> 配套：M15-DEP `deploy/nginx.conf`（actuator 内网限制）、`application-prod.yml`（prometheus 暴露）
> 本目录为配置交付物，随 B1 提交评审；部署时请放置到服务器 `deploy/monitoring/`。

---

## 一、组件架构

```
┌────────────┐   scrape (/actuator/prometheus, 内网)   ┌──────────────┐
│ broadband  │ ───────────────────────────────────────▶│  Prometheus  │
│  backend   │                                         └──────┬───────┘
│  :8082     │                                              │ 规则评估
└────────────┘                                             ▼
                                           ┌──────────────┐   ┌─────────┐
                                           │ Alertmanager │──▶│ 通知(邮件/ │
                                           └──────────────┘   │ 企微/Webhook)│
                                                                └─────────┘
                                           ┌──────────────┐
                                           │   Grafana    │◀── 运维看板
                                           │   :3000      │
                                           └──────────────┘
```

- **Prometheus**：抓取后端 `/actuator/prometheus`（Micrometer 暴露的 JVM / HTTP / 业务指标）。
- **Alertmanager**：接收 Prometheus 告警，路由到邮件 / 企业微信 webhook。
- **Grafana**：可视化看板（JVM、HTTP、支付链路、行为埋点）。

## 二、与现有部署的协同（重要）

1. `application-prod.yml` 已配置 `management.endpoints.web.exposure.include: health,info,prometheus` 且 `health.show-details: never`。
2. `deploy/nginx.conf` 已限制 `/actuator/**` 仅内网来源；**Prometheus 必须部署在与后端同内网网段抓取**，切勿将 9090/9093/3000 暴露公网。
3. 若后端在 Docker / K8s 内，将 `prometheus.yml` 中 `targets` 改为容器服务名或 ClusterIP（见文件注释）。

## 三、快速启动

```bash
# 部署时：将本目录内容放到服务器 deploy/monitoring/ 后
cd deploy/monitoring
docker compose up -d
# Grafana: http://<内网IP>:3000  (默认 admin/admin，首次登录请改密)
# Prometheus: http://<内网IP>:9090
# Alertmanager: http://<内网IP>:9093
```

## 四、告警通知配置

编辑 `alertmanager.yml`（本目录未含，按下列模板自建）：

```yaml
route:
  receiver: 'wechat'
  group_by: ['alertname']
receivers:
  - name: 'wechat'
    webhook_configs:
      - url: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY'
```

## 五、告警规则清单（`rules-broadband.yml`）

| 告警 | 触发条件 | 含义 |
|---|---|---|
| `JvmHeapMemoryLeak` | 堆内存 1h 内持续单调抬升 | 疑似内存泄漏（对照 T-06 长稳） |
| `Http5xxErrorRateHigh` | 5xx 错误率 > 0.1% | 服务异常（对照门禁 <0.1%） |
| `HttpP95LatencyHigh` | P95 > 1s | 接口变慢 |
| `PaymentFailureRateHigh` | 支付失败率 > 1% | 支付链路异常 |
| `SlaTimeoutCompensation` | SLA 超时赔付触发 | 运营赔付异常波动 |

## 六、上线后数据观察项（对应复盘报告 R8）

1. 支付链路：微信回调成功率/时延、退款闭环率、Mock→APIv3 切换前后对比。
2. 行为埋点→推荐：`user_behavior_event` 日增量与推荐位 CTR。
3. 长稳（T-06）：JVM 堆是否锯齿（正常）还是单调抬升（泄漏）；`http_req_failed` 是否 <0.1%；P95 是否收敛。
4. 多住宅：切换率、单账号多住宅占比。
5. SLA/赔付：超时赔付触发率。
6. 安全：强制改密触发率、越权 403、JWT 密钥环境变量覆盖率。

## 七、Grafana Dashboard

`dashboard-broadband.json` 提供基础看板（JVM / HTTP / 支付）。导入：Grafana → Dashboards → Import → 上传该 JSON。
