<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import { analyticsFunnel } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoAnalyticsFunnel } from '@/mock/fallback'

const data = ref(null)
const live = ref(false)
const loading = ref(true)

const stages = computed(() => data.value?.stages || [])
const registered = computed(() => data.value?.registered || 0)

const funnelOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 90, right: 40, top: 20, bottom: 24 },
  xAxis: { type: 'value', name: '客户数' },
  yAxis: { type: 'category', data: stages.value.map((s) => s.stage).reverse() },
  series: [{
    type: 'bar',
    data: stages.value.map((s) => ({ value: s.count, itemStyle: { color: '#4f46e5', borderRadius: [0, 4, 4, 0] } })).reverse(),
    label: { show: true, position: 'right', formatter: (p) => `${p.value} 人` }
  }]
}))

async function load() {
  const r = await loadResource(() => analyticsFunnel(), () => demoAnalyticsFunnel())
  data.value = r.data
  live.value = r.live
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">营销漏斗 · 客户转化全链路</h2>
        <p class="page-sub">注册 → 活跃 → 下单 → 支付 → 复购，五阶段真实转化（T-04 数据分析深化）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="mini-stats">
      <StatCard label="注册客户" :value="registered" unit="人" />
      <StatCard label="创建订单" :value="stages.length ? stages[2]?.count : 0" unit="人" tone="down" />
      <StatCard label="支付成功" :value="stages.length ? stages[3]?.count : 0" unit="人" tone="down" />
      <StatCard label="下单转化率" :value="registered ? Math.round(((stages[2]?.count || 0) / registered) * 100) : 0" unit="%" />
    </div>

    <div class="card">
      <div class="card-head"><h3>转化漏斗（按阶段客户数）</h3></div>
      <ChartBox :option="funnelOption" height="320px" />
    </div>

    <div class="card">
      <div class="card-head"><h3>阶段明细 & 转化率</h3></div>
      <el-table v-loading="loading" :data="stages" size="small" style="width: 100%">
        <el-table-column prop="stage" label="阶段" min-width="140" />
        <el-table-column label="客户数" width="120" align="right">
          <template #default="{ row }"><b>{{ row.count }}</b></template>
        </el-table-column>
        <el-table-column label="相对注册转化率" min-width="220">
          <template #default="{ row }">
            <el-progress :percentage="registered ? Math.round((row.count / registered) * 100) : 0" :stroke-width="14" />
          </template>
        </el-table-column>
        <el-table-column label="阶段转化" width="120" align="right">
          <template #default="{ row }">{{ row.conversion }}%</template>
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
