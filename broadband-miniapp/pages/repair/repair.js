// pages/repair/repair.js —— 故障报修提交表单（V1.13）
const api = require('../../utils/api.js');

Page({
  data: {
    customerId: 'demo',
    categories: [],
    categoryIndex: 0,
    faultDesc: '',
    contactPhone: ''
  },

  onLoad() {
    const app = getApp();
    const customer = (app.globalData && app.globalData.customer) || {};
    this.setData({ customerId: customer.id || 'demo' });
    if (customer.phone) this.setData({ contactPhone: customer.phone });
    this.loadCategories();
  },

  loadCategories() {
    api.getRepairCategories()
      .then(list => this.setData({ categories: list || [] }))
      .catch(() => this.setData({ categories: [] }));
  },

  onCategoryChange(e) {
    this.setData({ categoryIndex: Number(e.detail.value) });
  },

  onDescInput(e) {
    this.setData({ faultDesc: e.detail.value });
  },

  onPhoneInput(e) {
    this.setData({ contactPhone: e.detail.value });
  },

  submit() {
    const { categories, categoryIndex, faultDesc, contactPhone, customerId } = this.data;
    const cat = categories[categoryIndex];
    if (!cat) {
      wx.showToast({ title: '请选择故障类型', icon: 'none' });
      return;
    }
    if (!faultDesc || !faultDesc.trim()) {
      wx.showToast({ title: '请描述故障现象', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '提交中' });
    api.createRepair({
      customerId,
      faultCategory: cat.value,
      faultDesc: faultDesc.trim(),
      contactPhone: (contactPhone || '').trim()
    }).then(r => {
      wx.hideLoading();
      if (r && r.ok) {
        wx.showToast({ title: '报修提交成功', icon: 'success' });
        setTimeout(() => wx.redirectTo({ url: '/pages/repair/list' }), 800);
      } else {
        wx.showToast({ title: '提交失败', icon: 'none' });
      }
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '提交失败', icon: 'none' });
    });
  }
});
