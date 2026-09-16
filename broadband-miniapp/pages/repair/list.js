// pages/repair/list.js —— 我的报修列表（V1.13）
const api = require('../../utils/api.js');

Page({
  data: {
    list: [],
    loading: true
  },

  onShow() {
    this.load();
  },

  load() {
    const app = getApp();
    const customerId = (app.globalData && app.globalData.customer && app.globalData.customer.id) || 'demo';
    this.setData({ loading: true });
    api.getMyRepairs(customerId)
      .then(list => this.setData({ list: list || [], loading: false }))
      .catch(() => this.setData({ loading: false }));
  },

  openDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/repair/detail?id=' + id });
  },

  goCreate() {
    wx.navigateTo({ url: '/pages/repair/repair' });
  }
});
