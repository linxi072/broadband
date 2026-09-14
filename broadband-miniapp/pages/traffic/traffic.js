// pages/traffic/traffic.js —— 流量监控（融合套餐手机流量 + 宽带用量 + 趋势 + 预警）
const api = require('../../../utils/api.js');

Page({
  data: {
    loading: true,
    pkgName: '',
    mobile: null,      // {total, used, unit}
    broadband: null,  // {hours, peak}
    trend: [],        // 近 7 日流量（百分比）
    pct: 0,
    warn: false
  },

  onLoad() {
    this.loadUsage();
  },

  loadUsage() {
    api.getTrafficUsage({ customerId: 'demo' }).then(res => {
      const d = (res && res.data) ? res.data : res;
      if (!d) { this.fallback(); return; }
      const pct = d.mobile && d.mobile.total
        ? Math.min(100, Math.round(d.mobile.used / d.mobile.total * 100)) : 0;
      this.setData({
        pkgName: d.pkgName,
        mobile: d.mobile,
        broadband: d.broadband,
        trend: d.trend || [],
        pct,
        warn: d.warn,
        loading: false
      });
    }).catch(() => this.fallback());
  },

  fallback() {
    const mobile = { total: 60, used: 48, unit: 'G' };
    this.setData({
      pkgName: '1000M 融合',
      mobile,
      broadband: { hours: 286, peak: '943M' },
      trend: [40, 55, 48, 70, 62, 85, 73],
      pct: Math.round(48 / 60 * 100),
      warn: false,
      loading: false
    });
  },

  buyPack(e) {
    const g = e.currentTarget.dataset.g;
    wx.showToast({ title: '已加购 ' + g + 'G（mock）', icon: 'none' });
  }
});
