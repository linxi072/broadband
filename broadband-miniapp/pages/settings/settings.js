const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { items: [ { key: 'notify', icon: '🔔', name: '消息通知', desc: '工单/赔付提醒' }, { key: 'about', icon: 'ℹ️', name: '关于我们', desc: '宽带业务管理系统' }, { key: 'cache', icon: '🧹', name: '清除缓存', desc: '' }, { key: 'logout', icon: '🚪', name: '退出登录', desc: '' } ] },
  onLoad(options) {

  },
  tap(e) {
    const k = e.currentTarget.dataset.key;
    if (k === 'logout') { getApp().logout(); wx.reLaunch({ url: '/pages/login/login' }); }
    else wx.showToast({ title: e.currentTarget.dataset.name, icon: 'none' });
  }
});
