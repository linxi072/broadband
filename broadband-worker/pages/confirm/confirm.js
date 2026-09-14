const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { order: { id: 'B001', cust: '陈先生', addr: '保利花园 1-2-302', pkg: '1000M' }, services: [
      { key: 'design', name: '组网设计', ok: true }, { key: 'check', name: '上门检测', ok: true },
      { key: 'config', name: '终端调测', ok: true }, { key: 'cable', name: '线路整理', ok: true },
      { key: 'maint', name: '设备保养', ok: true }, { key: 'report', name: '质量报告', ok: false }
    ], down: '', up: '', sign: '' },
  onLoad(options) {

  },
  onDown(e) { this.setData({ down: e.detail.value }); },
  onUp(e) { this.setData({ up: e.detail.value }); },
  onSign(e) { this.setData({ sign: e.detail.value }); },
  toggleService(e) { const i = e.currentTarget.dataset.i; const s = this.data.services; s[i].ok = !s[i].ok; this.setData({ services: s }); },
  submit() {
    if (!this.data.down || !this.data.up) { wx.showToast({ title: '请填写测速结果', icon: 'none' }); return; }
    if (!this.data.sign) { wx.showToast({ title: '请客户签名', icon: 'none' }); return; }
    wx.showLoading({ title: '提交中' });
    setTimeout(() => { wx.hideLoading(); wx.showToast({ title: '完工已提交' }); setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 800); }, 600);
  }
});
