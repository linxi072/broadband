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
        if (res.statusCode >= 200 && res.statusCode < 300) { resolve(res.data); return; }
        // 401 门禁：登录态失效统一清 token 并跳登录页，避免停在空白页
        if (res.statusCode === 401) {
          const app = getApp();
          if (app && app.setWorkerLogin) app.setWorkerLogin(null, null);
          wx.removeStorageSync('token');
          wx.redirectTo({ url: '/pages/login/login' });
        }
        reject(res.data || { message: '请求失败(' + res.statusCode + ')' });
      },
      fail: (err) => reject(err)
    });
  });
}
const api = {
  // 本人的工单（师傅端专用，服务端按 token 隔离，只返回本人工单）
  getWorkOrders(status) { return request('/api/worker/work-orders?status=' + (status || ''), 'GET'); },
  // 工单详情（师傅端专用，服务端校验归属，非本人工单返回空）
  getWorkOrder(id) { return request('/api/worker/work-orders/' + (id || ''), 'GET'); },
  evaluateSla(record) { return request('/api/sla/evaluate', 'POST', record); },
  // 首页统计 + 今日工单（GET /api/worker/summary?date=）
  getSummary(date) { return request('/api/worker/summary?date=' + (date || ''), 'GET'); },
  // 本人时段容量占用（GET /api/worker/capacity?date=）
  getMyCapacity(date) { return request('/api/worker/capacity?date=' + (date || ''), 'GET'); },
  // 未来 7 天排班（GET /api/worker/schedule）
  getSchedule() { return request('/api/worker/schedule', 'GET'); },
  // 完工提交：测速/签名/服务项落库并置 DONE
  completeWorkOrder(id, payload) {
    return request('/api/worker/work-orders/' + id + '/complete', 'POST', payload || {});
  },
  getCapacity(timeSlot) { return request('/api/dispatch/capacity?timeSlot=' + encodeURIComponent(timeSlot || ''), 'GET'); },
  // 修复：原先误调 /api/auth/miniapp-login（签发的是 CUSTOMER 令牌，调 /api/admin/* 过不了 RBAC）；
  // 现调专用 POST /api/auth/worker-login 换取 dept=WORKER 的真实 JWT
  workerLogin(phone, code) {
    return request('/api/auth/worker-login', 'POST', { phone: phone, code: code })
      .then(r => ({
        token: r.token,
        worker: { name: r.worker.name, no: r.worker.id, phone: r.worker.phone, star: 4.8 }
      }));
  }
};
module.exports = api;
