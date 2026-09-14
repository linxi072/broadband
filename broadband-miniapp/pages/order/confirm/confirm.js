const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');
Page({
  data: { pkg: { name: '1000M 融合「美好家」', price: 129 }, address: '保利花园 1-2-302', contact: '陈先生 138****0001', period: 24 },
  onLoad(options) {

  },
  submit() {
    wx.showLoading({ title: '提交中' });
    setTimeout(() => { wx.hideLoading(); wx.showToast({ title: '下单成功' }); setTimeout(() => wx.redirectTo({ url: '/pages/order/list/list' }), 800); }, 600);
  }
});
