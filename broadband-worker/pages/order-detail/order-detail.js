const api = require('../../utils/api.js');

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
  data: { order: null },
  onLoad(options) {
    if (!options || !options.id) {
      wx.showToast({ title: '缺少工单号', icon: 'none' });
      return;
    }
    // 真实数据：GET /api/worker/work-orders/{id}（服务端校验归属，非本人工单返回空）
    api.getWorkOrder(options.id).then(o => {
      if (!o || !o.id) {
        wx.showToast({ title: '工单不存在或无权限查看', icon: 'none' });
        return;
      }
      this.setData({
        order: {
          id: o.id,
          cust: o.customer || '—',
          phone: o.phone || '—',
          addr: ((o.community || '') + ' ' + (o.address || '')).trim(),
          pkg: o.pkgDesc || '—',
          slot: o.timeSlot || '—',
          statusText: STATUS_TEXT[o.status] || o.status || '—',
          statusClass: STATUS_CLASS[o.status] || 'tag-default',
          items: ['组网设计', '终端调测', '线路整理']
        }
      });
    }).catch(err => {
      wx.showToast({ title: (err && err.message) || '加载工单失败', icon: 'none' });
    });
  },
  navigate() { wx.showToast({ title: '调起地图导航（演示）', icon: 'none' }); },
  call() {
    const p = (this.data.order && this.data.order.phone) || '';
    if (!/^1\d{10}$/.test(p)) {
      wx.showToast({ title: '该工单未登记联系电话', icon: 'none' });
      return;
    }
    wx.makePhoneCall({ phoneNumber: p });
  },
  start() {
    const id = (this.data.order && this.data.order.id) || '';
    wx.navigateTo({ url: '/pages/confirm/confirm?id=' + id });
  }
});
