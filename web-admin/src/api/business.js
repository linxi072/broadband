import request, { silent } from './request'

/* =========================================================================
   一、已落地的 13 个后端接口（真实数据）
   ========================================================================= */

// ---------- 小区覆盖 community ----------
export function checkCommunity(name) {
  return request({ url: '/community/check', method: 'get', params: { name } })
}
export function submitDemand(data) {
  return request({ url: '/community/demand', method: 'post', data })
}

// ---------- 派单 install · dispatch ----------
export function runDispatch() {
  return request({ url: '/dispatch/run', method: 'post', data: {}, timeout: 30000 })
}
export function getCapacity(timeSlot) {
  return request({ url: '/dispatch/capacity', method: 'get', params: { timeSlot } })
}

// ---------- 装维 SLA install · sla ----------
export function slaEvaluate(record) {
  return request({ url: '/sla/evaluate', method: 'post', data: record })
}
export function slaEvaluateBatch(list) {
  return request({ url: '/sla/evaluate-batch', method: 'post', data: list })
}
export function slaBoard() {
  return request({ url: '/sla/board', method: 'get' })
}
export function slaRules() {
  return request({ url: '/sla/rules', method: 'get' })
}
export function slaCompensations() {
  return request({ url: '/sla/compensations', method: 'get' })
}

// ---------- 套餐 product · pkg ----------
export function packageDetail(id) {
  return request({ url: '/package/detail', method: 'get', params: { id } })
}
export function upgradeOptions(customerId) {
  return request({ url: '/package/upgrade-options', method: 'get', params: { customerId } })
}
export function submitUpgrade(data) {
  return request({ url: '/package/upgrade', method: 'post', data })
}

// ---------- 流量监控 product · traffic ----------
export function trafficUsage(customerId) {
  return request({ url: '/traffic/usage', method: 'get', params: { customerId } })
}

/* =========================================================================
   二、后台列表类接口（服务端已实现；不可达时页面自动降级演示数据）
   ========================================================================= */

// ---------- 数据看板 ----------
export function dashboardStats() {
  return silent({ url: '/admin/dashboard/stats', method: 'get' })
}
export function dashboardRecentOrders(limit = 6) {
  return silent({ url: '/admin/dashboard/recent-orders', method: 'get', params: { limit } })
}

// ---------- 订单 / 工单 ----------
export function orderList(params) {
  return silent({ url: '/admin/orders', method: 'get', params })
}
export function workOrderList(params) {
  return silent({ url: '/admin/work-orders', method: 'get', params })
}
export function resetWorkOrders() {
  return request({ url: '/admin/work-orders/reset', method: 'post', data: {} })
}

// ---------- 投诉与评价 ----------
export function reviewList(params) {
  return silent({ url: '/admin/reviews', method: 'get', params })
}
export function closeReview(id) {
  return request({ url: `/admin/reviews/${id}/close`, method: 'put', data: {} })
}

// ---------- 销售 / 财务 ----------
export function salesReport(params) {
  return silent({ url: '/admin/sales/report', method: 'get', params })
}
export function financeReport(params) {
  return silent({ url: '/admin/finance/report', method: 'get', params })
}

// ---------- 性能监控 ----------
export function monitorOverview() {
  return silent({ url: '/admin/monitor/overview', method: 'get' })
}

// ---------- 客户 ----------
export function customerList(params) {
  return silent({ url: '/admin/customers', method: 'get', params })
}
export function customer360(id) {
  return silent({ url: `/admin/customer/${id}/360`, method: 'get' })
}

// ---------- 装维 SLA 履约看板 ----------
export function slaDashboard() {
  return silent({ url: '/admin/sla/dashboard', method: 'get' })
}
export function slaOvertimeDetail(dayOfWeek, hour) {
  return silent({ url: '/admin/sla/overtime-detail', method: 'get', params: { dayOfWeek, hour } })
}

// ---------- 套餐 ----------
export function adminPackageList() {
  return silent({ url: '/admin/packages', method: 'get' })
}
export function savePackage(data) {
  return silent({ url: '/admin/packages', method: 'post', data })
}
export function productMarketing() {
  return silent({ url: '/admin/product/marketing', method: 'get' })
}
export function productFunnelDetail(stage) {
  return silent({ url: '/admin/product/funnel-detail', method: 'get', params: { stage } })
}

// ---------- 小区覆盖 ----------
export function adminCommunityList(params) {
  return silent({ url: '/admin/communities', method: 'get', params })
}
export function saveCommunity(data) {
  return request({ url: '/admin/communities', method: 'post', data })
}

// ---------- 师傅与容量 ----------
export function workerList() {
  return silent({ url: '/admin/workers', method: 'get' })
}
export function saveWorkerCapacity(data) {
  return request({ url: '/admin/worker-capacity', method: 'post', data })
}

// ---------- 套餐升级管理 ----------
export function adminUpgradeOrders() {
  return silent({ url: '/admin/upgrade-orders', method: 'get' })
}

// ---------- 流量运营 ----------
export function adminTrafficOverview() {
  return silent({ url: '/admin/traffic/overview', method: 'get' })
}
