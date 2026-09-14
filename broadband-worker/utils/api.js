// utils/api.js —— 师傅端请求封装（对齐后端 :8082）
function request(path, method, data) {
  // 登录态：若本地已存 token，统一以 Bearer 注入请求头
  const token = (getApp() && getApp().globalData && getApp().globalData.token) || '';
  const header = { 'content-type': 'application/json' };
  if (token) header['Authorization'] = 'Bearer ' + token;
  return new Promise((resolve, reject) => {
    wx.request({
      url: (getApp().globalData.baseUrl || '') + path,
      method: method || 'GET',
      data: data || {},
      header: header,
      // 修复：非 2xx 必须 reject，否则 401/403 被当成成功、页面静默无数据
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) resolve(res.data);
        else reject(res.data || { message: '请求失败(' + res.statusCode + ')' });
      },
      fail: (err) => reject(err)
    });
  });
}
const api = {
  // 工单池（管理端接口，需 order:view 权限；师傅端正式接入需配套 worker 角色授权，见 README M8）
  getWorkOrders(status) { return request('/api/admin/work-orders?status=' + (status || ''), 'GET'); },
  // 修复：原先指向 /api/admin/work-orders/{id}（后端无该单条接口 → 404）；后端已新增 GET /api/admin/work-orders/{id}
  getWorkOrder(id) { return request('/api/admin/work-orders/' + (id || ''), 'GET'); },
  evaluateSla(record) { return request('/api/sla/evaluate', 'POST', record); },
  getCapacity(timeSlot) { return request('/api/dispatch/capacity?timeSlot=' + encodeURIComponent(timeSlot || ''), 'GET'); },
  // 修复：原先是本地 mock 返回假 token；现调 POST /api/auth/miniapp-login 换取真实 JWT
  workerLogin(phone, code) {
    return request('/api/auth/miniapp-login', 'POST', { phone: phone, code: code })
      .then(r => ({
        token: r.token,
        worker: { name: r.customer.name, no: r.customer.id, phone: phone, star: 4.8, level: r.customer.level }
      }));
  }
};
module.exports = api;
