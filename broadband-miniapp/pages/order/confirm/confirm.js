const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    pkg: { name: '1000M 融合「美好家」', price: 129, id: 'pkg1000' },
    address: '保利花园 1-2-302',
    contact: '陈先生 138****0001',
    communityId: 'cm001',
    period: 24,
    // v1.15 支付真闭环状态机
    step: 'init',        // init -> paying -> paid
    outTradeNo: '',
    payParams: '',
    pollTimer: null
  },

  onLoad(options) {
    // 预留：从套餐/地址页带参进入（演示态使用默认值）
    if (options && options.packageId) {
      this.setData({ 'pkg.id': options.packageId });
    }
  },

  onUnload() {
    if (this.data.pollTimer) clearInterval(this.data.pollTimer);
  },

  getCustomerId() {
    const g = getApp().globalData || {};
    return g.customerId || (auth && auth.getCustomerId && auth.getCustomerId()) || 'demo';
  },

  // 提交订单 → 发起支付（异步）→ 模拟网关回调 → 轮询确认 PAID
  submit() {
    if (this.data.step === 'paying') return;
    const customerId = this.getCustomerId();
    wx.showLoading({ title: '提交中' });

    // 1) 下单（PENDING）
    api.createOrder({
      customerId: customerId,
      packageId: this.data.pkg.id,
      communityId: this.data.communityId,
      contactName: '陈先生',
      contactPhone: '13800000001',
      orderType: 'NEW_INSTALL'
    }).then((order) => {
      const orderId = order.orderId;
      // 2) 发起支付：返回商户单号与拉起参数，订单仍 PENDING（真闭环，未收款不置 PAID）
      return api.initiatePay(orderId, 'WECHAT_MOCK').then((pay) => {
        wx.hideLoading();
        if (!pay || !pay.outTradeNo) {
          wx.showToast({ title: '发起支付失败', icon: 'none' });
          return;
        }
        this.setData({ step: 'paying', outTradeNo: pay.outTradeNo, payParams: pay.payParams || '' });
        // 3) 开发态：调用模拟网关触发「支付成功」回调（真实微信环境此处调 wx.requestPayment）
        return api.simulatePay(pay.outTradeNo).then(() => this.startPolling(orderId));
      });
    }).catch((err) => {
      wx.hideLoading();
      wx.showToast({ title: (err && err.message) || '下单失败', icon: 'none' });
    });
  },

  // 4) 轮询支付状态，直到订单 PAID
  startPolling(orderId) {
    this.setData({ pollTimer: setInterval(() => {
      api.paymentStatus(orderId).then((st) => {
        if (st && (st.payStatus === 'PAID' || st.orderStatus === 'PAID')) {
          clearInterval(this.data.pollTimer);
          this.setData({ pollTimer: null, step: 'paid' });
          wx.showToast({ title: '支付成功' });
          setTimeout(() => wx.redirectTo({ url: '/pages/order/list/list' }), 1000);
        }
      }).catch(() => {});
    }, 1500) });
  }
});
