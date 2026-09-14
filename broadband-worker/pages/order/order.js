const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { tabs: ['全部','待接单','施工中','已完成'], active: 0, list: [
      { id: 'B001', cust: '陈先生', addr: '保利花园 1-2-302', statusText: '待上门', badge: '待接单' },
      { id: 'B003', cust: '张先生', addr: '锦绣华庭 3-8-1202', statusText: '施工中', badge: '施工中' },
      { id: 'B004', cust: '赵女士', addr: '中央公馆 2-3-501', statusText: '已完成', badge: '已完成' }
    ] },
  onLoad(options) {

  },
  switchTab(e) { this.setData({ active: e.currentTarget.dataset.i }); },
  goDetail(e) { wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id }); }
});
