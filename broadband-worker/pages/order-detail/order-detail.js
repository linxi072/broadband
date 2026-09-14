const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { order: { id: 'B001', cust: '陈先生', phone: '138****0001', addr: '保利花园 1-2-302', pkg: '1000M 融合', slot: '今日 14:00-16:00', statusText: '待上门', items: ['组网设计','终端调测','线路整理'] } },
  onLoad(options) {

  },
  navigate() { wx.showToast({ title: '调起地图导航（演示）', icon: 'none' }); },
  call() { wx.makePhoneCall({ phoneNumber: '13800000001' }); },
  start() { wx.navigateTo({ url: '/pages/confirm/confirm?id=' + this.data.order.id }); }
});
