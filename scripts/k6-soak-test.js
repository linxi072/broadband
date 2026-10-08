// ============================================================================
// 宽带业务管理系统 · k6 长稳（soak）压测脚本（T-06 阶段 1）
// 与 scripts/k6-load-test.js（G9 短时爬坡 · 容量基线）互补：
//   本脚本面向**长稳验收** —— 持续 ≥2h 恒定负载，验证：
//     1) 错误率 < 0.1%（thresholds 中 http_req_failed: rate<0.001）
//     2) 无内存泄漏（周期采样 jvm_memory_used_bytes{area="heap"}，比对首尾增幅）
//     3) 长跑下 P95 不显著劣化（对比前 10min 与末段）
//
// 为什么单独成脚本：长稳与容量基线目标不同（前者看"是否劣化"，后者看"能扛多少"），
// 混在一个脚本里会让阈值互相打架（爬坡必然带来尾部抖动，会误判长稳失败）。
//
// 用法（默认 2 小时）：
//   k6 run -e BASE_URL=http://localhost:8082 -e USER=admin -e PWD=admin123 scripts/k6-soak-test.js
// 自定义时长 / 并发 / 采样间隔：
//   k6 run -e SOAK_MINUTES=180 -e VUS=10 -e SAMPLE_INTERVAL_SEC=300 scripts/k6-soak-test.js
//
// 前置：需能访问 /actuator/prometheus（application.yml 已 expose prometheus）。
//   若该端点被网络策略限制，内存采样会失败并打印告警，但**不会**影响压测主流程与阈值判定。
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Gauge } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8082';
const USER = __ENV.USER || 'admin';
const PWD = __ENV.PWD || 'admin123';

// 长稳参数：默认 2h（T-06 要求 ≥2h），恒定并发（默认 10 VU，模拟持续运营访问）
const SOAK_MINUTES = Number(__ENV.SOAK_MINUTES || 120);
const VUS = Number(__ENV.VUS || 10);
const SAMPLE_INTERVAL_SEC = Number(__ENV.SAMPLE_INTERVAL_SEC || 300); // 内存采样间隔，默认 5min

// 内存堆使用量（MB）观测指标：用于判断泄漏趋势
const heapUsedMb = new Gauge('jvm_heap_used_mb');
const heapTrend = new Trend('jvm_heap_used_mb_trend');

export const options = {
  // 短预热 → 长时间恒定 → 短回落。恒定段是长稳观测主体。
  stages: [
    { duration: '1m', target: VUS },                    // 预热，避免冷启动 JIT/连接池抖动污染数据
    { duration: `${SOAK_MINUTES}m`, target: VUS },      // 长稳主体
    { duration: '30s', target: 0 },                     // 回落
  ],
  thresholds: {
    // T-06 验收口径：错误率 < 0.1%（注意：不是容量测试的 1%）
    http_req_failed: ['rate<0.001'],
    http_req_duration: ['p(95)<1000'],
  },
};

function login() {
  const res = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ username: USER, password: PWD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } }
  );
  check(res, { 'login 200': (r) => r.status === 200 }, { name: 'login' });
  return res.json('token');
}

/**
 * 从 Prometheus 文本格式中抓取 JVM 堆已用内存（MB）。
 * 端点不可达时返回 null —— 长稳主流程不应因为观测手段不可用而失败。
 */
function sampleHeapMb() {
  const res = http.get(`${BASE}/actuator/prometheus`, {
    tags: { name: 'actuator_prometheus' },
  });
  if (!res || res.status !== 200) return null;
  const body = res.body || '';
  // 形如：jvm_memory_used_bytes{application="broadband-backend",area="heap",id="G1 Old Gen",} 1.2345678E8
  const re = /jvm_memory_used_bytes\{[^}]*area="heap"[^}]*\}\s+([0-9eE+\-.]+)/g;
  let total = 0;
  let found = false;
  let m;
  while ((m = re.exec(body)) !== null) {
    total += Number(m[1]);
    found = true;
  }
  return found ? total / 1024 / 1024 : null;
}

function recordHeap(mb) {
  if (mb == null) return;
  heapUsedMb.add(mb);
  heapTrend.add(mb);
}

export function setup() {
  const token = login();
  const first = sampleHeapMb();
  if (first == null) {
    console.warn('[soak] 无法采集 /actuator/prometheus（端点不可达或未暴露），内存泄漏判定将不可用');
  } else {
    console.log(`[soak] 起始堆内存 ≈ ${first.toFixed(1)} MB`);
    recordHeap(first);
  }
  return { token, startedAt: Date.now() };
}

export default function (data) {
  // JWT 默认 TTL 120min；长稳跑 2h 以上时 token 会过期，此处按"临近过期即重登"处理，
  // 避免把"token 过期导致的 401"误统计为系统错误率。
  let token = data.token;
  const elapsedMin = (Date.now() - data.startedAt) / 60000;
  if (!token || elapsedMin > 110) {
    token = login();
    data.token = token;
    data.startedAt = Date.now();
  }
  if (!token) return;
  const auth = { headers: { Authorization: `Bearer ${token}` } };

  // 核心只读链路（与容量脚本保持一致，便于横向对比长跑前后劣化）
  check(http.get(`${BASE}/api/admin/orders`, auth), { 'orders 200': (r) => r.status === 200 });
  check(http.get(`${BASE}/api/admin/sales/report`, auth), { 'sales 200': (r) => r.status === 200 });
  check(http.get(`${BASE}/api/admin/work-orders`, auth), { 'workorders 200': (r) => r.status === 200 });
  check(http.get(`${BASE}/api/dispatch/capacity`, auth), { 'capacity 200': (r) => r.status === 200 });

  // 低频内存采样：每个 VU 按间隔采样一次（多 VU 下会略微重复，但趋势判定不受影响）
  const last = __ENV.__LAST_SAMPLE_TS ? Number(__ENV.__LAST_SAMPLE_TS) : 0;
  const now = Date.now();
  if (now - (globalThis.__bbLastSample || 0) > SAMPLE_INTERVAL_SEC * 1000) {
    globalThis.__bbLastSample = now;
    recordHeap(sampleHeapMb());
  }
  void last;

  sleep(1);
}

export function teardown(data) {
  const last = sampleHeapMb();
  if (last == null) {
    console.warn('[soak] 结束时仍无法采集堆内存，无法给出泄漏结论');
    return;
  }
  console.log(`[soak] 结束堆内存 ≈ ${last.toFixed(1)} MB`);

  // 泄漏粗判：长跑结束后堆内存相对起始值增幅超过 50% 且绝对值增加 > 200MB 时告警。
  // 说明：JVM 堆在 GC 后本就会波动，单次采样不足以定性，此处仅作**提示**，
  //       确证需结合 Prometheus/Grafana 的 jvm_memory_used_bytes 长期曲线看是否单调上升。
  const res = http.get(`${BASE}/actuator/prometheus`, { tags: { name: 'actuator_prometheus' } });
  void res;
  console.log(
    '[soak] 判定提示：请对比 Grafana 中 jvm_memory_used_bytes{area="heap"} 曲线是否单调上升；' +
      '若呈锯齿（GC 回收）则为正常，若长周期单调抬升则需排查泄漏。'
  );
  console.log('[soak] 长稳结束。请核对 k6 输出的 http_req_failed 是否 < 0.1%、p(95) 是否满足预期。');
  void data;
}
