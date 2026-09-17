// pages/account/bills.js —— 账单明细（V1.14 运营留存）
const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

function fmtDate(ms) {
  if (ms == null) return '';
  const d = new Date(typeof ms === 'number' ? ms : Number(ms));
  if (isNaN(d.getTime())) return String(ms);
  const p = n => (n < 10 ? '0' + n : '' + n);
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
}

function buildPeriods() {
  const arr = ['全部'];
  const now = Date.now();
  for (let i = 0; i < 6; i++) {
    const d = new Date(now);
    d.setMonth(d.getMonth() - i);
    const y = d.getFullYear();
    const m = (d.getMonth() + 1 < 10 ? '0' : '') + (d.getMonth() + 1);
    arr.push(y + '-' + m);
  }
  return arr;
}

const ORDER_TYPE = {
  PACKAGE: '套餐办理', UPGRADE: '套餐升级', REPAIR: '故障报修',
  TRAFFIC: '流量包', MOVE: '移机', OTHER: '其他业务'
};

Page({
  data: { list: [], loading: true, periods: [], periodIndex: 0 },

  onShow() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    this.setData({ periods: buildPeriods() });
    this.load();
  },

  load() {
    const app = getApp();
    const cid = (app.globalData.customer && app.globalData.customer.id) || 'demo';
    const idx = this.data.periodIndex;
    const period = idx === 0 ? '' : this.data.periods[idx];
    this.setData({ loading: true });
    api.getAccountBills(cid, period)
      .then(list => this.setData({
        list: (list || []).map(it => ({
          ...it,
          dateText: fmtDate(it.createdTime),
          typeLabel: ORDER_TYPE[it.orderType] || it.orderType || '业务'
        })),
        loading: false
      }))
      .catch(() => this.setData({ loading: false }));
  },

  onPeriod(e) {
    this.setData({ periodIndex: Number(e.detail.value) });
    this.load();
  }
});
