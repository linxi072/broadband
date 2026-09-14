// pages/package/detail/detail.js
const api = require('../../utils/api.js');

// 融合套餐 mock（后端 /api/package/detail 未实现时兜底；字段对齐设计稿 v1.8）
const MOCK = {
  id: 'demo',
  name: '500M 融合套餐',
  // 主图 + 轮播图（此处用色块占位，真实为图片 URL 数组）
  images: [
    { color: '#2563eb', label: '主图 · 全家共享' },
    { color: '#1d4ed8', label: '轮播 · 宽带+手机' },
    { color: '#3b82f6', label: '轮播 · FTTR 全屋光纤' }
  ],
  monthlyFee: 79,
  originalFee: 129,
  // 套餐组成（融合：宽带+手机+IPTV+副卡）
  converge: [
    { label: '宽带', desc: '500M 高速宽带' },
    { label: '手机', desc: '30GB 流量 + 500 分钟' },
    { label: 'IPTV', desc: '4K 超清电视' },
    { label: '副卡', desc: '2 张共享副卡' }
  ],
  // 动态可选参数（对应后台套餐管理「动态参数组」）
  params: [
    {
      key: 'bandwidth', name: '宽带速率', type: 'single', required: true,
      options: [
        { value: '300M', extraFee: 0 },
        { value: '500M', extraFee: 0 },
        { value: '1000M', extraFee: 30 }
      ]
    },
    {
      key: 'contract', name: '合约期', type: 'single', required: true,
      options: [
        { value: '12个月', extraFee: 0 },
        { value: '24个月', extraFee: 0 },
        { value: '36个月', extraFee: 0 }
      ]
    },
    {
      key: 'addon', name: '增值服务', type: 'multi', required: false,
      options: [
        { value: 'FTTR全屋光纤', extraFee: 30 },
        { value: '全屋WiFi', extraFee: 15 },
        { value: '移动看家', extraFee: 10 },
        { value: '家庭云', extraFee: 10 }
      ]
    }
  ],
  deposit: 200,        // 调测费
  deviceRent: 10,      // 设备租赁/月
  penalty: '合约期内提前解约，按剩余月份 30% 赔付违约金'
};

Page({
  data: {
    pkg: null,
    selected: {},   // groupKey -> 单选值 或 多选数组
    totalFee: 0,
    swiperIndex: 0,
    community: ''   // 来自查询页携带的小区名
  },

  onLoad(options) {
    const id = options.id || 'demo';
    const community = decodeURIComponent(options.community || '');
    this.setData({ community });
    this.loadDetail(id);
  },

  loadDetail(id) {
    api.getPackageDetail(id).then((res) => {
      const pkg = (res && res.id) ? res : MOCK;
      this.initSelected(pkg);
      this.setData({ pkg });
    }).catch(() => {
      this.initSelected(MOCK);
      this.setData({ pkg: MOCK });
    });
  },

  initSelected(pkg) {
    const selected = {};
    (pkg.params || []).forEach((g) => {
      selected[g.key] = (g.type === 'single')
        ? (g.options[0] ? g.options[0].value : '')
        : [];
    });
    this.setData({ selected });
    this.calcTotal();
  },

  onSwiperChange(e) {
    this.setData({ swiperIndex: e.detail.current });
  },

  // 单选（带宽 / 合约期）
  onSelectSingle(e) {
    const { group, value } = e.currentTarget.dataset;
    const selected = Object.assign({}, this.data.selected);
    selected[group] = value;
    this.setData({ selected });
    this.calcTotal();
  },

  // 多选（增值服务）
  onToggleMulti(e) {
    const { group, value } = e.currentTarget.dataset;
    const selected = Object.assign({}, this.data.selected);
    const arr = selected[group] ? selected[group].slice() : [];
    const idx = arr.indexOf(value);
    if (idx >= 0) arr.splice(idx, 1); else arr.push(value);
    selected[group] = arr;
    this.setData({ selected });
    this.calcTotal();
  },

  calcTotal() {
    const pkg = this.data.pkg;
    if (!pkg) return;
    let total = pkg.monthlyFee || 0;
    const selected = this.data.selected;
    (pkg.params || []).forEach((g) => {
      if (g.type !== 'multi') return;
      (selected[g.key] || []).forEach((v) => {
        const opt = g.options.find((o) => o.value === v);
        if (opt && opt.extraFee) total += opt.extraFee;
      });
    });
    this.setData({ totalFee: total });
  },

  // 联动小区覆盖查询（设计稿 v1.8：下单前校验可装性）
  goCommunity() {
    wx.showToast({ title: '能力校验：前往小区覆盖查询', icon: 'none' });
  },

  // 立即办理 -> 跳转填写安装地址（携带已选参数）
  submit() {
    const pkg = this.data.pkg;
    const params = encodeURIComponent(JSON.stringify(this.data.selected));
    wx.showToast({ title: '已选 ' + this.data.totalFee + ' 元/月', icon: 'success' });
    setTimeout(() => {
      wx.showToast({ title: '跳转安装地址校验（待建页面）', icon: 'none' });
    }, 800);
    console.log('[下单] pkgId=' + pkg.id + ' selected=' + params);
  }
});
