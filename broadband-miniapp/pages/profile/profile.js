const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { customer: { name: '陈先生', phone: '138****0001', level: '千兆五星' }, menus: [
      { key: 'order', icon: '📋', name: '我的订单' },
      { key: 'bills', icon: '🧾', name: '账户账单' },
      { key: 'review', icon: '⭐', name: '评价投诉' },
      { key: 'account', icon: '💳', name: '账户中心' },
      { key: 'points', icon: '🎁', name: '积分成长' },
      { key: 'promotion', icon: '🎉', name: '优惠活动' },
      { key: 'support', icon: '💡', name: '帮助中心' },
      { key: 'self', icon: '🛠', name: '自助服务' },
      { key: 'setting', icon: '⚙️', name: '设置' }
    ] },
  onLoad(options) {

  },
  onShow() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    if (getApp().globalData.customer) this.setData({ customer: getApp().globalData.customer });
  },
  goMenu(e) {
    const k = e.currentTarget.dataset.key;
    const map = {
      order: '/pages/order/list/list', bills: '/pages/account/bills', review: '/pages/review/review',
      account: '/pages/account/account', points: '/pages/points/points', promotion: '/pages/promotion/list',
      support: '/pages/support/support', self: '/pages/self/self', setting: '/pages/settings/settings'
    };
    wx.navigateTo({ url: map[k] });
  },
  logout() { getApp().logout(); wx.reLaunch({ url: '/pages/login/login' }); }
});
