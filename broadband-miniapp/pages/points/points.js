// pages/points/points.js —— 积分成长（V1.14 运营留存）
const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

Page({
  data: { balance: null, tasks: [], loading: true, signing: false },

  onShow() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    this.load();
  },

  load() {
    const app = getApp();
    const cid = (app.globalData.customer && app.globalData.customer.id) || 'demo';
    this.setData({ loading: true });
    Promise.all([api.getPointsBalance(cid), api.getPointsTasks()])
      .then(([b, tasks]) => this.setData({ balance: b, tasks: tasks || [], loading: false }))
      .catch(() => this.setData({ loading: false }));
  },

  sign() {
    const app = getApp();
    const cid = (app.globalData.customer && app.globalData.customer.id) || 'demo';
    if (this.data.signing) return;
    if (this.data.balance && this.data.balance.signedToday) {
      wx.showToast({ title: '今日已签到', icon: 'none' });
      return;
    }
    this.setData({ signing: true });
    api.signPoints(cid)
      .then(r => {
        this.setData({ signing: false });
        if (r && r.alreadySigned) wx.showToast({ title: '今日已签到', icon: 'none' });
        else wx.showToast({ title: '+' + (r.earned || 0) + ' 积分', icon: 'success' });
        this.load();
      })
      .catch(() => this.setData({ signing: false }));
  },

  goMall() { wx.navigateTo({ url: '/pages/points/mall' }); }
});
