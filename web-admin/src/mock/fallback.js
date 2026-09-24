/**
 * 演示兜底数据（仅在对应后端接口不可达时使用）
 * 页面会在标题旁显示「演示数据」标记，与「实时数据」区分，避免误读。
 */

/** 客户 360 全景演示兜底（与 GET /api/admin/customer/:id/360 返回结构对齐） */
export function demoCustomer360(id) {
  const base = demoCustomers.find((c) => c.id === id) || demoCustomers[0]
  const name = base.name
  const statusLabelOf = (s) =>
    ({ ASSIGNED: '已派单', PENDING: '待派单', INSTALLING: '安装中', DONE: '已完成', CANCELLED: '已取消' }[s] || s)
  return {
    profile: {
      id: base.id, name: base.name, phone: base.phone, level: base.level, levelLabel: base.level,
      pkgName: base.pkgName, pkgMonthlyFee: base.monthlyFee, packageId: 'pkg-demo',
      communityName: '保利花园', address: '深圳市南山区科技园路 1 号', statusLabel: base.status,
      contractEnd: base.contractEnd, contractStatus: '生效中', tags: base.tags || []
    },
    orders: demoOrders.filter((o) => o.customer === name).map((o) => ({
      id: o.id, packageName: o.pkgName, amount: o.amount, orderTypeLabel: '新装',
      statusLabel: o.status, createdAt: o.time
    })),
    contracts: [
      { id: 'CT-DMO01', packageId: 'pkg-demo', monthlyFee: base.monthlyFee, startDate: '2026-04-01', endDate: base.contractEnd, statusLabel: '生效中' }
    ],
    traffic: { period: '2026-09', mobileTotal: 60, mobileUsed: 42, broadbandHours: 210, broadbandPeak: '943M', dailyTrend: [5, 6, 7, 5, 8, 7, 4] },
    reviews: demoReviews.filter((r) => r.customer === name).map((r) => ({
      id: r.id, orderId: r.orderId, score: r.score, tags: r.tags, typeLabel: r.type, content: '',
      statusLabel: r.status, createdAt: r.time
    })),
    workOrders: demoWorkOrders.map((w) => ({
      id: w.id, statusLabel: statusLabelOf(w.status), timeSlot: w.timeSlot, customerName: name,
      packageDesc: w.type, workerId: w.worker, downSpeed: w.status === 'DONE' ? 942 : null,
      upSpeed: w.status === 'DONE' ? 48 : null, signName: w.status === 'DONE' ? name : null,
      completeTime: w.status === 'DONE' ? '2026-09-15 11:30' : null, bizOrderId: w.orderId, bizStatus: null
    })),
    upgradeOrders: demoUpgradeOrders.filter((u) => u.customer === name).map((u) => ({
      id: u.id, fromPackageId: u.fromPkg, targetBandKey: u.toPkg, monthDiff: u.monthDiff,
      oneTimeDiff: u.oneTimeDiff, currentFee: 0, newFee: 0, effectType: u.effectType,
      statusLabel: u.status, createdAt: u.time
    })),
    lifecycle: { stage: 'STABLE', stageLabel: '稳定期', color: 'success', daysSince: 12, lastActive: Date.now() - 12 * 86400000, reasons: ['近 12 天内有互动', '最近互动：' + new Date(Date.now() - 12 * 86400000).toLocaleString('zh-CN')] },
    touchRecords: [
      { type: 'order', title: '下单 · 500M 融合 40G', detail: '¥129 · 已支付', time: Date.now() - 12 * 86400000, timeText: '2026-09-14 09:12' },
      { type: 'install', title: '安装完工 · 新装', detail: '师傅 张师傅', time: Date.now() - 11 * 86400000, timeText: '2026-09-13 11:30' },
      { type: 'review', title: '评价 · 5★', detail: '师傅专业、速度快', time: Date.now() - 10 * 86400000, timeText: '2026-09-12 18:20' },
      { type: 'upgrade', title: '升级申请 · 1000M 融合', detail: '待审核', time: Date.now() - 2 * 86400000, timeText: '2026-09-14 12:01' },
      { type: 'contract', title: '签约合约', detail: '¥129 · 到期 2028-03-31', time: Date.now() - 400 * 86400000, timeText: '2026-04-01' }
    ],
    summary: { orderCount: 4, paidAmount: 496, workOrderCount: demoWorkOrders.length, reviewCount: 1, avgScore: 5, upgradeCount: 1 }
  }
}

export const demoCustomers = [
  { id: 'c001', name: '陈先生', phone: '13800001111', level: '五星', pkgName: '500M 融合 40G', monthlyFee: 129, contractEnd: '2028-03-31', status: '在用', tags: ['高价值', '合约中'] },
  { id: 'c002', name: '李女士', phone: '13800002222', level: '四星', pkgName: '300M 融合 20G', monthlyFee: 99, contractEnd: '2027-01-31', status: '在用', tags: ['宽带+IPTV'] },
  { id: 'c003', name: '王先生', phone: '13800003333', level: '五星', pkgName: '1000M 融合 60G', monthlyFee: 199, contractEnd: '2028-09-30', status: '在用', tags: ['千兆', 'FTTR'] },
  { id: 'c004', name: '赵女士', phone: '13800004444', level: '三星', pkgName: '200M 单宽', monthlyFee: 69, contractEnd: '2026-12-31', status: '待续约', tags: ['临期'] }
]

export const demoOrders = [
  { id: 'B20260914001', customer: '陈先生', phone: '13800001111', pkgName: '500M 融合', amount: 129, sales: '刘伟', status: '已支付', time: '2026-09-14 09:12', community: '保利花园' },
  { id: 'B20260914002', customer: '李女士', phone: '13800002222', pkgName: '300M 融合', amount: 99, sales: '赵敏', status: '安装中', time: '2026-09-14 10:05', community: '海岸城公寓' },
  { id: 'B20260914003', customer: '王先生', phone: '13800003333', pkgName: '1000M 融合', amount: 199, sales: '刘伟', status: '待受理', time: '2026-09-14 11:20', community: '保利花园' },
  { id: 'B20260914004', customer: '赵女士', phone: '13800004444', pkgName: '200M 单宽', amount: 69, sales: '—', status: '已完成', time: '2026-09-13 16:40', community: '阳光新村' }
]

export const demoWorkOrders = [
  { id: 'WO-0001', orderId: 'B20260914001', community: '保利花园', address: '1-2-302', timeSlot: '2026-09-15#AM', worker: '张师傅', type: '新装', status: 'ASSIGNED', adjacent: true },
  { id: 'WO-0002', orderId: 'B20260914002', community: '海岸城公寓', address: 'B-1806', timeSlot: '2026-09-15#AM', worker: '李师傅', type: '新装', status: 'ASSIGNED', adjacent: false },
  { id: 'WO-0003', orderId: 'B20260914003', community: '保利花园', address: '5-1-1101', timeSlot: '2026-09-15#PM', worker: '—', type: '移机', status: 'PENDING', adjacent: true },
  { id: 'WO-0004', orderId: 'B20260914004', community: '阳光新村', address: '9-402', timeSlot: '2026-09-15#PM', worker: '王师傅', type: '报修', status: 'DONE', adjacent: false }
]

export const demoCommunities = [
  { id: 'cm001', name: '保利花园', region: '南山区/科技园', street: '科技园街道', lat: 22.541, lng: 113.951, covered: true, carrier: '电信·联通', portTotal: 64, portUsed: 32, status: 'AVAILABLE' },
  { id: 'cm002', name: '海岸城公寓', region: '南山区/粤海', street: '粤海街道', lat: 22.518, lng: 113.935, covered: true, carrier: '电信', portTotal: 48, portUsed: 40, status: 'TIGHT' },
  { id: 'cm003', name: '阳光新村', region: '宝安区/新安', street: '新安街道', lat: 22.568, lng: 113.883, covered: false, carrier: '—', portTotal: 0, portUsed: 0, status: 'UNAVAILABLE' },
  { id: 'cm004', name: '科技园公寓', region: '南山区/科技园', street: '科技园街道', lat: 22.544, lng: 113.947, covered: true, carrier: '联通', portTotal: 96, portUsed: 41, status: 'AVAILABLE' }
]

export const demoWorkers = [
  { id: 'w001', name: '张师傅', level: '高级', skill: 'FTTR/千兆', phone: '13900000001', status: '在线' },
  { id: 'w002', name: '李师傅', level: '中级', skill: '常规装维', phone: '13900000002', status: '在线' },
  { id: 'w003', name: '王师傅', level: '初级', skill: '常规装维', phone: '13900000003', status: '休息' }
]

export const demoCapacity = [
  { workerId: 'w001', workerName: '张师傅', level: '高级', timeSlot: '09:00-12:00', adjCap: 4, nonCap: 2, mode: '单独配置' },
  { workerId: 'w001', workerName: '张师傅', level: '高级', timeSlot: '14:00-17:00', adjCap: 4, nonCap: 2, mode: '单独配置' },
  { workerId: 'w002', workerName: '李师傅', level: '中级', timeSlot: '全天', adjCap: 3, nonCap: 1, mode: '继承默认' },
  { workerId: 'w003', workerName: '王师傅', level: '初级', timeSlot: '全天', adjCap: 2, nonCap: 1, mode: '单独配置' }
]

export const demoPackages = [
  { id: 'pkg300', name: '300M 融合套餐', category: '融合', monthlyFee: 99, originalFee: 129, contract: '12 个月', status: '上架', online: true, mainImage: '300M 融合', images: 3, params: 3 },
  { id: 'pkg500', name: '500M 融合套餐', category: '融合', monthlyFee: 129, originalFee: 169, contract: '24 个月', status: '上架', online: true, mainImage: '500M 融合', images: 4, params: 4 },
  { id: 'pkg1000', name: '1000M 融合套餐「美好家」', category: '千兆', monthlyFee: 199, originalFee: 259, contract: '24 个月', status: '上架', online: true, mainImage: '1000M 融合', images: 5, params: 4 },
  { id: 'pkg2000', name: '2000M 千兆尊享', category: '千兆', monthlyFee: 299, originalFee: 399, contract: '24 个月', status: '下架', online: false, mainImage: '2000M', images: 3, params: 4 }
]

export const demoUpgradeOrders = [
  { id: 'UP20260914001', customer: '陈先生', fromPkg: '500M 融合', toPkg: '1000M 融合', monthDiff: 70, oneTimeDiff: 1260, effectType: 'immediate', status: '待审核', time: '2026-09-14 12:01' },
  { id: 'UP20260913002', customer: '李女士', fromPkg: '300M 融合', toPkg: '2000M +FTTR', monthDiff: 230, oneTimeDiff: 1620, effectType: 'nextMonth', status: '已生效', time: '2026-09-13 15:22' }
]

export const demoReviews = [
  { id: 'RV001', orderId: 'B20260912008', customer: '陈先生', worker: '张师傅', score: 5, tags: ['准时', '专业', '速度快'], type: '评价', status: '已回访', time: '2026-09-12 18:20' },
  { id: 'RV002', orderId: 'B20260911003', customer: '王先生', worker: '李师傅', score: 3, tags: ['迟到'], type: '投诉', status: '处理中', time: '2026-09-11 20:05' },
  { id: 'RV003', orderId: 'B20260910002', customer: '赵女士', worker: '王师傅', score: 4, tags: ['专业'], type: '评价', status: '已闭环', time: '2026-09-10 17:10' }
]

export const demoSales = [
  { id: 's001', name: '刘伟', region: '南山区', orders: 86, amount: 12480, conversion: 62, month: '2026-09' },
  { id: 's002', name: '赵敏', region: '宝安区', orders: 71, amount: 9860, conversion: 55, month: '2026-09' },
  { id: 's003', name: '王芳', region: '福田区', orders: 64, amount: 8320, conversion: 49, month: '2026-09' }
]

export const demoFinance = [
  { id: 'f001', month: '2026-09', revenue: 864000, refund: 12800, compensation: 340, receivable: 42100, status: '对账中' },
  { id: 'f002', month: '2026-08', revenue: 812500, refund: 9600, compensation: 520, receivable: 38600, status: '已结账' },
  { id: 'f003', month: '2026-07', revenue: 786300, refund: 7400, compensation: 610, receivable: 35200, status: '已结账' }
]

export const demoNodes = [
  { name: 'broadband-svc-01', addr: '127.0.0.1:8082', status: '在线', cpu: 38, mem: 71, qps: 842, rt: 126 },
  { name: 'mysql-01', addr: '127.0.0.1:3306', status: '在线', cpu: 22, mem: 48, qps: 310, rt: 8 },
  { name: 'nginx-gateway', addr: '127.0.0.1:80', status: '在线', cpu: 9, mem: 18, qps: 890, rt: 3 }
]

/** 部门（按区域划分）演示兜底（与 GET /api/system/departments 返回结构对齐） */
export const demoDepartments = [
  { id: 'D1', parentId: null, name: '华南大区', region: '华南', sortOrder: 1, status: 'ENABLED', createdTime: 1756678800000 },
  { id: 'D2', parentId: 'D1', name: '深圳分公司', region: '华南', sortOrder: 2, status: 'ENABLED', createdTime: 1756678800000 },
  { id: 'D3', parentId: 'D1', name: '广州分公司', region: '华南', sortOrder: 3, status: 'ENABLED', createdTime: 1756678800000 },
  { id: 'D4', parentId: null, name: '华东大区', region: '华东', sortOrder: 4, status: 'ENABLED', createdTime: 1756678800000 },
  { id: 'D5', parentId: 'D4', name: '上海分公司', region: '华东', sortOrder: 5, status: 'ENABLED', createdTime: 1756678800000 }
]


export const demoSlowApis = [
  { api: 'POST /api/dispatch/run', calls: 1204, avg: 312, status: '偏慢' },
  { api: 'GET /api/package/detail', calls: 8910, avg: 88, status: '正常' },
  { api: 'POST /api/sla/evaluate-batch', calls: 642, avg: 256, status: '偏慢' },
  { api: 'GET /api/admin/dashboard/stats', calls: 53, avg: 1800, status: '超时' }
]

export const demoMenus = [
  { id: 'M1', name: '数据看板', path: '/dashboard', perm: 'dashboard:view', type: 'MENU' },
  { id: 'M2', name: '订单管理', path: '/order', perm: 'order:view', type: 'MENU' },
  { id: 'M3', name: '客户管理', path: '/customer', perm: 'customer:view', type: 'MENU' },
  { id: 'M4', name: '套餐管理', path: '/package', type: 'DIR', children: [
    { id: 'M41', name: '套餐列表', path: '/package', perm: 'package:view', type: 'MENU' },
    { id: 'M42', name: '新增/编辑套餐', path: '/package/edit', perm: 'package:edit', type: 'MENU' },
    { id: 'M43', name: '营销看板', path: '/package/marketing', perm: 'package:view', type: 'MENU' }
  ] },
  { id: 'M5', name: '套餐升级', path: '/package/upgrade', perm: 'upgrade:view', type: 'MENU' },
  { id: 'M6', name: '小区覆盖管理', path: '/community', type: 'DIR', children: [
    { id: 'M61', name: '小区列表', path: '/community', perm: 'community:view', type: 'MENU' },
    { id: 'M62', name: '新增/编辑覆盖', path: '/community/edit', perm: 'community:edit', type: 'MENU' }
  ] },
  { id: 'M7', name: '安装工单', path: '/workorder/pool', type: 'DIR', children: [
    { id: 'M71', name: '工单池', path: '/workorder/pool', perm: 'workorder:view', type: 'MENU' },
    { id: 'M72', name: '派单调度', path: '/workorder/dispatch', perm: 'dispatch:run', type: 'MENU' },
    { id: 'M73', name: '容量配置', path: '/workorder/capacity', perm: 'capacity:config', type: 'MENU' },
    { id: 'M74', name: '调度规则', path: '/workorder/rules', perm: 'capacity:config', type: 'MENU' }
  ] },
  { id: 'M8', name: '装维 SLA 与赔付', path: '/sla', perm: 'sla:view', type: 'MENU' },
  { id: 'M9', name: '流量监控', path: '/traffic', perm: 'traffic:view', type: 'MENU' },
  { id: 'M10', name: '投诉与评价', path: '/review', perm: 'review:view', type: 'MENU' },
  { id: 'M11', name: '销售管理', path: '/sales', perm: 'sales:view', type: 'MENU' },
  { id: 'M12', name: '财务管理', path: '/finance', perm: 'finance:view', type: 'MENU' },
  { id: 'M13', name: '权限管理', path: '/system/user', type: 'DIR', children: [
    { id: 'M131', name: '用户管理', path: '/system/user', perm: 'system:user', type: 'MENU' },
    { id: 'M132', name: '角色管理', path: '/system/role', perm: 'system:role', type: 'MENU' },
    { id: 'M133', name: '菜单权限', path: '/system/menu', perm: 'system:menu', type: 'MENU' },
    { id: 'M134', name: '操作日志', path: '/system/log', perm: 'system:log', type: 'MENU' },
    { id: 'M44', name: '部门管理', path: '/system/department', perm: 'system:dept', type: 'MENU' }
  ] },
  { id: 'M14', name: '性能监控', path: '/monitor', perm: 'monitor:view', type: 'MENU' },
  { id: 'M150', name: '数据智能', path: '/intelligence', type: 'DIR', children: [
    { id: 'M151', name: '客户分群与智能营销', path: '/intelligence', perm: 'intelligence:view', type: 'MENU' }
  ] },
  { id: 'M180', name: '支付管理', path: '/admin/pay/transactions', type: 'DIR', children: [
    { id: 'M181', name: '支付流水', path: '/admin/pay/transactions', perm: 'payment:view', type: 'MENU' },
    { id: 'M182', name: '退款处理', path: '/admin/pay/refund', perm: 'payment:view', type: 'MENU' }
  ] },
  { id: 'M190', name: '数据分析深化', path: '/analytics/customer360', type: 'DIR', children: [
    { id: 'M191', name: '客户 360', path: '/analytics/customer360', perm: 'analytics:view', type: 'MENU' },
    { id: 'M192', name: '营销漏斗', path: '/analytics/funnel', perm: 'analytics:view', type: 'MENU' },
    { id: 'M193', name: 'SLA 超时与赔付', path: '/analytics/sla', perm: 'analytics:view', type: 'MENU' }
  ] }
]

export const demoTrafficOverview = {
  activeCustomers: 12860,
  monthPool: '4.2 PB',
  overCustomers: 86,
  alarming: 12,
  top: [
    { customer: '陈先生', pkg: '1000M 融合 60G', used: 58, total: 60, status: '预警' },
    { customer: '李女士', pkg: '500M 融合 40G', used: 22, total: 40, status: '正常' },
    { customer: '王先生', pkg: '2000M 融合 100G', used: 61, total: 100, status: '正常' },
    { customer: '赵女士', pkg: '200M 单宽', used: 39, total: 40, status: '预警' }
  ]
}

export const demoDashboard = {
  monthOrders: 1286,
  revenue: 864000,
  pendingInstall: 342,
  fulfillmentRate: 98.2,
  slaRate: 98.3,
  slowPay: 17,
  avgResponse: 42,
  monthComp: 340,
  orderTrend: [62, 74, 58, 86, 91, 78, 103],
  orderTrendLabels: ['09-08', '09-09', '09-10', '09-11', '09-12', '09-13', '09-14']
}

/** SLA 履约看板演示兜底（与 GET /api/admin/sla/dashboard 返回结构对齐） */
export function demoSlaDashboard() {
  const today = new Date()
  const dayLabels = []
  const overtimeByDay = []
  const metByDay = []
  for (let i = 13; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    dayLabels.push(`${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
    overtimeByDay.push(Math.floor(Math.random() * 6) + 1)
    metByDay.push(Math.floor(Math.random() * 30) + 40)
  }
  const compTrend = []
  for (let i = 5; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth() - i, 1)
    compTrend.push({
      month: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
      compAmount: Math.floor(Math.random() * 400) + 200,
      compCount: Math.floor(Math.random() * 20) + 5
    })
  }
  return {
    summary: { total: 1286, met: 1258, overtime: 28, slaRate: 97.8, avgResponseMin: 38, totalCompAmount: 1840, pendingCompCount: 6 },
    byType: [
      { orderType: 'NEW_INSTALL', orderTypeLabel: '新装宽带', total: 642, met: 632, overtime: 10, slaRate: 98.4 },
      { orderType: 'MOVE', orderTypeLabel: '宽带移机', total: 218, met: 213, overtime: 5, slaRate: 97.7 },
      { orderType: 'REPAIR', orderTypeLabel: '故障报修', total: 312, met: 301, overtime: 11, slaRate: 96.5 },
      { orderType: 'SPEED_UP', orderTypeLabel: '宽带提速', total: 86, met: 85, overtime: 1, slaRate: 98.8 },
      { orderType: 'RENEW', orderTypeLabel: '续费', total: 28, met: 27, overtime: 1, slaRate: 96.4 }
    ],
    overtimeByDay: dayLabels.map((date, i) => ({ date, overtime: overtimeByDay[i], met: metByDay[i] })),
    compTrend,
    heatmap: (() => {
      const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      const hours = Array.from({ length: 24 }, (_, h) => h)
      const values = days.map((_, d) => hours.map((h) => {
        const base = (d < 5 ? 1 : 0.4) * (h >= 9 && h <= 21 ? 1 : 0.3)
        return Math.floor(Math.random() * 3 * base) + ((h >= 18 && h <= 21 && d < 5) ? Math.floor(Math.random() * 4) : 0)
      }))
      return { days, hours, values }
    })(),
    recentCompensations: [
      { orderId: 'WO-SLA-001', custName: '王先生', orderType: 'REPAIR', compType: 'CASH', compAmount: 30, reason: '故障报修超时 45 分钟', status: 'PAID', createdTime: Date.now() - 3600_000 },
      { orderId: 'WO-SLA-002', custName: '李女士', orderType: 'NEW_INSTALL', compType: 'VOUCHER', compAmount: 50, reason: '新装超时 1.2 小时', status: 'VERIFYING', createdTime: Date.now() - 7200_000 },
      { orderId: 'WO-SLA-003', custName: '陈先生', orderType: 'MOVE', compType: 'FEE_WAIVE', compAmount: 20, reason: '移机超时', status: 'PENDING', createdTime: Date.now() - 14400_000 }
    ]
  }
}

/** SLA 超时热力下钻演示兜底（与 GET /api/admin/sla/overtime-detail 返回结构对齐） */
export function demoSlaOvertimeDetail(dayOfWeek, hour) {
  const types = [['REPAIR', '故障报修', '王先生', '保利花园'], ['NEW_INSTALL', '新装宽带', '李女士', '海岸城公寓'], ['MOVE', '宽带移机', '陈先生', '阳光新村']]
  const n = 2 + ((dayOfWeek + hour) % 3)
  const list = []
  for (let i = 0; i < n; i++) {
    const t = types[(i + hour) % types.length]
    const base = Date.now() - (i + 1) * 3600_000 * (dayOfWeek + 1)
    list.push({
      id: 'SR-D' + dayOfWeek + '-' + hour + '-' + i,
      orderId: 'WO-SLA-00' + ((hour * 7 + i) % 99 + 1),
      orderType: t[0],
      orderTypeLabel: t[1],
      customerName: t[2],
      community: t[3],
      acceptTime: base - 7200_000,
      completeTime: base,
      createdTime: base - 7200_000
    })
  }
  return list
}

/** 营销看板漏斗下钻演示兜底（与 GET /api/admin/product/funnel-detail 返回结构对齐） */
export function demoMarketingFunnelDetail(stage) {
  if (stage === '升级申请' || stage === '升级生效') {
    return demoUpgradeOrders.map((u) => ({
      id: u.id, customerId: 'c00' + ((demoUpgradeOrders.indexOf(u) % 4) + 1), target: u.toPkg, status: u.status, createdTime: Date.now() - 86400000
    }))
  }
  const pool = (stage === '已完成')
    ? demoOrders.filter((o) => o.status === '已完成')
    : (stage === '已支付')
      ? demoOrders.filter((o) => ['已支付', '安装中', '已完成'].includes(o.status))
      : demoOrders
  return pool.map((o) => ({
    id: o.id, customer: o.customer, packageName: o.pkgName, amount: o.amount,
    orderType: 'NEW_INSTALL', status: o.status, createdTime: Date.now() - 86400000
  }))
}

/** 套餐营销看板演示兜底（与 GET /api/admin/product/marketing 返回结构对齐） */
export function demoMarketingDashboard() {
  return {
    summary: {
      totalOrders: 28,
      totalRevenue: 3860,
      doneOrders: 19,
      avgOrderAmount: 138,
      customerCount: 12,
      upgradeCount: 6,
      effectiveUpgrades: 4,
      upgradeRate: 66.7
    },
    packageRanking: [
      { name: '1000M 融合 60G', orders: 9, revenue: 1791, ratio: 46.4 },
      { name: '500M 融合 40G', orders: 11, revenue: 1419, ratio: 36.8 },
      { name: '300M 融合 20G', orders: 6, revenue: 594, ratio: 15.4 },
      { name: '200M 单宽', orders: 2, revenue: 138, ratio: 3.6 }
    ],
    orderTypeDist: [
      { type: 'NEW_INSTALL', typeLabel: '新装宽带', orders: 16, revenue: 2218 },
      { type: 'RENEW', typeLabel: '续费', orders: 7, revenue: 966 },
      { type: 'SPEED_UP', typeLabel: '宽带提速', orders: 3, revenue: 396 },
      { type: 'ADDON', typeLabel: '加购', orders: 2, revenue: 280 }
    ],
    upgradeByStatus: [
      { status: 'EFFECTIVE', statusLabel: '已生效', count: 4 },
      { status: 'SUBMITTED', statusLabel: '待审核', count: 1 },
      { status: 'REJECTED', statusLabel: '已驳回', count: 1 }
    ],
    customerLevelDist: [
      { level: 'VIP', levelLabel: '五星', count: 3 },
      { level: 'GOLD', levelLabel: '四星', count: 5 },
      { level: 'SILVER', levelLabel: '三星', count: 3 },
      { level: 'NORMAL', levelLabel: '普通', count: 1 }
    ],
    revenueTrend: [
      { month: '2026-04', revenue: 560, orders: 4 },
      { month: '2026-05', revenue: 690, orders: 5 },
      { month: '2026-06', revenue: 720, orders: 6 },
      { month: '2026-07', revenue: 580, orders: 4 },
      { month: '2026-08', revenue: 660, orders: 5 },
      { month: '2026-09', revenue: 650, orders: 4 }
    ],
    funnel: [
      { stage: '业务订单', value: 28 },
      { stage: '已支付', value: 24 },
      { stage: '已完成', value: 19 },
      { stage: '升级申请', value: 6 },
      { stage: '升级生效', value: 4 }
    ]
  }
}

/** 全部权限码（演示登录用） */
export const ALL_PERMS = [
  'dashboard:view', 'order:view', 'customer:view', 'package:view', 'upgrade:view',
  'community:view', 'workorder:view', 'dispatch:run', 'capacity:config', 'sla:view',
  'traffic:view', 'review:view', 'sales:view', 'finance:view',
  'points:view', 'promotion:view', 'support:view', 'account:view',
  'intelligence:view', 'payment:view', 'analytics:view',
  'system:user', 'system:role', 'system:menu', 'system:log', 'monitor:view'
]

/* =========================================================================
 * v1.14 运营留存 · 演示兜底数据（接口不可达时降级）
 * 字段结构与对应后端 C 端接口返回一一对齐
 * ========================================================================= */

export const demoPointsBalance = {
  customerId: 'C-DMO01', name: '张伟', balance: 1280, totalEarned: 1560, totalSpent: 280,
  signStreak: 6, signedToday: false
}

export const demoPointsTasks = [
  { id: 'T1', title: '每日签到', desc: '连续签到赢积分', points: 5, type: 'DAILY', done: true },
  { id: 'T2', title: '完善资料', desc: '补全实名与地址', points: 50, type: 'ONE_TIME', done: true },
  { id: 'T3', title: '首单评价', desc: '完成一次安装评价', points: 30, type: 'ONE_TIME', done: false },
  { id: 'T4', title: '邀请好友', desc: '成功邀请 1 位好友办理', points: 100, type: 'INVITE', done: false }
]

export const demoPointsMall = [
  { id: 'M1', name: '5G 提速包（7 天）', cost: 200, stock: 99, type: 'SPEEDUP' },
  { id: 'M2', name: '腾讯视频月卡', cost: 500, stock: 50, type: 'VOUCHER' },
  { id: 'M3', name: '路由器抵扣券 ¥30', cost: 800, stock: 20, type: 'COUPON' }
]

export const demoPromotions = [
  { id: 'P1', title: '千兆融合限时直降', subtitle: '月费直降 30 元，连续 12 期', cover: '', type: 'NEW', target: 'pkg-1000', startDate: '2026-09-01', endDate: '2026-09-30', ruleJson: '{"cut":30,"months":12}', status: 'ONLINE' },
  { id: 'P2', title: '老用户续约送时长', subtitle: '合约续约赠送 3 个月', cover: '', type: 'RENEW', target: '', startDate: '2026-09-10', endDate: '2026-10-10', ruleJson: '{"giftMonths":3}', status: 'ONLINE' },
  { id: 'P3', title: '宽带+电视全家桶', subtitle: '办宽带送 IPTV', cover: '', type: 'BUNDLE', target: '', startDate: '2026-09-15', endDate: '2026-12-15', ruleJson: '{"gift":"iptv"}', status: 'ONLINE' }
]

export const demoFaqs = [
  { id: 'F1', category: '安装', question: '新装宽带多久能上门？', answer: '城区通常 24 小时内预约，48 小时内完成安装。' },
  { id: 'F2', category: '故障', question: '宽带突然断网怎么办？', answer: '请先重启光猫与路由器；仍异常可在「报修」提交工单，师傅将主动联系。' },
  { id: 'F3', category: '账单', question: '如何开具电子发票？', answer: '在「我的-账单」选择订单申请发票，财务审核后推送电子票。' },
  { id: 'F4', category: '套餐', question: '合约期内能升级带宽吗？', answer: '支持补差升级，按剩余合约月数折算一次性补差费用。' }
]

export const demoAccountSummary = {
  customerId: 'C-DMO01', name: '张伟', level: 'GOLD', points: 1280, monthConsume: 129, contractEnd: '2028-03-31'
}

export const demoAccountBills = [
  { id: 'BO1', packageName: '1000M 融合 60G', amount: 129, orderType: 'NEW', status: 'DONE', createdTime: Date.now() - 20 * 86400000, statusText: '已完成' },
  { id: 'BO2', packageName: '提速包（7 天）', amount: 19, orderType: 'UPGRADE', status: 'PAID', createdTime: Date.now() - 5 * 86400000, statusText: '已支付' },
  { id: 'BO3', packageName: '安装调测费', amount: 100, orderType: 'NEW', status: 'PAID', createdTime: Date.now() - 60 * 86400000, statusText: '已支付' }
]

/* =========================================================================
 * v1.15 支付真闭环 & 数据智能 · 演示兜底数据（接口不可达时降级）
 * 字段结构与对应后端接口返回一一对齐
 * ========================================================================= */

/** 支付流水列表（后台 /api/admin/pay/transactions） */
export const demoPayTransactions = [
  { id: 'PT1', outTradeNo: 'OUT1726800000001a', bizOrderId: 'B20260914001', customerId: 'c001', customerName: '陈先生', packageName: '500M 融合 40G', channel: 'WECHAT_MOCK', amount: 129, status: 'PAID', transactionId: 'MOCKTXN1001', paidTime: Date.now() - 18 * 86400000, createdTime: Date.now() - 18 * 86400000 },
  { id: 'PT2', outTradeNo: 'OUT1726800000002b', bizOrderId: 'B20260914003', customerId: 'c003', customerName: '王先生', packageName: '1000M 融合 60G', channel: 'WECHAT_MOCK', amount: 199, status: 'PAYING', transactionId: null, paidTime: null, createdTime: Date.now() - 2 * 3600000 },
  { id: 'PT3', outTradeNo: 'OUT1726800000003c', bizOrderId: 'B20260914004', customerId: 'c004', customerName: '赵女士', packageName: '200M 单宽', channel: 'WECHAT_MOCK', amount: 69, status: 'REFUNDED', transactionId: 'MOCKTXN1003', paidTime: Date.now() - 40 * 86400000, createdTime: Date.now() - 40 * 86400000 },
  { id: 'PT4', outTradeNo: 'OUT1726800000004d', bizOrderId: 'B20260914002', customerId: 'c002', customerName: '李女士', packageName: '300M 融合 20G', channel: 'WECHAT_MOCK', amount: 99, status: 'PAID', transactionId: 'MOCKTXN1004', paidTime: Date.now() - 12 * 86400000, createdTime: Date.now() - 12 * 86400000 }
]

/** 客户生命周期分群占比（/api/intelligence/segments） */
export function demoIntelSegments() {
  const total = demoCustomers.length + 6
  const raw = [
    { key: 'NEW', label: '新客', color: '#409EFF', count: 4, ratio: 21.1 },
    { key: 'ACTIVE', label: '活跃', color: '#67C23A', count: 7, ratio: 36.8 },
    { key: 'AT_RISK', label: '预警', color: '#E6A23C', count: 3, ratio: 15.8 },
    { key: 'CHURN_RISK', label: '流失风险', color: '#F56C6C', count: 2, ratio: 10.5 },
    { key: 'HIGH_VALUE', label: '高价值', color: '#9B59B6', count: 2, ratio: 10.5 },
    { key: 'COMPLAINT', label: '投诉处理', color: '#FA8C16', count: 1, ratio: 5.3 }
  ]
  return { total, segments: raw }
}

/** 流失风险明细（/api/intelligence/churn） */
export function demoIntelChurn(limit = 50) {
  const list = [
    { id: 'c004', name: '赵女士', level: 'SILVER', segment: 'CHURN_RISK', segmentLabel: '流失风险', orderCount: 3, totalSpent: 207, pointsBalance: 320, complaintOpen: 0, daysSinceLastOrder: 168, riskScore: 73, riskReasons: ['超过 120 天未下单，流失高风险', '普通会员，黏性较弱'] },
    { id: 'c002', name: '李女士', level: 'GOLD', segment: 'AT_RISK', segmentLabel: '预警', orderCount: 2, totalSpent: 198, pointsBalance: 540, complaintOpen: 0, daysSinceLastOrder: 92, riskScore: 41, riskReasons: ['60 天以上未互动，存在流失预警'] },
    { id: 'c001', name: '陈先生', level: 'VIP', segment: 'HIGH_VALUE', segmentLabel: '高价值', orderCount: 4, totalSpent: 496, pointsBalance: 1280, complaintOpen: 0, daysSinceLastOrder: 12, riskScore: 22, riskReasons: ['高价值客户，需重点维系'] },
    { id: 'c003', name: '王先生', level: 'VIP', segment: 'HIGH_VALUE', segmentLabel: '高价值', orderCount: 3, totalSpent: 597, pointsBalance: 980, complaintOpen: 1, daysSinceLastOrder: 38, riskScore: 55, riskReasons: ['高价值客户，需重点维系', '存在未闭环投诉，满意度风险'] }
  ].slice(0, limit)
  return { total: 19, list }
}

/** 智能营销规则（/api/intelligence/campaigns） */
export function demoIntelCampaigns() {
  const matched = { NEW: 4, ACTIVE: 7, AT_RISK: 3, CHURN_RISK: 2, HIGH_VALUE: 2, COMPLAINT: 1 }
  const list = [
    { id: 'IC_NEW', name: '新客首单关怀', segment: 'NEW', segmentLabel: '新客', channel: 'SMS', content: '欢迎办理宽带，首月体验专属提速包，详询客服。', triggerType: 'AUTO', status: 'ENABLED', reachCount: 12, lastTrigger: Date.now() - 3 * 86400000, createdTime: Date.now() - 30 * 86400000 },
    { id: 'IC_RISK', name: '流失预警挽回', segment: 'CHURN_RISK', segmentLabel: '流失风险', channel: 'PUSH', content: '好久不见～专属续费优惠限时领取，回TA续享高速宽带。', triggerType: 'AUTO', status: 'ENABLED', reachCount: 5, lastTrigger: Date.now() - 7 * 86400000, createdTime: Date.now() - 30 * 86400000 },
    { id: 'IC_COMPL', name: '投诉关怀回访', segment: 'COMPLAINT', segmentLabel: '投诉处理', channel: 'SMS', content: '非常抱歉给您带来不便，专属客服将尽快回访处理。', triggerType: 'MANUAL', status: 'ENABLED', reachCount: 1, lastTrigger: null, createdTime: Date.now() - 30 * 86400000 },
    { id: 'IC_VIP', name: '高价值客户权益', segment: 'HIGH_VALUE', segmentLabel: '高价值', channel: 'COUPON', content: '尊敬的VIP客户，赠送5G提速周卡，感恩一路相伴。', triggerType: 'MANUAL', status: 'ENABLED', reachCount: 8, lastTrigger: Date.now() - 14 * 86400000, createdTime: Date.now() - 30 * 86400000 },
    { id: 'IC_ATRISK', name: '活跃预警激活', segment: 'AT_RISK', segmentLabel: '预警', channel: 'PUSH', content: '您有专属提速券待领取，立即体验千兆极速。', triggerType: 'AUTO', status: 'ENABLED', reachCount: 3, lastTrigger: Date.now() - 5 * 86400000, createdTime: Date.now() - 30 * 86400000 }
  ].map((c) => ({ ...c, matchedCustomers: matched[c.segment] || 0 }))
  return { list }
}

/* =========================================================================
 * v1.15 T-04 数据分析深化（三页）· 演示兜底数据（接口不可达时降级）
 * 字段结构与 /api/admin/analytics/* 返回一一对齐
 * ========================================================================= */

/** 客户 360（/api/admin/analytics/customer-360） */
export function demoAnalyticsCustomer360(id = 'c001') {
  const base = demoCustomers.find((c) => c.id === id) || demoCustomers[0]
  return {
    basic: { id: base.id, name: base.name, phone: base.phone, level: base.level, communityId: 'cm001', status: 'ACTIVE', createdTime: Date.now() - 400 * 86400000 },
    segment: 'HIGH_VALUE',
    segmentLabel: '高价值',
    riskScore: 22,
    riskReasons: ['高价值客户，需重点维系'],
    orderCount: 4,
    totalSpent: 496,
    pointsBalance: 1280,
    complaintOpen: 0,
    daysSinceLastOrder: 12,
    recentOrders: [
      { id: 'B20260914001', orderType: 'NEW_INSTALL', packageName: '500M 融合 40G', amount: 129, status: 'DONE', createdTime: Date.now() - 12 * 86400000 },
      { id: 'B20260914002', orderType: 'RENEW', packageName: '500M 融合 40G', amount: 129, status: 'PAID', createdTime: Date.now() - 40 * 86400000 }
    ],
    recentReviews: [
      { score: 5, type: 'REVIEW', content: '师傅专业、速度快', status: 'CLOSED', createdTime: Date.now() - 10 * 86400000 }
    ],
    activeRefunds: []
  }
}

/** 营销漏斗（/api/admin/analytics/funnel） */
export function demoAnalyticsFunnel() {
  const registered = 19
  const stages = [
    { stage: '注册客户', count: 19, conversion: 100 },
    { stage: '活跃参与', count: 14, conversion: 73.7 },
    { stage: '创建订单', count: 11, conversion: 57.9 },
    { stage: '支付成功', count: 9, conversion: 47.4 },
    { stage: '复购客户', count: 4, conversion: 21.1 }
  ]
  return { registered, stages }
}

/** SLA 超时热力（/api/admin/analytics/sla-heatmap） */
export function demoAnalyticsSlaHeatmap(range = 30) {
  const byCommunity = [
    { communityId: 'cm001', communityName: '保利花园', overtimeCount: 6 },
    { communityId: 'cm002', communityName: '海岸城公寓', overtimeCount: 4 },
    { communityId: 'cm003', communityName: '阳光新村', overtimeCount: 2 }
  ]
  const trend = []
  const today = new Date()
  for (let i = 4; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i * 7)
    const wk = d.toISOString().slice(0, 10)
    trend.push({ weekStart: wk, overtimeCount: Math.floor(Math.random() * 4) + 1 })
  }
  return { range, totalOvertime: 12, byCommunity, trend }
}

/** 赔付趋势（/api/admin/analytics/payout-trend） */
export function demoAnalyticsPayoutTrend(range = 90) {
  const trend = []
  const today = new Date()
  for (let i = 5; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth() - i, 1)
    const wk = d.toISOString().slice(0, 10)
    trend.push({ weekStart: wk, amount: Math.round(Math.random() * 400 + 200), count: Math.floor(Math.random() * 8) + 2 })
  }
  const totalAmount = trend.reduce((a, t) => a + t.amount, 0)
  return { range, totalAmount, totalCount: trend.reduce((a, t) => a + t.count, 0), trend }
}
