// utils/api.js —— 请求封装，对齐后端接口（Spring Boot :8082）
function request(path, method, data) {
  // 登录态：若本地已存 token，统一以 Bearer 注入请求头（c 端开放层由后端 protect-client-api 开关决定是否校验）
  const token = (getApp() && getApp().globalData && getApp().globalData.token) || '';
  const header = { 'content-type': 'application/json' };
  if (token) header['Authorization'] = 'Bearer ' + token;
  return new Promise((resolve, reject) => {
    wx.request({
      url: (getApp().globalData.baseUrl || '') + path,
      method: method || 'GET',
      data: data || {},
      header: header,
      // 真实缺陷修复：非 2xx 必须 reject，否则 401/403/404 会被当成成功、页面静默无数据
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) resolve(res.data);
        else {
          // M9 登录态收口：401 表示 token 失效/缺失，清空登录态并跳登录页重新鉴权
          if (res.statusCode === 401) {
            const app = getApp();
            if (app && typeof app.logout === 'function') app.logout();
            wx.reLaunch({ url: '/pages/login/login' });
          }
          reject(res.data || { message: '请求失败(' + res.statusCode + ')' });
        }
      },
      fail: (err) => reject(err)
    });
  });
}
const api = {
  // —— 套餐（C 端开放层）——
  getPackageDetail(id) { return request('/api/package/detail?id=' + (id || ''), 'GET'); },
  // 修复：原先指向不存在的 /api/package/list（404）；后端已新增 GET /api/package/list（在售套餐）
  listPackages() { return request('/api/package/list', 'GET'); },
  getUpgradeOptions(payload) { return request('/api/package/upgrade-options?customerId=' + encodeURIComponent((payload && payload.customerId) || 'demo'), 'GET'); },
  submitUpgrade(payload) { return request('/api/package/upgrade', 'POST', payload || {}); },
  // —— 小区 ——
  checkCommunity(name) { return request('/api/community/check?name=' + encodeURIComponent(name || ''), 'GET'); },
  registerDemand(payload) { return request('/api/community/demand', 'POST', payload || {}); },
  // —— 流量 ——
  getTrafficUsage(payload) { return request('/api/traffic/usage?customerId=' + encodeURIComponent((payload && payload.customerId) || 'demo'), 'GET'); },
  // —— 订单（C 端开放层，按客户查自己的订单）——
  // 修复：原先指向 /api/admin/orders（需 order:view 权限 → 403，且语义是管理端）；
  // 后端已新增 GET /api/order/my?customerId=（开放层）
  getOrders(customerId) { return request('/api/order/my?customerId=' + encodeURIComponent(customerId || 'demo'), 'GET'); },
  // —— 小程序登录（真实后端鉴权）——
  // 修复：原先是本地 mock 返回假 token；现调 POST /api/auth/miniapp-login 换取真实 JWT
  login(phone, code) {
    return request('/api/auth/miniapp-login', 'POST', { phone: phone, code: code })
      .then(r => ({ token: r.token, customer: r.customer }));
  },
  // —— SLA 测速（管理端接口，需登录态）——
  evaluateSla(record) { return request('/api/sla/evaluate', 'POST', record); }
};
module.exports = api;
