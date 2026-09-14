const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { oldAddr: '保利花园 1-2-302', newAddr: '', sameNet: true, checking: false, result: null },
  onLoad(options) {

  },
  onNew(e) { this.setData({ newAddr: e.detail.value }); },
  toggleSame(e) { this.setData({ sameNet: e.detail.value }); },
  check() {
    if (!this.data.newAddr) { wx.showToast({ title: '请填写新地址', icon: 'none' }); return; }
    this.setData({ checking: true });
    setTimeout(() => { this.setData({ checking: false, result: { ok: true, text: '新地址可安装（同运营商，免改号）' } }); }, 500);
  },
  submit() {
    if (!this.data.newAddr) { wx.showToast({ title: '请填写新地址', icon: 'none' }); return; }
    wx.showToast({ title: '移机申请已提交' });
  }
});
