const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

function speedOf(name) {
  const m = /(\d+\s?M)/i.exec(name || '');
  return m ? m[1].toUpperCase() : '';
}

Page({
  data: { loading: true, packages: [] },
  onLoad(options) {
    // 修复：原先是写死的静态数据；现从开放层 GET /api/package/list 拉取真实在售套餐
    api.listPackages()
      .then(list => {
        this.setData({
          loading: false,
          packages: (list || []).map(p => ({
            id: p.id,
            name: p.name,
            hot: false,
            speed: speedOf(p.name) || p.category || '',
            price: p.monthlyFee,
            tags: [p.category].filter(Boolean).concat(p.cover ? [] : [])
          }))
        });
      })
      .catch(() => { this.setData({ loading: false, packages: [] }); });
  },
  goDetail(e) { wx.navigateTo({ url: '/pages/package/detail/detail?id=' + e.currentTarget.dataset.id }); }
});
