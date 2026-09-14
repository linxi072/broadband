const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

Page({
  data: { date: '', slots: [], empty: false },

  onLoad() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    this.load();
  },

  onShow() { if (auth.isLogin()) this.load(); },

  load() {
    api.getMyCapacity()
      .then(r => {
        const slots = r.slots || [];
        this.setData({ date: r.date || '', slots: slots, empty: slots.length === 0 });
      })
      .catch(err => {
        console.error('[capacity] 加载失败', err);
        wx.showToast({ title: '加载失败，请重试', icon: 'none' });
      });
  }
});
