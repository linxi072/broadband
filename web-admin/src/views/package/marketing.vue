<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import { productMarketing } from '@/api/business'
import { loadResource, money } from '@/composables/useResource'
import { demoMarketingDashboard } from '@/mock/fallback'

const PALETTE = ['#4f46e5', '#22c55e', '#f59e0b', '#ef4444', '#06b6d4', '#a855f7']

const data = ref(null)
const live = ref(false)
const loading = ref(true)

const summary = computed(() => data.value?.summary || {})
const ranking = computed(() => data.value?.packageRanking || [])
const typeDist = computed(() => data.value?.orderTypeDist || [])
const levelDist = computed(() => data.value?.customerLevelDist || [])
const upgradeDist = computed(() => data.value?.upgradeByStatus || [])
const trend = computed(() => data.value?.revenueTrend || [])

// ---- 图表配置
const packagePie = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 元 ({d}%)' },
  legend: { bottom: 0, type: 'scroll' },
  color: PALETTE,
  series: [{
    type: 'pie',
    radius: ['42%', '68%'],
    center: ['50%', '46%'],
    avoidLabelOverlap: true,
    itemStyle: { borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: ranking.value.map((x) => ({ name: x.name, value: x.revenue }))
  }]
}))

const typePie = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
  legend: { bottom: 0, type: 'scroll' },
  color: PALETTE,
  series: [{
    type: 'pie',
    radius: ['42%', '68%'],
    center: ['50%', '46%'],
    itemStyle: { borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: typeDist.value.map((x) => ({ name: x.typeLabel, value: x.orders }))
  }]
}))

const levelPie = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
  legend: { bottom: 0, type: 'scroll' },
  color: PALETTE,
  series: [{
    type: 'pie',
    radius: ['42%', '68%'],
    center: ['50%', '46%'],
    itemStyle: { borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: levelDist.value.map((x) => ({ name: x.levelLabel, value: x.count }))
  }]
}))

const upgradePie = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
  legend: { bottom: 0, type: 'scroll' },
  color: PALETTE,
  series: [{
    type: 'pie',
    radius: ['42%', '68%'],
    center: ['50%', '46%'],
    itemStyle: { borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: upgradeDist.value.map((x) => ({ name: x.statusLabel, value: x.count }))
  }]
}))

const trendOption = computed(() => {
  const list = trend.value
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收(元)', '订单数'] },
    grid: { left: 54, right: 54, top: 30, bottom: 28 },
    xAxis: { type: 'category', data: list.map((x) => x.month) },
    yAxis: [
      { type: 'value', name: '元' },
      { type: 'value', name: '单' }
    ],
    series: [
      { name: '营收(元)', type: 'line', smooth: true, data: list.map((x) => x.revenue),
        itemStyle: { color: '#4f46e5' }, areaStyle: { opacity: 0.12 } },
      { name: '订单数', type: 'bar', yAxisIndex: 1, data: list.map((x) => x.orders),
        itemStyle: { color: '#22c55e', borderRadius: [4, 4, 0, 0] } }
    ]
  }
})

async function load() {
  const r = await loadResource(() => productMarketing(), () => demoMarketingDashboard())
  data.value = r.data
  live.value = r.live
}

onMounted(async () => {
  loading.value = true
  await load()
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">套餐营销看板</h2>
        <p class="page-sub">套餐销量、业务结构、升级转化与客户分层一站式洞察</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <StatCard label="累计订单" :value="summary.totalOrders ?? 0" foot="全量业务订单" />
      <StatCard label="累计营收" :value="money(summary.totalRevenue)" tone="up" foot="未取消订单金额" />
      <StatCard label="客单价" :value="money(summary.avgOrderAmount)" foot="未取消均值" />
      <StatCard label="客户总数" :value="summary.customerCount ?? 0" foot="在册客户" />
      <StatCard label="升级转化率" :value="summary.upgradeRate ?? 0" unit="%" tone="down" foot="已生效 / 总升级单" />
      <StatCard label="已生效升级" :value="summary.effectiveUpgrades ?? 0" tone="down" :foot="`升级单 ${summary.upgradeCount ?? 0} 笔`" />
    </div>

    <div class="charts">
      <div class="card chart-card">
        <div class="card-head"><h3>套餐营收占比</h3></div>
        <ChartBox :option="packagePie" height="300px" />
      </div>
      <div class="card chart-card">
        <div class="card-head"><h3>业务类型分布</h3></div>
        <ChartBox :option="typePie" height="300px" />
      </div>
      <div class="card chart-card">
        <div class="card-head"><h3>客户分层分布</h3></div>
        <ChartBox :option="levelPie" height="300px" />
      </div>
      <div class="card chart-card">
        <div class="card-head"><h3>升级单状态分布</h3></div>
        <ChartBox :option="upgradePie" height="300px" />
      </div>
      <div class="card chart-card span2">
        <div class="card-head"><h3>近 6 月营收趋势</h3></div>
        <ChartBox :option="trendOption" height="300px" />
      </div>
    </div>

    <div class="card">
      <div class="card-head">
        <h3>套餐销量排行</h3>
        <SourceTag :live="live" />
      </div>
      <el-table v-loading="loading" :data="ranking" style="width: 100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="套餐" min-width="200" />
        <el-table-column label="订单数" width="120" sortable :sort-by="(r) => r.orders">
          <template #default="{ row }">{{ row.orders }}</template>
        </el-table-column>
        <el-table-column label="营收(元)" width="140" sortable :sort-by="(r) => r.revenue">
          <template #default="{ row }"><b>{{ money(row.revenue) }}</b></template>
        </el-table-column>
        <el-table-column label="营收占比" width="200">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.ratio || 0)" :color="PALETTE[0]" :stroke-width="14" />
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无套餐销量数据" /></template>
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
}

.stat-grid {
  margin: 0 0 16px;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
}

.charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.span2 { grid-column: span 2; }

@media (max-width: 1100px) {
  .charts { grid-template-columns: 1fr; }
  .span2 { grid-column: span 1; }
}
</style>
