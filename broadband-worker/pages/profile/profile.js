const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { worker: { name: '王师傅', no: 'W1001', star: 4.8, phone: '139****0002' }, stats: [ { n: 128, l: '累计工单' }, { n: 4.8, l: '平均星级' }, { n: 98, l: '好评率%' } ], tags: ['准时','专业','耐心'] },
  onShow() {
    if (!auth.isLogin()) { wx.redirectTo({ url: '/pages/login/login' }); return; }
    if (getApp().globalData.worker) this.setData({ worker: getApp().globalData.worker });
  },
  logout() { getApp().logout(); wx.reLaunch({ url: '/pages/login/login' }); }
});
