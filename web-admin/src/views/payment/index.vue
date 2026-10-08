<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import { adminPayTransactions, adminPayRefund } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoPayTransactions } from '@/mock/fallback'

const transactions = ref([])
const live = ref(false)
const loading = ref(true)

const tab = ref('transactions')
const statusFilter = ref('')

const refundTarget = ref(null)
const refundReason = ref('')
const refunding = ref(false)

const filtered = computed(() => {
  if (!statusFilter.value) return transactions.value
  return transactions.value.filter((t) => t.status === statusFilter.value)
})

const stats = computed(() => {
  const list = transactions.value || []
  const sum = (s) => list.filter((t) => t.status === s).reduce((a, t) => a + (t.amount || 0), 0)
  const paid = list.filter((t) => t.status === 'PAID').length
  return {
    paid: sum('PAID'),
    paying: sum('PAYING'),
    refunded: sum('REFUNDED'),
    paidCount: paid,
    total: list.length
  }
})

async function load() {
  loading.value = true
  const r = await loadResource(
    () => adminPayTransactions({ status: statusFilter.value || undefined }),
    () => demoPayTransactions
  )
  transactions.value = r.data || []
  live.value = r.live
  loading.value = false
}

function statusType(s) {
  return s === 'PAID' ? 'success' : s === 'PAYING' ? 'warning' : s === 'REFUNDED' ? 'info' : 'danger'
}
function statusText(s) {
  return { PAID: '已支付', PAYING: '支付中', REFUNDED: '已退款', CLOSED: '已关闭' }[s] || s
}

async function onRefund() {
  if (!refundTarget.value) return ElMessage.warning('请选择一笔已支付流水')
  refunding.value = true
  try {
    await adminPayRefund({ outTradeNo: refundTarget.value.outTradeNo, reason: refundReason.value })
    ElMessage.success(`已提交退款：${refundTarget.value.outTradeNo}`)
  } catch (e) {
    ElMessage.warning('后端不可达，已模拟退款成功（演示）')
  } finally {
    refundTarget.value = null
    refundReason.value = ''
    refunding.value = false
    await load()
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">支付管理 · 流水与退款</h2>
        <p class="page-sub">微信支付真闭环流水查询与退款处理（v1.15 支付真闭环，Mock/真实网关统一 pay_transaction 状态机）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="mini-stats">
      <StatCard label="已支付金额" :value="'¥' + stats.paid.toLocaleString()" foot="累计已收" />
      <StatCard label="支付中" :value="stats.paying" unit="元" tone="down" foot="待网关回调" />
      <StatCard label="已退款" :value="stats.refunded" unit="元" tone="down" foot="累计退款" />
      <StatCard label="支付成功率" :value="stats.total ? Math.round((stats.paidCount / stats.total) * 100) : 0" unit="%" foot="已支付 / 总笔数" />
    </div>

    <div class="card">
      <div class="card-head">
        <el-radio-group v-model="tab">
          <el-radio-button label="transactions">支付流水</el-radio-button>
          <el-radio-button label="refund">退款处理</el-radio-button>
        </el-radio-group>
        <el-select
          v-if="tab === 'transactions'"
          v-model="statusFilter"
          placeholder="全部状态"
          clearable
          style="width: 140px"
          @change="load"
        >
          <el-option label="已支付" value="PAID" />
          <el-option label="支付中" value="PAYING" />
          <el-option label="已退款" value="REFUNDED" />
        </el-select>
      </div>

      <el-table v-if="tab === 'transactions'" v-loading="loading" :data="filtered" size="small" style="width: 100%">
        <el-table-column prop="outTradeNo" label="商户单号" min-width="180" />
        <el-table-column prop="customerName" label="客户" width="100" />
        <el-table-column prop="packageName" label="套餐" min-width="150" show-overflow-tooltip />
        <el-table-column label="渠道" width="120">
          <template #default="{ row }">{{ row.channel === 'WECHAT_MOCK' ? '微信(模拟)' : row.channel }}</template>
        </el-table-column>
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }"><b>¥{{ row.amount }}</b></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="150">
          <template #default="{ row }">{{ row.paidTime ? new Date(row.paidTime).toLocaleString('zh-CN') : '—' }}</template>
        </el-table-column>
      </el-table>

      <div v-else class="refund-box">
        <el-alert type="info" :closable="false" title="退款将调用 /api/admin/pay/refund（payment:view），仅对已支付流水可发起。" />
        <div class="refund-form">
          <el-select
            v-model="refundTarget"
            value-key="outTradeNo"
            placeholder="选择一笔已支付流水"
            style="width: 380px"
            filterable
          >
            <el-option
              v-for="t in transactions.filter((t) => t.status === 'PAID')"
              :key="t.outTradeNo"
              :value="t"
              :label="`${t.customerName} · ${t.packageName} · ¥${t.amount} · ${t.outTradeNo}`"
            />
          </el-select>
          <el-input v-model="refundReason" placeholder="退款原因（选填）" style="width: 240px" />
          <el-button type="danger" :loading="refunding" @click="onRefund">发起退款</el-button>
        </div>
        <p class="hint-line">演示数据下退款为模拟操作；接入真实微信支付后，将同步调用微信退款 API 并回写 pay_transaction 状态。</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 18px; }
.page-sub { margin: 4px 0 0; font-size: 13px; color: var(--bd-text-mute); }
.mini-stats { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.mini-stats > * { flex: 1; min-width: 160px; }
.card { margin-bottom: 16px; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.refund-box { padding: 8px 0; }
.refund-form { display: flex; gap: 12px; align-items: center; margin: 16px 0; flex-wrap: wrap; }
.hint-line { font-size: 13px; color: var(--bd-text-mute); margin-top: 8px; }
</style>
