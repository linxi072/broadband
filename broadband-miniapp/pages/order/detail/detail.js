const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');
Page({
  data: { order: { id: 'B20260901001', name: '1000M 融合「美好家」', addr: '保利花园 1-2-302', contact: '陈先生 138****0001', amount: 129, time: '2026-09-01 10:30', statusText: '安装中', steps: ['已下单','已受理','师傅已派单','安装中','已完成'], current: 3 } },
  onLoad(options) {

  },
  call() { wx.makePhoneCall({ phoneNumber: '13800000001' }); },
  cancel() { wx.showToast({ title: '已申请取消', icon: 'none' }); }
});
