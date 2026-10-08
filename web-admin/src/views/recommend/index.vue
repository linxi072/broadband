<script setup>
import { onMounted, ref, computed } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import { adminRecommendShortcuts, adminRecommendTrend } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoRecommendShortcuts, demoRecommendTrend } from '@/mock/fallback'

const rows = ref([])
const total = ref(0)
const trend = ref([])
const live = ref(false)
const loading = ref(true)

const scope = ref('global')
const eventType = ref('')
const days = ref(30)

// 事件类型下拉（与埋点上报 eventType 对齐）
const EVENT_TYPES = [
  { value: '', label: '全部事件' },
  { value: 'PAGE_VIEW', label: '页面浏览' },
  { value: 'CLICK', label: '点击' },
  { value: 'ORDER', label: '下单' },
  { value: 'SEARCH', label: '搜索' },
  { value: 'PAY', label: '支付' },
  { value: 'SHARE', label: '分享' }
]

// 入口标题映射（埋点 entry → 展示名）
const ENTRY_TITLE = {
  package_upgrade: '套餐升级',
  community_check: '小区可装性校验',
  pay: '宽带续费/支付',
  support: '在线客服',
  traffic: '流量监控',
  points_mall: '积分商城',
  promotion: '优惠活动',
  account: '账户账单'
}

const summary = computed(() => {
  const totalHit = rows.value.reduce((a, r) => a + (r.hit || 0), 0)
  const totalUsers = rows.value.reduce((a, r) => a + (r.users || 0), 0)
  const top = rows.value[0]
  return { totalHit, totalUsers, topEntry: top ? top.entryTitle : '—' }
})

// 占比 = 该入口触达 / 全部入口触达
const ratioOf = (hit) => {
  const totalHit = rows.value.reduce((a, r) => a + (r.hit || 0), 0)
  if (!totalHit) return 0
  return ((hit / totalHit) * 100).toFixed(1)
}

// 趋势最大量（用于条形宽度归一）
const maxTrendCnt = computed(() =>
  trend.value.reduce((m, t) => Math.max(m, t.cnt || 0), 0) || 1
)

function buildParams() {
  const params = { scope: scope.value, days: days.value, limit: 10 }
  if (eventType.value) params.eventType = eventType.value
  return params
}

async function load() {
  loading.value = true
  const params = buildParams()
  const r = await loadResource(() => adminRecommendShortcuts(params), () => demoRecommendShortcuts(params))
  rows.value = r.data?.list || []
  total.value = r.data?.total || 0
  live.value = r.live
  loading.value = false
}

async function loadTrend() {
  const r = await loadResource(() => adminRecommendTrend({ days: days.value }), () => demoRecommendTrend({ days: days.value }))
  trend.value = r.data?.list || []
}

onMounted(async () => {
  await Promise.all([load(), loadTrend()])
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">智能推荐 · 推荐位分析</h2>
        <p class="page-sub">基于用户行为埋点，聚合高频入口 Top 榜，辅助首页快捷入口编排与精准营销（v1.18 新模块）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="mini-stats">
      <StatCard label="入口触达总量" :value="summary.totalHit" unit="次" />
      <StatCard label="去重用户数" :value="summary.totalUsers" unit="人" />
      <StatCard label="最高频入口" :value="summary.topEntry" />
      <StatCard label="观测窗口" :value="days + ' 天'" />
    </div>

    <div class="card">
      <div class="card-head">
        <div class="filters">
          <el-select v-model="scope" placeholder="统计范围" style="width: 140px" @change="load">
            <el-option label="全局（全部角色）" value="global" />
            <el-option label="仅 C 端客户" value="customer" />
          </el-select>
          <el-select v-model="eventType" placeholder="事件类型" clearable style="width: 140px" @change="load">
            <el-option v-for="t in EVENT_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-select v-model="days" placeholder="观测天数" style="width: 130px" @change="() => { load(); loadTrend() }">
            <el-option :value="7" label="近 7 天" />
            <el-option :value="30" label="近 30 天" />
            <el-option :value="90" label="近 90 天" />
          </el-select>
          <el-button type="primary" @click="() => { load(); loadTrend() }">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" size="small" style="width: 100%">
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ row }">
            <span class="rank" :class="'rank-' + row.rank">{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="entryTitle" label="入口 / 功能" min-width="160" />
        <el-table-column prop="entry" label="标识" min-width="150" show-overflow-tooltip />
        <el-table-column label="触达次数" width="110" align="right">
          <template #default="{ row }"><b>{{ row.hit }}</b></template>
        </el-table-column>
        <el-table-column label="去重用户" width="100" align="right">
          <template #default="{ row }">{{ row.users }}</template>
        </el-table-column>
        <el-table-column label="占比" width="160">
          <template #default="{ row }">
            <el-progress :percentage="Number(ratioOf(row.hit))" :stroke-width="10" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <div class="card-head">
        <h3 class="section-title">行为趋势（按天事件量）</h3>
      </div>
      <div v-if="trend.length" class="trend">
        <div v-for="t in trend" :key="t.day" class="trend-row">
          <span class="trend-day">{{ t.day }}</span>
          <div class="trend-bar-wrap">
            <div class="trend-bar" :style="{ width: (100 * (t.cnt || 0) / maxTrendCnt) + '%' }"></div>
          </div>
          <span class="trend-cnt">{{ t.cnt }} 次 / {{ t.users }} 人</span>
        </div>
      </div>
      <el-empty v-else description="暂无趋势数据" :image-size="60" />
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
.mini-stats > * { flex: 1; min-width: 150px; }
.card { margin-bottom: 16px; }
.card-head { margin-bottom: 12px; }
.filters { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.section-title { margin: 0; font-size: 15px; }
.rank {
  display: inline-block;
  width: 24px; height: 24px; line-height: 24px;
  border-radius: 50%;
  font-weight: 700;
  color: #fff;
  background: #c0c4cc;
}
.rank-1 { background: #f56c6c; }
.rank-2 { background: #e6a23c; }
.rank-3 { background: #409eff; }
.trend { display: flex; flex-direction: column; gap: 8px; }
.trend-row { display: flex; align-items: center; gap: 12px; font-size: 13px; }
.trend-day { width: 90px; color: var(--bd-text-mute); }
.trend-bar-wrap { flex: 1; background: #f0f2f5; border-radius: 4px; height: 14px; overflow: hidden; }
.trend-bar { height: 100%; background: linear-gradient(90deg, #409eff, #67c23a); border-radius: 4px; }
.trend-cnt { width: 130px; text-align: right; color: var(--bd-text-mute); }
</style>
