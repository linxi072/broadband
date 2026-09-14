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
      success: (res) => resolve(res.data),
      fail: (err) => reject(err)
    });
  });
}
const api = {
  getPackageDetail(id) { return request('/api/package/detail?id=' + (id || ''), 'GET'); },
  checkCommunity(name) { return request('/api/community/check?name=' + encodeURIComponent(name || ''), 'GET'); },
  registerDemand(payload) { return request('/api/community/demand', 'POST', payload || {}); },
  evaluateSla(record) { return request('/api/sla/evaluate', 'POST', record); },
  getUpgradeOptions(payload) { return request('/api/package/upgrade-options', 'GET', payload || {}); },
  submitUpgrade(payload) { return request('/api/package/upgrade', 'POST', payload || {}); },
  getTrafficUsage(payload) { return request('/api/traffic/usage', 'GET', payload || {}); },
  // —— 小程序登录态（演示：本地 mock；接后端时替换为 /api/auth/login）——
  login(phone, code) {
    return new Promise((resolve) => {
      setTimeout(() => resolve({ token: 'mock-' + Date.now(), customer: { name: '陈先生', phone: phone, level: '千兆五星' } }), 200);
    });
  },
  listPackages() { return request('/api/package/list', 'GET'); },
  getOrders(customerId) { return request('/api/admin/orders?customerId=' + (customerId || ''), 'GET'); }
};
module.exports = api;
