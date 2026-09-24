// ============================================================================
// 宽带业务管理系统 · k6 压测脚本（G9 压测）
// 覆盖核心链路：登录 → 订单列表 → 销售报表 → 工单 → 派单容量。
// 目的：给出下单/派单/支付链路的容量基线（峰值 VU 与 P95 时延）。
//
// 用法：
//   k6 run -e BASE_URL=http://localhost:8082 -e USER=admin -e PWD=admin123 scripts/k6-load-test.js
// 容量基线建议：先以 20→50 VU 爬坡，若 P95 < 800ms 且失败率 < 1% 可继续加压；
//   一旦 P95 突破 1s 或错误率 > 5%，即接近系统拐点（联动 alert.rules.yml 阈值）。
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8082';
const USER = __ENV.USER || 'admin';
const PWD = __ENV.PWD || 'admin123';

export const options = {
  // 爬坡 → 峰值 → 回落
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],        // 整体失败率 < 1%
    http_req_duration: ['p(95)<800'],      // P95 时延 < 800ms
  },
};

export default function () {
  // 1) 登录换取 token
  const loginRes = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ username: USER, password: PWD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(loginRes, { 'login 200': r => r.status === 200 });
  const token = loginRes.json('token');
  if (!token) return;
  const auth = { headers: { Authorization: `Bearer ${token}` } };

  // 2) 核心只读链路（模拟运营后台高峰访问）
  check(http.get(`${BASE}/api/admin/orders`, auth), { 'orders 200': r => r.status === 200 });
  check(http.get(`${BASE}/api/admin/sales/report`, auth), { 'sales 200': r => r.status === 200 });
  check(http.get(`${BASE}/api/admin/work-orders`, auth), { 'workorders 200': r => r.status === 200 });
  check(http.get(`${BASE}/api/dispatch/capacity`, auth), { 'capacity 200': r => r.status === 200 });

  sleep(1);
}
