<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import ChartBox from '@/components/ChartBox.vue'
import DetailDrawer from '@/components/DetailDrawer.vue'
import { salesReport, salesReportDetail } from '@/api/business'
import { loadResource, money } from '@/composables/useResource'
import { demoSales, demoSalesDetail } from '@/mock/fallback'

const rows = ref([])
const live = ref(false)
const loading = ref(true)

const total = computed(() => ({
  orders: rows.value.reduce((s, r) => s + Number(r.orders || 0), 0),
  amount: rows.value.reduce((s, r) => s + Number(r.amount || 0), 0),
  doneRate: rows.value.length
    ? (rows.value.reduce((s, r) => s + Number(r.doneRate || 0), 0) / rows.value.length).toFixed(1)
    : 0
}))

const barOption = computed(() => ({
  grid: { left: 8, right: 16, top: 24, bottom: 4, containLabel: true },
  tooltip: { trigger: 'axis' },
  legend: { data: ['订单量', '完成率(%)'], right: 8, top: 0, textStyle: { color: '#6b7280', fontSize: 11 } },
  xAxis: {
    type: 'category',
    data: rows.value.map((r) => r.name),
    axisLabel: { color: '#6b7280', fontSize: 11 }
  },
  yAxis: [
    { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#9ca3af', fontSize: 11 } },
    { type: 'value', max: 100, splitLine: { show: false }, axisLabel: { color: '#9ca3af', fontSize: 11 } }
  ],
  series: [
    { name: '订单量', type: 'bar', barWidth: 26, data: rows.value.map((r) => r.orders), itemStyle: { color: '#4f46e5', borderRadius: [4, 4, 0, 0] } },
    { name: '完成率(%)', type: 'line', yAxisIndex: 1, smooth: true, data: rows.value.map((r) => r.doneRate), itemStyle: { color: '#f59e0b' }, lineStyle: { width: 2.5 } }
  ]
}))

const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { color: '#6b7280', fontSize: 11 } },
  series: [
    {
      type: 'pie',
      radius: ['46%', '68%'],
      center: ['50%', '46%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { fontSize: 11, color: '#374151' },
      data: rows.value.map((r, i) => ({
        name: r.name,
        value: r.amount,
        itemStyle: { color: ['#4f46e5', '#6366f1', '#a5b4fc', '#f59e0b', '#10b981'][i % 5] }
      }))
    }
  ]
}))

onMounted(async () => {
  const r = await loadResource(() => salesReport(), demoSales)
  const data = r.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || demoSales
  live.value = r.live
  loading.value = false
})

// ---- 销售业绩下钻（US-2.2）：点击柱/饼图某销售，查看其底层订单明细
const detail = ref([])
const detailLoading = ref(false)
const detailVisible = ref(false)
const detailTitle = ref('')
const STATUS_LABEL = { PENDING: '待受理', PAID: '已支付', INSTALLING: '安装中', DONE: '已完成', CANCELLED: '已取消', REFUND: '已退款' }
const detailColumns = [
  { prop: 'orderNo', label: '订单号', minWidth: 150 },
  { prop: 'customerName', label: '客户', width: 110 },
  { prop: 'packageName', label: '套餐', minWidth: 130 },
  { prop: 'amount', label: '金额', width: 110, format: (r) => money(r.amount) },
  { prop: 'status', label: '状态', width: 100, format: (r) => STATUS_LABEL[r.status] || r.status },
  { prop: 'orderType', label: '类型', width: 120 },
  { prop: 'createdDate', label: '日期', width: 120 }
]

async function onSalesChartClick(params) {
  if (!params || !params.name) return
  const name = params.name
  detailTitle.value = `销售业绩明细 · ${name}`
  detailLoading.value = true
  detailVisible.value = true
  const r = await loadResource(() => salesReportDetail({ salesName: name }), () => demoSalesDetail(name))
  detail.value = Array.isArray(r.data) ? r.data : (r.data && r.data.rows) || []
  detailLoading.value = false
}
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">销售管理</h2>
        <p class="page-sub">销售业绩、订单量与该销售订单的完成率（按 sales_name 聚合 biz_order 表）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat"><div class="label">销售人数</div><div class="value">{{ rows.length }}<small> 人</small></div></div>
      <div class="stat"><div class="label">合计订单</div><div class="value">{{ total.orders }}<small> 单</small></div></div>
      <div class="stat is-up"><div class="label">合计业绩</div><div class="value">{{ money(total.amount) }}</div></div>
      <div class="stat is-down"><div class="label">平均完成率</div><div class="value">{{ total.doneRate }}<small> %</small></div></div>
    </div>

    <div class="cols">
      <div class="card">
        <div class="card-head"><h3>业绩对比<span class="hint">点击柱/饼下钻明细</span></h3><SourceTag :live="live" /></div>
        <div class="card-body"><ChartBox :option="barOption" height="260px" @chart-click="onSalesChartClick" /></div>
      </div>
      <div class="card">
        <div class="card-head"><h3>业绩占比<span class="hint">点击饼块下钻明细</span></h3><SourceTag :live="live" /></div>
        <div class="card-body"><ChartBox :option="pieOption" height="260px" @chart-click="onSalesChartClick" /></div>
      </div>
    </div>

    <div class="card">
      <div class="card-head"><h3>销售业绩明细</h3></div>
      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="name" label="销售" width="120" />
        <el-table-column prop="region" label="负责片区" width="140" />
        <el-table-column prop="month" label="统计月份" width="120" />
        <el-table-column prop="orders" label="订单量" width="100" />
        <el-table-column label="业绩金额" width="140">
          <template #default="{ row }">
            <b class="up">{{ money(row.amount) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="完成率（已完成 / 总单）" min-width="220">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.doneRate) || 0" :stroke-width="10" color="#4f46e5" />
          </template>
        </el-table-column>
        <template #empty><EmptyState icon="💼" title="暂无销售数据" desc="暂无销售业绩记录" /></template>
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

.cols {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
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

@media (max-width: 1100px) {
  .cols {
    grid-template-columns: 1fr;
  }
}
</style>
