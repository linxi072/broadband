// pages/support/support.js —— 帮助中心 / 在线客服（V1.14 运营留存）
const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

const CATS = ['全部', '网络', '账单', '报修', '账户'];
const TYPES = ['业务咨询', '故障报修', '投诉建议', '其他'];
const TYPE_CODE = ['CONSULT', 'REPAIR', 'COMPLAINT', 'OTHER'];

Page({
  data: {
    faqs: [], loading: true,
    cats: CATS, catIdx: 0,
    types: TYPES,
    expandedId: '',
    form: { typeIdx: 0, content: '', contact: '' },
    submitting: false
  },

  onShow() { this.loadFaq(); },

  loadFaq() {
    const cat = this.data.cats[this.data.catIdx];
    api.getSupportFaq(cat === '全部' ? '' : cat)
      .then(list => this.setData({ faqs: list || [], loading: false }))
      .catch(() => this.setData({ loading: false }));
  },

  onCat(e) {
    this.setData({ catIdx: Number(e.currentTarget.dataset.idx) });
    this.loadFaq();
  },

  toggle(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ expandedId: this.data.expandedId === id ? '' : id });
  },

  onType(e) { this.setData({ 'form.typeIdx': Number(e.detail.value) }); },
  onContent(e) { this.setData({ 'form.content': e.detail.value }); },
  onContact(e) { this.setData({ 'form.contact': e.detail.value }); },

  submit() {
    if (this.data.submitting) return;
    const content = (this.data.form.content || '').trim();
    if (!content) { wx.showToast({ title: '请填写咨询内容', icon: 'none' }); return; }
    const app = getApp();
    const cid = (app.globalData.customer && app.globalData.customer.id) || null;
    this.setData({ submitting: true });
    api.submitSupportTicket({
      customerId: cid,
      type: TYPE_CODE[this.data.form.typeIdx],
      content: content,
      contact: (this.data.form.contact || '').trim() || null
    })
      .then(() => {
        this.setData({ submitting: false, form: { typeIdx: 0, content: '', contact: '' }, expandedId: '' });
        wx.showToast({ title: '提交成功', icon: 'success' });
      })
      .catch(err => {
        this.setData({ submitting: false });
        wx.showToast({ title: (err && err.message) || '提交失败', icon: 'none' });
      });
  }
});
