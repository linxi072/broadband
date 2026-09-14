const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { customer: { name: '陈先生', phone: '138****0001', level: '千兆五星' }, menus: [
      { key: 'order', icon: '📋', name: '我的订单' }, { key: 'addr', icon: '📍', name: '地址管理' },
      { key: 'review', icon: '⭐', name: '评价投诉' }, { key: 'self', icon: '🛠', name: '自助服务' },
      { key: 'smart', icon: '🏠', name: '智慧家庭' }, { key: 'setting', icon: '⚙️', name: '设置' }
    ] },
  onLoad(options) {

  },
  onShow() {
if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    if (getApp().globalData.customer) this.setData({ customer: getApp().globalData.customer });
  },
  goMenu(e) {
    const k = e.currentTarget.dataset.key;
    const map = { order: '/pages/order/list/list', addr: '/pages/address/address', review: '/pages/review/review', self: '/pages/self/self', smart: '/pages/smart/smart', setting: '/pages/settings/settings' };
    wx.navigateTo({ url: map[k] });
  },
  logout() { getApp().logout(); wx.reLaunch({ url: '/pages/login/login' }); }
});
