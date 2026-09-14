const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

Page({
  data: {
    id: '',
    order: { id: '', cust: '', addr: '', pkg: '' },
    services: [
      { key: 'design', name: '组网设计', ok: false }, { key: 'check', name: '上门检测', ok: false },
      { key: 'config', name: '终端调测', ok: false }, { key: 'cable', name: '线路整理', ok: false },
      { key: 'maint', name: '设备保养', ok: false }, { key: 'report', name: '质量报告', ok: false }
    ],
    down: '', up: '', sign: '', submitting: false
  },

  onLoad(options) {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    const id = options.id || '';
    this.setData({ id: id });
    if (id) this.load(id);
  },

  load(id) {
    api.getWorkOrder(id)
      .then(o => {
        if (!o || !o.id) {
          wx.showToast({ title: '工单不存在或无权查看', icon: 'none' });
          setTimeout(() => wx.navigateBack(), 900);
          return;
        }
        this.setData({
          order: {
            id: o.id,
            cust: o.customer || '—',
            addr: (o.community || '') + ' ' + (o.address || ''),
            pkg: o.pkgDesc || '—'
          }
        });
      })
      .catch(err => {
        console.error('[confirm] 工单加载失败', err);
        wx.showToast({ title: '加载失败，请重试', icon: 'none' });
      });
  },

  onDown(e) { this.setData({ down: e.detail.value }); },
  onUp(e) { this.setData({ up: e.detail.value }); },
  onSign(e) { this.setData({ sign: e.detail.value }); },
  toggleService(e) {
    const i = e.currentTarget.dataset.i;
    const s = this.data.services;
    s[i].ok = !s[i].ok;
    this.setData({ services: s });
  },

  submit() {
    if (!this.data.down || !this.data.up) { wx.showToast({ title: '请填写测速结果', icon: 'none' }); return; }
    if (!this.data.sign) { wx.showToast({ title: '请客户签名', icon: 'none' }); return; }
    if (this.data.submitting) return;

    const done = this.data.services.filter(s => s.ok).map(s => s.key).join(',');
    this.setData({ submitting: true });
    wx.showLoading({ title: '提交中' });

    api.completeWorkOrder(this.data.id, {
      down: Number(this.data.down),
      up: Number(this.data.up),
      sign: this.data.sign,
      services: done
    })
      .then(() => {
        wx.hideLoading();
        this.setData({ submitting: false });
        wx.showToast({ title: '完工已提交' });
        setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 800);
      })
      .catch(err => {
        wx.hideLoading();
        this.setData({ submitting: false });
        console.error('[confirm] 提交失败', err);
        wx.showToast({ title: (err && err.message) || '提交失败', icon: 'none' });
      });
  }
});
