// pages/index/index.js
const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

function greeting() {
  const h = new Date().getHours();
  if (h < 6) return '夜深了';
  if (h < 12) return '早上好';
  if (h < 14) return '中午好';
  if (h < 18) return '下午好';
  return '晚上好';
}

const STATUS_TEXT = { PENDING: '待处理', PAID: '已支付', INSTALLING: '安装中', DONE: '已完成', CANCELLED: '已取消' };
const TYPE_TEXT = { NEW_INSTALL: '新装', MOVE: '移机', RENEW: '续费', SPEED_UP: '提速', REPAIR: '报修', ADDON: '加购' };

Page({
  data: {
    greeting: greeting(),
    isLogin: false,
    customer: null,
    loadingPkg: true,
    packages: [],
    loadingOrder: true,
    recentOrder: null
  },
  onShow() {
    const logged = auth.isLogin();
    this.setData({ isLogin: logged, customer: getApp().globalData.customer || null });
    this.loadPackages();
    if (logged) this.loadRecentOrder();
    else this.setData({ loadingOrder: false, recentOrder: null });
  },
  loadPackages() {
    this.setData({ loadingPkg: true });
    api.listPackages().then(list => {
      const pkgs = (list || []).slice(0, 4).map(p => ({
        id: p.id,
        name: p.name,
        monthlyFee: p.monthlyFee,
        speed: (p.name.match(/(\d+\s?M)/i) || [, p.category || ''])[1] || p.category || '',
        tags: [p.category].filter(Boolean)
      }));
      this.setData({ loadingPkg: false, packages: pkgs });
    }).catch(() => this.setData({ loadingPkg: false, packages: [] }));
  },
  loadRecentOrder() {
    this.setData({ loadingOrder: true });
    const cid = (getApp().globalData.customer && getApp().globalData.customer.id) || 'demo';
    api.getOrders(cid).then(list => {
      const o = (list || [])[0];
      this.setData({
        loadingOrder: false,
        recentOrder: o ? {
          id: o.id,
          name: o.packageName,
          status: STATUS_TEXT[o.status] || o.status,
          type: TYPE_TEXT[o.orderType] || o.orderType || ''
        } : null
      });
    }).catch(() => this.setData({ loadingOrder: false, recentOrder: null }));
  },
  goService() { wx.navigateTo({ url: '/pages/service/service' }); },
  goProfile() { wx.navigateTo({ url: '/pages/profile/profile' }); },
  goPkg() { wx.switchTab({ url: '/pages/package/list/list' }); },
  goOrder() { wx.switchTab({ url: '/pages/order/list/list' }); },
  goMove() { wx.navigateTo({ url: '/pages/move/move' }); },
  goUpgrade() { wx.navigateTo({ url: '/pages/upgrade/upgrade' }); },
  goCommunity() { wx.navigateTo({ url: '/pages/community/query/query' }); },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },
  goDetail(e) { wx.navigateTo({ url: '/pages/package/detail/detail?id=' + e.currentTarget.dataset.id }); }
});
