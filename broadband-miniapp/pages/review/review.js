// pages/review/review.js
const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

const HINT = { 1: '很不满意', 2: '不太满意', 3: '一般', 4: '满意', 5: '非常满意' };

Page({
  data: { stars: 5, starHint: '非常满意', tags: ['准时', '专业', '速度快', '态度好', '规范'], selected: [], complaint: '' },
  onLoad(options) { },
  setStar(e) {
    const s = e.currentTarget.dataset.i;
    this.setData({ stars: s, starHint: HINT[s] || '' });
  },
  toggleTag(e) {
    const t = e.currentTarget.dataset.t;
    const s = this.data.selected;
    const i = s.indexOf(t);
    if (i >= 0) s.splice(i, 1); else s.push(t);
    this.setData({ selected: s });
  },
  onComplaint(e) { this.setData({ complaint: e.detail.value }); },
  submit() {
    wx.showToast({ title: this.data.stars === 5 && !this.data.complaint ? '感谢好评' : '已提交', icon: 'none' });
  }
});
