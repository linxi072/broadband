const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

Page({
  data: {
    date: '',
    stats: [{ n: 0, l: '待上门' }, { n: 0, l: '今日已完成' }, { n: 0, l: '累计工单' }],
    today: [],
    empty: false
  },

  onLoad() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    this.load();
  },

  onShow() {
    if (auth.isLogin()) this.load();
  },

  load() {
    api.getSummary()
      .then(r => {
        const today = (r.today || []).map(o => ({
          id: o.id,
          cust: o.cust || o.customer || '—',
          addr: o.addr || '—',
          pkg: o.pkg || o.pkgDesc || '—',
          time: o.time || o.timeSlot || '—',
          statusText: o.statusText || '—'
        }));
        this.setData({
          date: r.date || '',
          stats: (r.stats && r.stats.length) ? r.stats : this.data.stats,
          today: today,
          empty: today.length === 0
        });
      })
      .catch(err => {
        console.error('[index] summary 加载失败', err);
        wx.showToast({ title: '加载失败，请重试', icon: 'none' });
      });
  },

  goDetail(e) { wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id }); },
  startComplete(e) { wx.navigateTo({ url: '/pages/confirm/confirm?id=' + e.currentTarget.dataset.id }); }
});
