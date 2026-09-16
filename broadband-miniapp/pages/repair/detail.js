// pages/repair/detail.js —— 报修详情 + 进度时间线（V1.13）
const api = require('../../utils/api.js');

Page({
  data: {
    id: '',
    wo: null,
    sla: [],
    timeline: []
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.load();
  },

  load() {
    api.getRepairDetail(this.data.id)
      .then(r => {
        if (r && r.workOrder) {
          this.setData({
            wo: r.workOrder,
            sla: r.slaRecords || [],
            timeline: r.timeline || []
          });
        }
      })
      .catch(() => {});
  },

  goBack() {
    wx.navigateBack();
  }
});
