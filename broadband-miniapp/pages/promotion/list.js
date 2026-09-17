// pages/promotion/list.js —— 优惠活动列表（V1.14 运营留存）
const api = require('../../utils/api.js');

const TYPES = [
  { key: '', label: '全部' },
  { key: 'ANNUAL', label: '包年' },
  { key: 'NEW_INSTALL', label: '新装' },
  { key: 'COMBO', label: '融合' }
];

Page({
  data: { list: [], loading: true, types: TYPES, typeIdx: 0 },

  onShow() { this.load(); },

  load() {
    const type = this.data.types[this.data.typeIdx].key;
    this.setData({ loading: true });
    api.getPromotions(type)
      .then(list => this.setData({ list: list || [], loading: false }))
      .catch(() => this.setData({ loading: false }));
  },

  onSeg(e) {
    this.setData({ typeIdx: Number(e.currentTarget.dataset.idx) });
    this.load();
  },

  open(e) { wx.navigateTo({ url: '/pages/promotion/detail?id=' + e.currentTarget.dataset.id }); }
});
