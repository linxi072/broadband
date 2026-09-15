const api = require('../../utils/api.js');

// 页签 → 后端工单状态（全部为空，不传 status 即不过滤）
const STATUS_TABS = ['', 'ASSIGNED', 'INSTALLING', 'DONE'];
const STATUS_TEXT = {
  PENDING: '待派单',
  ASSIGNED: '待上门',
  INSTALLING: '施工中',
  DONE: '已完成',
  CANCELLED: '已取消'
};
const STATUS_CLASS = {
  PENDING: 'tag-warning', ASSIGNED: 'tag-info', INSTALLING: 'tag-primary',
  DONE: 'tag-success', CANCELLED: 'tag-default'
};

Page({
  data: {
    tabs: ['全部', '待接单', '施工中', '已完成'],
    active: 0,
    list: [],
    loading: false,
    empty: false
  },
  onLoad() { this.load(); },
  onShow() { this.load(); },
  switchTab(e) {
    this.setData({ active: e.currentTarget.dataset.i }, () => this.load());
  },
  load() {
    const status = STATUS_TABS[this.data.active] || '';
    this.setData({ loading: true });
    // 真实数据：GET /api/worker/work-orders（服务端按 token 隔离，只返回本人工单）
    api.getWorkOrders(status).then(list => {
      const items = (list || []).map(o => ({
        id: o.id,
        cust: o.customer || '—',
        addr: ((o.community || '') + ' ' + (o.address || '')).trim(),
        statusText: STATUS_TEXT[o.status] || o.status || '—',
        statusClass: STATUS_CLASS[o.status] || 'tag-default'
      }));
      this.setData({ list: items, loading: false, empty: items.length === 0 });
    }).catch(err => {
      this.setData({ loading: false, empty: false });
      wx.showToast({ title: (err && err.message) || '加载工单失败', icon: 'none' });
    });
  },
  goDetail(e) {
    wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id });
  }
});
