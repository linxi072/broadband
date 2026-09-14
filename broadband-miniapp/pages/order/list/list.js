const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');
Page({
  data: { tabs: ['全部','安装中','已完成'], active: 0, orders: [
      { id: 'B20260901001', name: '1000M 融合「美好家」', addr: '保利花园 1-2-302', statusText: '安装中', amount: 129 },
      { id: 'B20260820007', name: '500M 全家享', addr: '阳光小区 5-1-901', statusText: '已完成', amount: 99 }
    ] },
  onLoad(options) {

  },
  switchTab(e) { this.setData({ active: e.currentTarget.dataset.i }); },
  goDetail(e) { wx.navigateTo({ url: '/pages/order/detail/detail?id=' + e.currentTarget.dataset.id }); }
});
