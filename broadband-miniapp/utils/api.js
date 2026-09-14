// utils/api.js —— 请求封装，对齐后端接口（Spring Boot :8082）
// 套餐详情：      GET  /api/package/detail?id=
// 小区可装校验：  GET  /api/community/check?name=
// 安装需求登记：  POST /api/community/demand
// 装维 SLA 评估： POST /api/sla/evaluate
// 套餐升级选项：  GET  /api/package/upgrade-options
// 套餐升级提交：  POST /api/package/upgrade
// 流量用量查询：  GET  /api/traffic/usage

function request(path, method, data) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: (getApp().globalData.baseUrl || '') + path,
      method: method || 'GET',
      data: data || {},
      header: { 'content-type': 'application/json' },
      success: (res) => resolve(res.data),
      fail: (err) => reject(err)
    });
  });
}

const api = {
  // 融合套餐详情（后端未实现时返回空，由页面走 mock）
  getPackageDetail(id) {
    return request('/api/package/detail?id=' + (id || ''), 'GET');
  },
  // 小区可安装性校验（下单前 / 查询页复用）
  checkCommunity(name) {
    return request('/api/community/check?name=' + encodeURIComponent(name || ''), 'GET');
  },
  // 安装需求登记（小区未覆盖时）
  registerDemand(payload) {
    return request('/api/community/demand', 'POST', payload || {});
  },
  // 装维 SLA 评估（完工回传，对应 SlaController）
  evaluateSla(record) {
    return request('/api/sla/evaluate', 'POST', record);
  },
  // 套餐升级：升档选项 + 补差预览（对应 PackageUpgradeController）
  getUpgradeOptions(payload) {
    return request('/api/package/upgrade-options', 'GET', payload || {});
  },
  // 套餐升级：提交升档申请
  submitUpgrade(payload) {
    return request('/api/package/upgrade', 'POST', payload || {});
  },
  // 流量监控：客户流量用量（对应 TrafficController）
  getTrafficUsage(payload) {
    return request('/api/traffic/usage', 'GET', payload || {});
  }
};

module.exports = api;
