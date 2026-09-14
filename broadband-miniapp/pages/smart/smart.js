const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { items: [
      { id: 'fttr', icon: '📡', name: 'FTTR 全屋光纤', desc: '千兆到每个房间', price: '+¥30/月' },
      { id: 'wifi', icon: '📶', name: '全屋 WiFi', desc: '专业组网方案', price: '+¥15/月' },
      { id: 'cam', icon: '🎥', name: '看家监控', desc: '云存储回看', price: '+¥10/月' },
      { id: 'cloud', icon: '☁️', name: '家庭云', desc: '2TB 存储空间', price: '+¥12/月' },
      { id: 'tv', icon: '📺', name: '电视会员', desc: '海量影视', price: '+¥19/月' }
    ] },
  onLoad(options) {

  },
  add(e) { wx.showToast({ title: '已加购：' + e.currentTarget.dataset.name }); }
});
