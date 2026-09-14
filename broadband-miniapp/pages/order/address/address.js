// pages/order/address/address.js —— 填写安装地址（下单前校验可装性）
const api = require('../../utils/api.js');

Page({
  data: {
    pkgId: '',
    community: '',           // 来自详情页/查询页携带的小区名
    communityInput: '',
    check: null,             // CommunityCheckResult
    canProceed: false,
    searched: false,
    timeSlots: ['09:00-11:00', '11:00-13:00', '14:00-16:00', '16:00-18:00', '19:00-21:00'],
    timeIndex: 0,
    form: { contact: '', phone: '', addressDetail: '' }
  },

  onLoad(options) {
    const pkgId = options.pkgId || '';
    const community = decodeURIComponent(options.community || '');
    this.setData({ pkgId, community, communityInput: community });
    if (community) this.doCheck(community);
  },

  onInput(e) {
    this.setData({ communityInput: e.detail.value });
  },

  // 复用 /api/community/check 校验小区可装性
  doCheck(name) {
    name = (name || this.data.communityInput || '').trim();
    if (!name) { wx.showToast({ title: '请输入小区名', icon: 'none' }); return; }
    api.checkCommunity(name).then((res) => {
      const r = (res && res.name) ? res : this.localFallback(name);
      this.setData({ check: r, canProceed: !!r.canProceed, searched: true, community: name });
    }).catch(() => {
      const r = this.localFallback(name);
      this.setData({ check: r, canProceed: !!r.canProceed, searched: true, community: name });
    });
  },

  localFallback(name) {
    const MOCK = {
      '南山科技园': { installable: true, portRemaining: 95, portStatus: 'AVAILABLE' },
      '保利花园': { installable: true, portRemaining: 1, portStatus: 'TIGHT' },
      '未覆盖村': { installable: false, portRemaining: 0, portStatus: 'UNAVAILABLE' }
    };
    const hit = MOCK[name] || (name.indexOf('科技园') >= 0 ? MOCK['南山科技园'] : null);
    if (hit) {
      return {
        name, region: '南山', installable: hit.installable, portRemaining: hit.portRemaining,
        portStatus: hit.portStatus, canProceed: hit.installable,
        message: hit.installable ? (hit.portStatus === 'TIGHT' ? '端口紧张，建议尽早下单' : '可安装') : '该小区暂未覆盖',
        suggestion: hit.installable ? '可立即办理' : '可登记需求'
      };
    }
    return {
      name, region: '', installable: false, portRemaining: 0, portStatus: 'UNAVAILABLE',
      canProceed: false, message: '未收录该小区，暂无法确认能否安装',
      suggestion: '可登记安装需求，覆盖后通知您'
    };
  },

  onFormInput(e) {
    const field = e.currentTarget.dataset.field;
    const form = Object.assign({}, this.data.form);
    form[field] = e.detail.value;
    this.setData({ form });
  },

  onTimeChange(e) {
    this.setData({ timeIndex: Number(e.detail.value) });
  },

  // 未覆盖 -> 去查询页换个小区
  changeCommunity() {
    wx.navigateTo({ url: '/pages/community/query/query' });
  },

  // 未覆盖 -> 登记需求
  register() {
    api.registerDemand({ name: this.data.community }).then(() => {
      wx.showToast({ title: '需求已登记，覆盖后通知', icon: 'success' });
    }).catch(() => {
      wx.showToast({ title: '需求已登记（本地）', icon: 'none' });
    });
  },

  // 下一步：校验通过才放行进入确认订单
  submit() {
    if (!this.data.canProceed) {
      wx.showToast({ title: '该小区暂不可安装，请更换', icon: 'none' });
      return;
    }
    const { pkgId, community, form, timeSlots, timeIndex } = this.data;
    console.log('[下单] pkgId=' + pkgId + ' community=' + community +
      ' contact=' + form.contact + ' phone=' + form.phone +
      ' addr=' + form.addressDetail + ' time=' + timeSlots[timeIndex]);
    wx.showToast({ title: '地址校验通过，进入确认订单', icon: 'success' });
    // 真实项目此处 navigateTo 到确认订单页 pages/order/confirm
  }
});
