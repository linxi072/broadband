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
  // 修复：原先是本地 mock 返回假 token；现调 POST /api/auth/miniapp-login 换取真实 JWT。
  // 真实微信环境由 wx.login() 拿到的 code 经后端 code2Session 换取 openid；
  // 演示态（未注入 WECHAT_SECRET）后端走 smsCode(演示码 1234) 分支，code 留空即可。
  // 前端无需感知后端处于哪种模式，两类字段一并上送。
  login(phone, smsCode, wxCode) {
    return request('/api/auth/miniapp-login', 'POST', { phone: phone, code: wxCode || '', smsCode: smsCode || '' })
      .then(r => ({ token: r.token, customer: r.customer }));
  },
  // —— SLA 测速（管理端接口，需登录态）——
  evaluateSla(record) { return request('/api/sla/evaluate', 'POST', record); },
  // —— 故障报修（V1.13 全流程）——
  // 故障类型字典（开放接口，无需登录）：返回 [{label,value,sort}]
  getRepairCategories() { return request('/api/dict/public/fault_category', 'GET'); },
  // 提交报修：{customerId, faultCategory, faultDesc, contactPhone?}
  createRepair(payload) { return request('/api/repair/create', 'POST', payload || {}); },
  // 我的报修列表
  getMyRepairs(customerId) { return request('/api/repair/my?customerId=' + encodeURIComponent(customerId || 'demo'), 'GET'); },
  // 报修详情（含 SLA 与时间线）
  getRepairDetail(id) { return request('/api/repair/' + encodeURIComponent(id), 'GET'); },
  // 撤销报修
  cancelRepair(id) { return request('/api/repair/' + encodeURIComponent(id) + '/cancel', 'POST', {}); },
  // 开放参数读取（如客服电话）
  getConfig(key) { return request('/api/config/public/' + encodeURIComponent(key), 'GET'); },
  // —— V1.14 运营留存：账户 / 积分 / 活动 / 帮助 ——
  getAccountSummary(customerId) { return request('/api/account/summary?customerId=' + encodeURIComponent(customerId || 'demo'), 'GET'); },
  getAccountBills(customerId, period) {
    let p = '/api/account/bills?customerId=' + encodeURIComponent(customerId || 'demo');
    if (period) p += '&period=' + encodeURIComponent(period);
    return request(p, 'GET');
  },
  getPointsBalance(customerId) { return request('/api/points/balance?customerId=' + encodeURIComponent(customerId || 'demo'), 'GET'); },
  signPoints(customerId) { return request('/api/points/sign?customerId=' + encodeURIComponent(customerId || 'demo'), 'POST', {}); },
  getPointsTasks() { return request('/api/points/tasks', 'GET'); },
  getPointsMall() { return request('/api/points/mall', 'GET'); },
  redeemPoints(customerId, itemId) {
    return request('/api/points/redeem?customerId=' + encodeURIComponent(customerId || 'demo') + '&itemId=' + encodeURIComponent(itemId), 'POST', {});
  },
  getPromotions(type) {
    let p = '/api/promotions';
    if (type) p += '?type=' + encodeURIComponent(type);
    return request(p, 'GET');
  },
  getPromotionDetail(id) { return request('/api/promotions/' + encodeURIComponent(id), 'GET'); },
  getSupportFaq(category) {
    let p = '/api/support/faq';
    if (category) p += '?category=' + encodeURIComponent(category);
    return request(p, 'GET');
  },
  submitSupportTicket(payload) { return request('/api/support/ticket', 'POST', payload || {}); }
};
module.exports = api;
