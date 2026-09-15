// pages/order/list/list.js
const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

const STATUS_TEXT = { PENDING: '待处理', PAID: '已支付', INSTALLING: '安装中', DONE: '已完成', CANCELLED: '已取消' };
const STATUS_CLASS = { PENDING: 'tag-warning', PAID: 'tag-info', INSTALLING: 'tag-primary', DONE: 'tag-success', CANCELLED: 'tag-danger' };
const TYPE_TEXT = { NEW_INSTALL: '新装', MOVE: '移机', RENEW: '续费', SPEED_UP: '提速', REPAIR: '报修', ADDON: '加购' };

Page({
  data: { tabs: ['全部', '安装中', '已完成'], active: 0, loading: true, orders: [], list: [] },
  onShow() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    this.load();
  },
  load() {
    this.setData({ loading: true });
    const cid = (getApp().globalData.customer && getApp().globalData.customer.id) || 'demo';
    api.getOrders(cid).then(list => {
      const orders = (list || []).map(o => ({
        id: o.id,
        name: o.packageName,
        addr: o.communityName || '',
        status: o.status,
        statusText: STATUS_TEXT[o.status] || o.status,
        statusClass: STATUS_CLASS[o.status] || 'tag-default',
        typeText: TYPE_TEXT[o.orderType] || o.orderType || '',
        amount: o.amount
      }));
      this.setData({ loading: false, orders });
      this.applyFilter();
    }).catch(() => this.setData({ loading: false, orders: [], list: [] }));
  },
  applyFilter() {
    const a = this.data.active;
    const list = this.data.orders.filter(o => {
      if (a === 0) return true;
      if (a === 1) return o.status !== 'DONE' && o.status !== 'CANCELLED';
      return o.status === 'DONE';
    });
    this.setData({ list });
  },
  switchTab(e) { this.setData({ active: e.currentTarget.dataset.i }); this.applyFilter(); },
  goDetail(e) { wx.navigateTo({ url: '/pages/order/detail/detail?id=' + e.currentTarget.dataset.id }); },
  goPkg() { wx.switchTab({ url: '/pages/package/list/list' }); }
});
