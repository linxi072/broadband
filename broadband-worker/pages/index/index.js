const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

const STATUS_CLASS = {
  '待派单': 'tag-warning', '待上门': 'tag-info', '施工中': 'tag-primary',
  '已完成': 'tag-success', '已取消': 'tag-default'
};

Page({
  data: {
    date: '',
    stats: [{ n: 0, l: '待上门' }, { n: 0, l: '今日已完成' }, { n: 0, l: '累计工单' }],
    today: [],
    loading: true,
    empty: false
  },

  onLoad() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    this.load();
  },

  onShow() {
    if (auth.isLogin()) this.load();
  },

  goProfile() { wx.switchTab({ url: '/pages/profile/profile' }); },
  goOrder() { wx.switchTab({ url: '/pages/order/order' }); },

  load() {
    this.setData({ loading: true });
    api.getSummary()
      .then(r => {
        const today = (r.today || []).map(o => ({
          id: o.id,
          cust: o.cust || o.customer || '—',
          addr: o.addr || '—',
          pkg: o.pkg || o.pkgDesc || '—',
          time: o.time || o.timeSlot || '—',
          statusText: o.statusText || '—',
          statusClass: STATUS_CLASS[o.statusText] || 'tag-default'
        }));
        this.setData({
          date: r.date || '',
          stats: (r.stats && r.stats.length) ? r.stats : this.data.stats,
          today: today,
          empty: today.length === 0,
          loading: false
        });
      })
      .catch(err => {
        console.error('[index] summary 加载失败', err);
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败，请重试', icon: 'none' });
      });
  },

  goDetail(e) { wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id }); },
  startComplete(e) { wx.navigateTo({ url: '/pages/confirm/confirm?id=' + e.currentTarget.dataset.id }); }
});
