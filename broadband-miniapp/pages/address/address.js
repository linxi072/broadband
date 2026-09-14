const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { list: [ { id: 1, addr: '保利花园 1-2-302', def: true }, { id: 2, addr: '阳光小区 5-1-901', def: false } ] },
  onLoad(options) {

  },
  onShow() {
if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); }
  },
  add() { wx.showToast({ title: '新增地址（演示）', icon: 'none' }); }
});
