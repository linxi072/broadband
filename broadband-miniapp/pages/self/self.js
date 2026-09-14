const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { items: [
      { key: 'speed', icon: '🚀', name: '在线测速' }, { key: 'bill', icon: '🧾', name: '账单查询' },
      { key: 'invoice', icon: '🧾', name: '电子发票' }, { key: 'pwd', icon: '🔑', name: '密码重置' },
      { key: 'unbind', icon: '🔌', name: '设备解绑' }, { key: 'faq', icon: '❓', name: '常见问题' }
    ] },
  onLoad(options) {

  },
  tap(e) { wx.showToast({ title: e.currentTarget.dataset.name + '（演示）', icon: 'none' }); }
});
