// pages/service/service.js
Page({
  data: {
    // 服务大厅九宫格（设计稿 v1.8 服务聚合页）
    services: [
      { key: 'new', icon: '📶', name: '新装宽带', desc: '预约上门安装', action: 'pkg' },
      { key: 'move', icon: '🚚', name: '宽带移机', desc: '搬家不改号', action: 'todo' },
      { key: 'renew', icon: '💳', name: '续费', desc: '套餐续约', action: 'todo' },
      { key: 'upgrade', icon: '⬆️', name: '套餐升级', desc: '升档 / 加购', action: 'upgrade' },
      { key: 'speed', icon: '⚡', name: '宽带提速', desc: '升级速率', action: 'todo' },
      { key: 'repair', icon: '🔧', name: '故障报修', desc: '报障与进度', action: 'todo' },
      { key: 'traffic', icon: '📊', name: '流量监控', desc: '用量 / 预警', action: 'traffic' },
      { key: 'smart', icon: '🏠', name: '智慧家庭', desc: 'FTTR/看家/云', action: 'pkg' },
      { key: 'self', icon: '🛠', name: '自助服务', desc: '测速/账单/发票', action: 'todo' },
      { key: 'query', icon: '🔍', name: '覆盖查询', desc: '能否安装', action: 'todo' }
    ]
  },
  onTap(e) {
    const item = e.currentTarget.dataset.item;
    if (item.action === 'pkg') {
      wx.navigateTo({ url: '/pages/package/detail/detail?id=demo' });
    } else if (item.key === 'query') {
      wx.navigateTo({ url: '/pages/community/query/query' });
    } else if (item.action === 'upgrade') {
      wx.navigateTo({ url: '/pages/package/upgrade/upgrade' });
    } else if (item.action === 'traffic') {
      wx.navigateTo({ url: '/pages/traffic/traffic' });
    } else {
      wx.showToast({ title: '功能建设中', icon: 'none' });
    }
  }
});
