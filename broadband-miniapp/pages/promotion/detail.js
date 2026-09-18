// pages/promotion/detail.js —— 优惠活动详情（V1.14 运营留存）
const api = require('../../utils/api.js');

function describeRule(r) {
  if (!r) return [];
  try {
    const o = typeof r === 'string' ? JSON.parse(r) : r;
    if (o.kind === 'buy_x_get_y') return ['购买 ' + o.buy + ' 个月，赠送 ' + o.get + ' 个月', '计费单位：' + (o.unit || 'month')];
    if (o.kind === 'gift') return ['赠送权益：' + (o.items || []).join('、')];
    if (o.kind === 'cut') return ['每' + (o.unit === 'month' ? '月' : (o.unit || '')) + '立减 ¥' + o.amount];
    return [JSON.stringify(o)];
  } catch (e) {
    return [String(r)];
  }
}

Page({
  data: { id: '', p: null, rules: [], loading: true },

  onLoad(options) {
    this.setData({ id: options.id });
    this.load();
  },

  load() {
    api.getPromotionDetail(this.data.id)
      .then(p => this.setData({ p: p || {}, rules: describeRule(p && p.ruleJson), loading: false }))
      .catch(() => this.setData({ loading: false }));
  }
});
