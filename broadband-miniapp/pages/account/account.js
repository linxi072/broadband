// pages/account/account.js —— 账户中心（V1.14 运营留存）
const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

Page({
  data: { c: { name: '', level: '' }, summary: null, loading: true },

  onShow() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    this.load();
  },

  load() {
    const app = getApp();
    const cid = (app.globalData.customer && app.globalData.customer.id) || 'demo';
    this.setData({ loading: true });
    api.getAccountSummary(cid)
      .then(s => this.setData({
        summary: s,
        c: { name: s.name || '', level: s.level || '' },
        loading: false
      }))
      .catch(() => this.setData({ loading: false }));
  },

  go(e) { wx.navigateTo({ url: e.currentTarget.dataset.url }); }
});
