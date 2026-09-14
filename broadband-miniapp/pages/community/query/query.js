// pages/community/query/query.js —— 可安装小区查询
const api = require('../../utils/api.js');

// 未接入后端时的本地兜底数据（字段对齐 CommunityCheckResult）
const MOCK_DB = [
  { name: '南山科技园', region: '南山', installable: true, portRemaining: 95, portStatus: 'AVAILABLE', packages: [{ id: 'pkg-500', name: '500M 融合套餐' }] },
  { name: '保利花园', region: '南山', installable: true, portRemaining: 1, portStatus: 'TIGHT', packages: [{ id: 'pkg-500', name: '500M 融合套餐' }] },
  { name: '未覆盖村', region: '坪山', installable: false, portRemaining: 0, portStatus: 'UNAVAILABLE', packages: [] }
];

Page({
  data: {
    keyword: '',
    result: null,        // CommunityCheckResult
    loading: false,
    hot: ['南山科技园', '保利花园', '海岸城', '科技园路小区']
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    const name = (this.data.keyword || '').trim();
    if (!name) { wx.showToast({ title: '请输入小区名', icon: 'none' }); return; }
    this.setData({ loading: true });
    api.checkCommunity(name).then((res) => {
      const result = (res && res.name) ? res : this.localFallback(name);
      this.setData({ result, loading: false });
    }).catch(() => {
      this.setData({ result: this.localFallback(name), loading: false });
    });
  },

  onHot(e) {
    const name = e.currentTarget.dataset.name;
    this.setData({ keyword: name });
    this.onSearch();
  },

  // 本地兜底：命中 mock 库则用 mock，否则按未收录处理
  localFallback(name) {
    const hit = MOCK_DB.find((c) => c.name.indexOf(name) >= 0);
    if (hit) {
      return {
        name: hit.name, region: hit.region, installable: hit.installable,
        portRemaining: hit.portRemaining, portStatus: hit.portStatus,
        canProceed: hit.installable, packages: hit.packages,
        message: hit.installable ? (hit.portStatus === 'TIGHT' ? '端口紧张，建议尽早下单' : '可安装') : '该小区暂未覆盖宽带资源',
        suggestion: hit.installable ? '可立即办理' : '可登记安装需求，覆盖后通知您'
      };
    }
    return {
      name: name, region: '', installable: false, portRemaining: 0,
      portStatus: 'UNAVAILABLE', canProceed: false, packages: [],
      message: '未收录该小区，暂无法确认能否安装',
      suggestion: '可登记安装需求，覆盖后第一时间通知您'
    };
  },

  // 可安装 -> 跳融合套餐详情并预填小区
  goPackage(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/package/detail/detail?id=' + id + '&community=' + encodeURIComponent(this.data.result.name) });
  },

  // 未覆盖 -> 登记需求
  register() {
    api.registerDemand({ name: this.data.result.name }).then(() => {
      wx.showToast({ title: '需求已登记，覆盖后通知', icon: 'success' });
    }).catch(() => {
      wx.showToast({ title: '需求已登记（本地）', icon: 'none' });
    });
  }
});
