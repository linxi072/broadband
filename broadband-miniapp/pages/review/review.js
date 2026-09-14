const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
Page({
  data: { stars: 5, tags: ['准时','专业','速度快','态度好','规范'], selected: [], complaint: '' },
  onLoad(options) {

  },
  setStar(e) { this.setData({ stars: e.currentTarget.dataset.i }); },
  toggleTag(e) {
    const t = e.currentTarget.dataset.t; const s = this.data.selected; const i = s.indexOf(t);
    if (i >= 0) s.splice(i, 1); else s.push(t); this.setData({ selected: s });
  },
  onComplaint(e) { this.setData({ complaint: e.detail.value }); },
  submit() { wx.showToast({ title: this.data.stars === 5 && !this.data.complaint ? '感谢好评' : '已提交', icon: 'none' }); }
});
