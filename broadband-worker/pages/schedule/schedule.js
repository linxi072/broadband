const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

Page({
  data: { shifts: [], empty: false },

  onLoad() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    this.load();
  },

  onShow() { if (auth.isLogin()) this.load(); },

  load() {
    api.getSchedule()
      .then(r => {
        const shifts = r.shifts || [];
        this.setData({ shifts: shifts, empty: shifts.length === 0 });
      })
      .catch(err => {
        console.error('[schedule] 加载失败', err);
        wx.showToast({ title: '加载失败，请重试', icon: 'none' });
      });
  }
});
