// pages/package/upgrade/upgrade.js —— 套餐升级（自助升档 + 补差预览）
const api = require('../../../utils/api.js');

Page({
  data: {
    loading: true,
    current: null,          // 当前套餐 {name, fee, contractLeftMonths}
    options: [],            // 升档选项（带宽/增值服务）
    selectedBand: '',       // 选中的带宽项 key
    selectedAddons: [],     // 选中的增值服务 key 列表
    effectType: 'immediate',// immediate | nextMonth
    preview: null           // 费用预览
  },

  onLoad() {
    this.loadOptions();
  },

  loadOptions() {
    api.getUpgradeOptions({ customerId: 'demo' }).then(res => {
      const d = (res && res.data) ? res.data : res;
      if (!d) { this.fallback(); return; }
      this.setData({ current: d.current, options: d.options || [], loading: false });
      const bands = (d.options || []).filter(o => o.type === 'bandwidth');
      if (bands.length) this.setData({ selectedBand: bands[0].key });
      this.calc();
    }).catch(() => this.fallback());
  },

  fallback() {
    // mock 兜底，保证骨架可演示（后端未实现时）
    const current = { name: '500M 融合', fee: 99, contractLeftMonths: 18 };
    const options = [
      { type: 'bandwidth', key: 'b500', name: '维持 500M', extraFee: 0 },
      { type: 'bandwidth', key: 'b1000', name: '1000M', extraFee: 40 },
      { type: 'bandwidth', key: 'b2000', name: '2000M', extraFee: 90 },
      { type: 'addon', key: 'fttr', name: 'FTTR', extraFee: 30 },
      { type: 'addon', key: 'wifi', name: '全屋WiFi', extraFee: 15 },
      { type: 'addon', key: 'watch', name: '看家', extraFee: 10 }
    ];
    this.setData({ current, options, loading: false, selectedBand: 'b1000' });
    this.calc();
  },

  selectBand(e) {
    this.setData({ selectedBand: e.currentTarget.dataset.key });
    this.calc();
  },
  toggleAddon(e) {
    const key = e.currentTarget.dataset.key;
    const arr = this.data.selectedAddons.slice();
    const i = arr.indexOf(key);
    if (i >= 0) arr.splice(i, 1); else arr.push(key);
    this.setData({ selectedAddons: arr });
    this.calc();
  },
  selectEffect(e) {
    this.setData({ effectType: e.currentTarget.dataset.val });
    this.calc();
  },

  // 补差计算：月差 = 带宽加价 + 增值服务加价；一次性补差 = 月差 × 剩余合约月数
  calc() {
    const { options, selectedBand, selectedAddons, current, effectType } = this.data;
    const band = options.find(o => o.key === selectedBand);
    const addons = options.filter(o => o.type === 'addon' && selectedAddons.indexOf(o.key) >= 0);
    const monthDiff = (band ? band.extraFee : 0) + addons.reduce((s, o) => s + o.extraFee, 0);
    const fee = (current ? current.fee : 99) + monthDiff;
    const leftMonths = current ? current.contractLeftMonths : 18;
    const oneTime = Math.round(monthDiff * leftMonths);
    this.setData({ preview: { monthDiff, fee, oneTime, effectType } });
  },

  submit() {
    const payload = {
      customerId: 'demo',
      targetBand: this.data.selectedBand,
      addons: this.data.selectedAddons,
      effectType: this.data.effectType
    };
    api.submitUpgrade(payload).then(() => {
      wx.showToast({ title: '升级申请已提交', icon: 'success' });
    }).catch(() => {
      wx.showToast({ title: '已提交（mock）', icon: 'none' });
    });
  }
});
