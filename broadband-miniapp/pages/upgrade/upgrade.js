const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { current: '1000M 融合「美好家」', plans: [
      { id: 'renew', name: '续约原套餐', desc: '合约再续 24 个月，月费不变', price: '¥129/月' },
      { id: 'speed', name: '提速至 2000M', desc: '宽带速率升级包', price: '+¥30/月' },
      { id: 'add', name: '加购 40G 流量包', desc: '当月生效', price: '+¥20/月' }
    ] },
  onLoad(options) {

  },
  handle(e) { wx.showToast({ title: '已提交：' + e.currentTarget.dataset.name }); }
});
