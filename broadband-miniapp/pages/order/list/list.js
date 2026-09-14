const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

const STATUS_TEXT = { PENDING: '待处理', PAID: '已支付', INSTALLING: '安装中', DONE: '已完成', CANCELLED: '已取消' };
const TYPE_TEXT = { NEW_INSTALL: '新装', MOVE: '移机', RENEW: '续费', SPEED_UP: '提速', REPAIR: '报修', ADDON: '加购' };

Page({
  data: { tabs: ['全部', '安装中', '已完成'], active: 0, loading: true, orders: [] },
  onLoad(options) {
    // 修复：原先是写死的静态数据；现从开放层 GET /api/order/my 拉取当前客户真实订单
    const cid = (getApp().globalData.customer && getApp().globalData.customer.id) || 'demo';
    api.getOrders(cid)
      .then(list => {
        const orders = (list || []).map(o => ({
          id: o.id,
          name: o.packageName,
          addr: o.communityName || (TYPE_TEXT[o.orderType] || o.orderType || ''),
          statusText: STATUS_TEXT[o.status] || o.status,
          typeText: TYPE_TEXT[o.orderType] || o.orderType,
          amount: o.amount
        }));
        this.setData({ loading: false, orders });
      })
      .catch(() => { this.setData({ loading: false, orders: [] }); });
  },
  switchTab(e) { this.setData({ active: e.currentTarget.dataset.i }); },
  goDetail(e) { wx.navigateTo({ url: '/pages/order/detail/detail?id=' + e.currentTarget.dataset.id }); }
});
