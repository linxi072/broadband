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
  { id: 'M14', name: '性能监控', path: '/monitor', perm: 'monitor:view', type: 'MENU' }
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
    recentCompensations: [
      { orderId: 'WO-SLA-001', custName: '王先生', orderType: 'REPAIR', compType: 'CASH', compAmount: 30, reason: '故障报修超时 45 分钟', status: 'PAID', createdTime: Date.now() - 3600_000 },
      { orderId: 'WO-SLA-002', custName: '李女士', orderType: 'NEW_INSTALL', compType: 'VOUCHER', compAmount: 50, reason: '新装超时 1.2 小时', status: 'VERIFYING', createdTime: Date.now() - 7200_000 },
      { orderId: 'WO-SLA-003', custName: '陈先生', orderType: 'MOVE', compType: 'FEE_WAIVE', compAmount: 20, reason: '移机超时', status: 'PENDING', createdTime: Date.now() - 14400_000 }
    ]
  }
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
    ]
  }
}

/** 全部权限码（演示登录用） */
export const ALL_PERMS = [
  'dashboard:view', 'order:view', 'customer:view', 'package:view', 'upgrade:view',
  'community:view', 'workorder:view', 'dispatch:run', 'capacity:config', 'sla:view',
  'traffic:view', 'review:view', 'sales:view', 'finance:view',
  'system:user', 'system:role', 'system:menu', 'system:log', 'monitor:view'
]
