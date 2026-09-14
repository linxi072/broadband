const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { phone: '', code: '', sending: false, countdown: 0 },
  onLoad(options) {
    if (getApp().globalData.token) { wx.redirectTo({ url: '/pages/profile/profile' }); }
  },
  onPhone(e) { this.setData({ phone: e.detail.value }); },
  onCode(e) { this.setData({ code: e.detail.value }); },
  sendCode() {
    if (this.data.sending) return;
    if (!/^1\d{10}$/.test(this.data.phone)) { wx.showToast({ title: '手机号格式不正确', icon: 'none' }); return; }
    this.setData({ sending: true, countdown: 60 });
    // 演示：后端 miniapp-login 固定校验验证码 1234
    wx.showToast({ title: '验证码已发送（演示：1234）', icon: 'none' });
    const t = setInterval(() => { const c = this.data.countdown - 1; if (c <= 0) { clearInterval(t); this.setData({ sending: false, countdown: 0 }); } else this.setData({ countdown: c }); }, 1000);
  },
  login() {
    // 真实登录：交由后端校验验证码并签发 JWT；前端不再本地 mock
    api.login(this.data.phone, this.data.code).then(r => {
      getApp().setClientLogin(r.token, r.customer);
      wx.showToast({ title: '登录成功' });
      setTimeout(() => wx.switchTab({ url: '/pages/profile/profile' }), 600);
    }).catch(err => {
      wx.showToast({ title: (err && err.message) || '登录失败', icon: 'none' });
    });
  }
});
