const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');
Page({
  data: { packages: [
      { id: 'pkg500', name: '1000M 融合「美好家」', price: 129, speed: '1000M', tags: ['宽带','60G流量','IPTV','2副卡'], hot: true },
      { id: 'pkg300', name: '500M 全家享', price: 99, speed: '500M', tags: ['宽带','40G流量','IPTV'], hot: false },
      { id: 'pkg200', name: '300M 单宽带', price: 69, speed: '300M', tags: ['宽带'], hot: false }
    ] },
  onLoad(options) {

  },
  goDetail(e) { wx.navigateTo({ url: '/pages/package/detail/detail?id=' + e.currentTarget.dataset.id }); }
});
