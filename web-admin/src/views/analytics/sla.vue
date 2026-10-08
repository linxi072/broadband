<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import { analyticsSlaHeatmap, analyticsPayoutTrend } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoAnalyticsSlaHeatmap, demoAnalyticsPayoutTrend } from '@/mock/fallback'

const range = ref(30)
const heat = ref(null)
const payout = ref(null)
const loading = ref(true)

const byCommunity = computed(() => heat.value?.byCommunity || [])
const heatTrend = computed(() => heat.value?.trend || [])
const payoutTrend = computed(() => payout.value?.trend || [])

const heatBar = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 90, right: 30, top: 20, bottom: 24 },
  xAxis: { type: 'value', name: '超时单' },
  yAxis: { type: 'category', data: byCommunity.value.map((c) => c.communityName).reverse() },
  series: [{
    type: 'bar',
    data: byCommunity.value.map((c) => ({ value: c.overtimeCount, itemStyle: { color: '#ef4444', borderRadius: [0, 4, 4, 0] } })).reverse(),
    label: { show: true, position: 'right' }
  }]
}))

const payoutLine = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 64, right: 30, top: 32, bottom: 30 },
  legend: { data: ['赔付金额', '赔付笔数'] },
  xAxis: { type: 'category', data: payoutTrend.value.map((t) => t.weekStart), axisLabel: { rotate: 30 } },
  yAxis: [
    { type: 'value', name: '金额(¥)' },
    { type: 'value', name: '笔数' }
  ],
  series: [
    { name: '赔付金额', type: 'line', smooth: true, data: payoutTrend.value.map((t) => t.amount), itemStyle: { color: '#f59e0b' } },
    { name: '赔付笔数', type: 'bar', yAxisIndex: 1, data: payoutTrend.value.map((t) => t.count), itemStyle: { color: '#06b6d4' } }
  ]
}))

async function load() {
  loading.value = true
  const [h, p] = await Promise.all([
    loadResource(() => analyticsSlaHeatmap(range.value), () => demoAnalyticsSlaHeatmap(range.value)),
    loadResource(() => analyticsPayoutTrend(range.value), () => demoAnalyticsPayoutTrend(range.value))
  ])
  heat.value = h.data
  payout.value = p.data
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">SLA 超时与赔付 · 运维质量</h2>
        <p class="page-sub">复用 SLA 子系统（sla_record / compensation），按小区 × 周聚合超时与赔付（T-04 数据分析深化）</p>
      </div>
      <SourceTag :live="!!(heat && payout)" />
    </div>

    <div class="mini-stats">
      <StatCard label="超时工单" :value="heat?.totalOvertime || 0" unit="单" tone="down" foot="统计窗口内" />
      <StatCard label="赔付总额" :value="'¥' + (payout?.totalAmount || 0).toLocaleString()" foot="累计赔付" />
      <StatCard label="赔付笔数" :value="payout?.totalCount || 0" unit="笔" tone="down" />
      <StatCard label="涉及小区" :value="byCommunity.length" unit="个" />
    </div>

    <div class="card">
      <div class="card-head"><h3>一、各小区超时工单分布（按小区）</h3></div>
      <ChartBox :option="heatBar" height="300px" />
      <el-table :data="byCommunity" size="small" style="width: 100%; margin-top: 12px">
        <el-table-column prop="communityName" label="小区" min-width="160" />
        <el-table-column prop="communityId" label="小区ID" min-width="120" />
        <el-table-column label="超时单" width="120" align="right">
          <template #default="{ row }"><b>{{ row.overtimeCount }}</b></template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <div class="card-head"><h3>二、超时趋势（按周）</h3></div>
      <el-table :data="heatTrend" size="small" style="width: 100%">
        <el-table-column prop="weekStart" label="周起始" min-width="140" />
        <el-table-column label="超时单" width="120" align="right">
          <template #default="{ row }"><b>{{ row.overtimeCount }}</b></template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <div class="card-head"><h3>三、赔付趋势（金额 / 笔数）</h3></div>
      <ChartBox :option="payoutLine" height="320px" />
      <el-table :data="payoutTrend" size="small" style="width: 100%; margin-top: 12px">
        <el-table-column prop="weekStart" label="月份" min-width="140" />
        <el-table-column label="赔付金额" width="140" align="right">
          <template #default="{ row }"><b>¥{{ row.amount.toLocaleString() }}</b></template>
        </el-table-column>
        <el-table-column label="赔付笔数" width="120" align="right">
          <template #default="{ row }">{{ row.count }}</template>
        </el-table-column>
      </el-table>
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
.card-head { margin-bottom: 12px; }
</style>
