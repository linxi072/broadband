<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import ChartBox from '@/components/ChartBox.vue'
import DetailDrawer from '@/components/DetailDrawer.vue'
import { financeReport, slaCompensations, financeReportDetail } from '@/api/business'
import { loadResource, money } from '@/composables/useResource'
import { demoFinance, demoFinanceDetail } from '@/mock/fallback'

const rows = ref([])
const live = ref(false)
const loading = ref(true)
const comps = ref([])

const latest = computed(() => rows.value[0] || {})

const trendOption = computed(() => {
  const list = [...rows.value].reverse()
  return {
    grid: { left: 8, right: 16, top: 28, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收', '退款', '赔付'], right: 8, top: 0, textStyle: { color: '#6b7280', fontSize: 11 } },
    xAxis: {
      type: 'category',
      data: list.map((r) => r.month),
      axisLabel: { color: '#6b7280', fontSize: 11 }
    },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#9ca3af', fontSize: 11 } },
    series: [
      { name: '营收', type: 'line', smooth: true, data: list.map((r) => r.revenue), itemStyle: { color: '#4f46e5' }, lineStyle: { width: 2.5 }, areaStyle: { color: 'rgba(79,70,229,0.12)' } },
      { name: '退款', type: 'line', smooth: true, data: list.map((r) => r.refund), itemStyle: { color: '#f59e0b' } },
      { name: '赔付', type: 'line', smooth: true, data: list.map((r) => r.compensation), itemStyle: { color: '#dc2626' } }
    ]
  }
})

onMounted(async () => {
  const [f, c] = await Promise.all([
    loadResource(() => financeReport(), demoFinance),
    loadResource(() => slaCompensations(), [])
  ])
  const data = f.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || demoFinance
  live.value = f.live
  comps.value = Array.isArray(c.data) ? c.data : []
  loading.value = false
})

// ---- 财务月度下钻（US-2.2）：点击趋势图某月份，查看该月底层订单 + 赔付明细
const detail = ref([])
const detailLoading = ref(false)
const detailVisible = ref(false)
const detailTitle = ref('')
const detailColumns = [
  { prop: 'type', label: '类型', width: 80 },
  { prop: 'ref', label: '单号/工单', minWidth: 150 },
  { prop: 'customer', label: '客户', width: 120 },
  { prop: 'amount', label: '金额', width: 110, format: (r) => money(r.amount) },
  { prop: 'status', label: '状态', width: 110 },
  { prop: 'reason', label: '备注', minWidth: 200, format: (r) => r.reason || '—' },
  { prop: 'createdDate', label: '日期', width: 120 }
]

async function onFinanceChartClick(params) {
  if (!params || !params.name) return
  const month = params.name
  detailTitle.value = `财务明细下钻 · ${month}`
  detailLoading.value = true
  detailVisible.value = true
  const r = await loadResource(() => financeReportDetail({ month }), () => demoFinanceDetail(month))
  const d = r.data || {}
  detail.value = d.rows || []
  detailLoading.value = false
}
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">财务管理</h2>
        <p class="page-sub">营收、退款、SLA 赔付与应收对账（由 biz_order + compensation 聚合）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat is-up"><div class="label">本月营收</div><div class="value">{{ money(latest.revenue) }}</div></div>
      <div class="stat is-warn"><div class="label">本月退款</div><div class="value">{{ money(latest.refund) }}</div></div>
      <div class="stat is-warn"><div class="label">SLA 赔付</div><div class="value">{{ money(latest.compensation) }}</div></div>
      <div class="stat"><div class="label">应收账款</div><div class="value">{{ money(latest.receivable) }}</div></div>
    </div>

    <div class="card">
      <div class="card-head"><h3>近 3 月收支趋势<span class="hint">点击月份下钻明细</span></h3><SourceTag :live="live" /></div>
      <div class="card-body"><ChartBox :option="trendOption" height="280px" @chart-click="onFinanceChartClick" /></div>
    </div>

    <div class="card">
      <div class="card-head"><h3>月度对账</h3></div>
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="month" label="月份" width="120" />
        <el-table-column label="营收" width="150">
          <template #default="{ row }"><b class="up">{{ money(row.revenue) }}</b></template>
        </el-table-column>
        <el-table-column label="退款" width="130">
          <template #default="{ row }">{{ money(row.refund) }}</template>
        </el-table-column>
        <el-table-column label="赔付" width="130">
          <template #default="{ row }">{{ money(row.compensation) }}</template>
        </el-table-column>
        <el-table-column label="净收入" width="150">
          <template #default="{ row }">
            {{ money(Number(row.revenue || 0) - Number(row.refund || 0) - Number(row.compensation || 0)) }}
          </template>
        </el-table-column>
        <el-table-column label="应收" width="140">
          <template #default="{ row }">{{ money(row.receivable) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === '已结账' ? 'success' : 'warning'" size="small" effect="light">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty><EmptyState icon="💰" title="暂无财务数据" desc="订单营收与对账数据将在此汇总" /></template>
      </el-table>
    </div>

    <div class="card">
      <div class="card-head">
        <h3>赔付明细（影响财务成本）</h3>
        <span class="hint">共 {{ comps.length }} 笔</span>
      </div>
      <el-table :data="comps.slice(0, 8)" size="small">
        <el-table-column prop="orderId" label="工单号" min-width="130" />
        <el-table-column prop="custName" label="客户" width="110" />
        <el-table-column label="赔付额度" width="120">
          <template #default="{ row }"><b class="up">{{ row.compAmount }}</b></template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        <template #empty><EmptyState icon="🛡" title="暂无赔付记录" desc="SLA 超时触发赔付后将在此留痕" /></template>
      </el-table>
    </div>

    <DetailDrawer
      v-model:visible="detailVisible"
      :title="detailTitle"
      :columns="detailColumns"
      :rows="detail"
      :loading="detailLoading"
      :live="live"
    />
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.stat-grid {
  margin: 0 0 16px;
}

.up {
  color: var(--bd-up);
}

.hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--bd-text-mute);
  margin-left: 8px;
}
</style>
