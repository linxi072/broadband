/**
 * 业务状态 → 标签语义映射（三端统一口径）
 * ------------------------------------------------------------------
 * 返回 { type, label }，type 直接对应 Element Plus el-tag 的 type：
 *   success | info | warning | danger | primary
 *
 * 设计目标：消除各视图各自实现 statusType 造成的语义不一致。
 * 中文标签与英文状态码（后端枚举）都做归一，统一走这里。
 */

// 归一键：小写 + 去空格，便于命中
function key(s) {
  return String(s == null ? '' : s).trim().toLowerCase()
}

// 状态字典：支持中文标签、英文 code、别名同时映射
const MAP = {
  // —— 订单 / 工单 生命周期 ——
  待受理: { type: 'warning', label: '待受理' },
  pending: { type: 'warning', label: '待受理' },
  已支付: { type: 'success', label: '已支付' },
  paid: { type: 'success', label: '已支付' },
  支付成功: { type: 'success', label: '已支付' },
  安装中: { type: 'primary', label: '安装中' },
  已派单: { type: 'primary', label: '已派单' },
  assigned: { type: 'primary', label: '已派单' },
  处理中: { type: 'primary', label: '处理中' },
  已完成: { type: 'success', label: '已完成' },
  done: { type: 'success', label: '已完成' },
  已取消: { type: 'info', label: '已取消' },
  canceled: { type: 'info', label: '已取消' },
  cancelled: { type: 'info', label: '已取消' },

  // —— 评价 / 投诉 ——
  待处理: { type: 'danger', label: '待处理' },
  已回访: { type: 'primary', label: '已回访' },
  已闭环: { type: 'success', label: '已闭环' },
  已回复: { type: 'primary', label: '已回复' },
  已解决: { type: 'success', label: '已解决' },
  已驳回: { type: 'danger', label: '已驳回' },
  已关闭: { type: 'info', label: '已关闭' },
  投诉: { type: 'danger', label: '投诉' },
  好评: { type: 'success', label: '好评' },
  差评: { type: 'danger', label: '差评' },
  中评: { type: 'warning', label: '中评' },

  // —— 套餐升级申请 ——
  待审批: { type: 'warning', label: '待审批' },
  待审核: { type: 'warning', label: '待审核' },
  审批中: { type: 'warning', label: '待审批' },
  已通过: { type: 'success', label: '已通过' },
  已生效: { type: 'success', label: '已生效' },
  已拒绝: { type: 'danger', label: '已拒绝' },

  // —— SLA / 赔付 ——
  超时: { type: 'danger', label: '超时' },
  超时赔付: { type: 'danger', label: '超时赔付' },
  正常: { type: 'success', label: '正常' },
  达标: { type: 'success', label: '达标' },
  赔付中: { type: 'warning', label: '赔付中' },

  // —— 客户 ——
  正常: { type: 'success', label: '正常' },
  黑名单: { type: 'danger', label: '黑名单' },
  高危: { type: 'danger', label: '高危' },
  风险: { type: 'warning', label: '风险' },

  // —— 通用开关 / 布尔 ——
  启用: { type: 'success', label: '启用' },
  禁用: { type: 'info', label: '禁用' },
  是: { type: 'success', label: '是' },
  否: { type: 'info', label: '否' },
  在线: { type: 'success', label: '在线' },
  离线: { type: 'info', label: '离线' },

  // —— v1.14 运营留存 · 活动状态 ——
  进行中: { type: 'success', label: '进行中' },
  online: { type: 'success', label: '进行中' },
  已下线: { type: 'info', label: '已下线' },
  offline: { type: 'info', label: '已下线' },
  草稿: { type: 'warning', label: '草稿' },
  draft: { type: 'warning', label: '草稿' },

  // —— v1.15 支付流水状态机 ——
  created: { type: 'info', label: '已创建' },
  paying: { type: 'warning', label: '支付中' },
  支付中: { type: 'warning', label: '支付中' },
  failed: { type: 'danger', label: '支付失败' },
  支付失败: { type: 'danger', label: '支付失败' },
  closed: { type: 'info', label: '已关闭' },
  refunded: { type: 'warning', label: '已退款' },
  已退款: { type: 'warning', label: '已退款' },
  refund: { type: 'warning', label: '已退款' },

  // —— v1.15 数据智能 · 客户分群 ——
  new: { type: 'primary', label: '新客' },
  active: { type: 'success', label: '活跃' },
  at_risk: { type: 'warning', label: '预警' },
  churn_risk: { type: 'danger', label: '流失风险' },
  high_value: { type: 'primary', label: '高价值' },
  complaint: { type: 'warning', label: '投诉处理' }
}

const DEFAULT = { type: 'info', label: '—' }

/** 单个状态 → 语义元信息 */
export function statusMeta(status) {
  return MAP[key(status)] || { ...DEFAULT, label: String(status == null ? '—' : status) }
}

/** 兼容旧调用：仅返回 el-tag type */
export function statusType(status) {
  return statusMeta(status).type
}

/**
 * 集合为一行「状态点」文本（用于无组件环境，如纯文本渲染）
 * 返回带语义色的样式对象，配合内联使用。
 */
export function statusColor(status) {
  const map = {
    success: '#16a34a',
    info: '#6b7280',
    warning: '#d97706',
    danger: '#dc2626',
    primary: '#4f46e5'
  }
  return map[statusMeta(status).type] || '#6b7280'
}
