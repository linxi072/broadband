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
      success: (res) => resolve(res.data),
      fail: (err) => reject(err)
    });
  });
}
const api = {
  getWorkOrders(status) { return request('/api/admin/work-orders?status=' + (status || ''), 'GET'); },
  getWorkOrder(id) { return request('/api/admin/work-orders/' + (id || ''), 'GET'); },
  evaluateSla(record) { return request('/api/sla/evaluate', 'POST', record); },
  getCapacity(timeSlot) { return request('/api/dispatch/capacity?timeSlot=' + encodeURIComponent(timeSlot || ''), 'GET'); },
  // —— 师傅登录态（演示：本地 mock；接后端时替换为 /api/auth/login）——
  workerLogin(phone, code) {
    return new Promise((resolve) => {
      setTimeout(() => resolve({ token: 'mock-' + Date.now(), worker: { name: '王师傅', no: 'W1001', star: 4.8, phone: phone } }), 200);
    });
  }
};
module.exports = api;
