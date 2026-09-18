// pages/points/mall.js —— 积分商城（V1.14 运营留存）
const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

Page({
  data: { items: [], balance: 0, loading: true, cid: 'demo', result: null, redeeming: false },

  onShow() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    const app = getApp();
    this.setData({ cid: (app.globalData.customer && app.globalData.customer.id) || 'demo' });
    this.load();
  },

  load() {
    this.setData({ loading: true });
    Promise.all([api.getPointsMall(), api.getPointsBalance(this.data.cid)])
      .then(([items, b]) => this.setData({
        items: items || [],
        balance: b ? b.balance : 0,
        loading: false
      }))
      .catch(() => this.setData({ loading: false }));
  },

  redeem(e) {
    const it = e.currentTarget.dataset.item;
    if (this.data.redeeming) return;
    if (it.stock === 0) { wx.showToast({ title: '已售罄', icon: 'none' }); return; }
    if (this.data.balance < it.cost) { wx.showToast({ title: '积分不足', icon: 'none' }); return; }
    this.setData({ redeeming: true });
    api.redeemPoints(this.data.cid, it.id)
      .then(r => {
        this.setData({
          redeeming: false,
          result: { code: r.couponCode, name: it.name, balance: r.balance }
        });
        this.load();
      })
      .catch(err => {
        this.setData({ redeeming: false });
        wx.showToast({ title: (err && err.message) || '兑换失败', icon: 'none' });
      });
  },

  closeResult() { this.setData({ result: null }); },
  noop() {}
});
