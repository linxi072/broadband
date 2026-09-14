const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { stats: [ { n: 3, l: '待上门' }, { n: 5, l: '今日已完成' }, { n: 128, l: '累计工单' } ], today: [
      { id: 'B001', cust: '陈先生', addr: '保利花园 1-2-302', pkg: '1000M', time: '今日 14:00-16:00', statusText: '待上门' },
      { id: 'B002', cust: '李女士', addr: '阳光小区 5-1-901', pkg: '500M', time: '今日 16:30-18:30', statusText: '待上门' }
    ] },
  onLoad(options) {

  },
  goDetail(e) { wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id }); },
  startComplete(e) { wx.navigateTo({ url: '/pages/confirm/confirm?id=' + e.currentTarget.dataset.id }); }
});
