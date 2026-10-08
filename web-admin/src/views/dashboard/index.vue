<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import SourceTag from '@/components/SourceTag.vue'
import { dashboardStats, dashboardRecentOrders, slaBoard, slaCompensations } from '@/api/business'
import { loadResource, money, fmtTime } from '@/composables/useResource'
import { demoDashboard, demoOrders } from '@/mock/fallback'

const router = useRouter()
const { t } = useI18n()

const stats = ref({ ...demoDashboard })
const statsLive = ref(false)
const board = ref(null)
const boardLive = ref(false)
const orders = ref([])
const ordersLive = ref(false)
const loading = ref(true)

const trendOption = computed(() => ({
  grid: { left: 8, right: 12, top: 24, bottom: 4, containLabel: true },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: stats.value.orderTrendLabels,
    axisLine: { lineStyle: { color: '#e5e7eb' } },
    axisLabel: { color: '#6b7280', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f1f5f9' } },
    axisLabel: { color: '#9ca3af', fontSize: 11 }
  },
  series: [
    {
      name: t('dashboard.trendName'),
      type: 'line',
      smooth: true,
      symbolSize: 6,
      data: stats.value.orderTrend,
      itemStyle: { color: '#4f46e5' },
      lineStyle: { width: 2.5 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(79,70,229,0.26)' },
            { offset: 1, color: 'rgba(79,70,229,0.02)' }
          ]
        }
      }
    }
  ]
}))

const slaOption = computed(() => {
  const d = stats.value
  return {
    grid: { left: 8, right: 12, top: 20, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: [t('dashboard.slaAxisRate'), t('dashboard.slaAxisFulfill'), t('dashboard.slaAxisAvgResp')],
      axisLabel: { color: '#6b7280', fontSize: 11 }
    },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#9ca3af', fontSize: 11 } },
    series: [
      {
        type: 'bar',
        barWidth: 34,
        data: [
          { value: d.slaRate, itemStyle: { color: '#4f46e5' } },
          { value: d.fulfillmentRate, itemStyle: { color: '#10b981' } },
          { value: d.avgResponse, itemStyle: { color: '#f59e0b' } }
        ],
        label: { show: true, position: 'top', fontSize: 11, color: '#374151' }
      }
    ]
  }
})

onMounted(async () => {
  loading.value = true

  const [s, b, o] = await Promise.all([
    loadResource(() => dashboardStats(), demoDashboard),
    loadResource(() => slaBoard(), null),
    loadResource(() => dashboardRecentOrders(6), () => demoOrders.slice(0, 4))
  ])

  if (s.data) stats.value = { ...demoDashboard, ...s.data }
  statsLive.value = s.live

  if (b.data) {
    board.value = b.data
    boardLive.value = true
    if (b.data.recentCompensations && b.data.recentCompensations.length) {
      // 看板接口已提供赔付明细，可复用为「近期履约异常」
    }
  }

  const list = Array.isArray(o.data) ? o.data : (o.data && o.data.records) || []
  orders.value = list.length ? list : demoOrders.slice(0, 4)
  ordersLive.value = o.live

  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">{{ t('dashboard.title') }}</h2>
        <p class="page-sub">{{ t('dashboard.sub') }}</p>
      </div>
      <SourceTag :live="statsLive" />
    </div>

    <template v-if="!loading">
    <div class="stat-grid">
      <StatCard :label="t('dashboard.monthOrders')" :value="stats.monthOrders" unit=" 单" :foot="t('dashboard.monthOrdersFoot')" />
      <StatCard :label="t('dashboard.revenue')" :value="money(stats.revenue)" tone="up" :foot="t('dashboard.revenueFoot')" />
      <StatCard :label="t('dashboard.pendingInstall')" :value="stats.pendingInstall" unit=" 单" tone="warn" :foot="t('dashboard.pendingInstallFoot')" />
      <StatCard :label="t('dashboard.fulfillmentRate')" :value="stats.fulfillmentRate" unit="%" tone="down" :foot="t('dashboard.fulfillmentRateFoot')" />
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-head">
          <h3>{{ t('dashboard.orderTrend') }} <span class="hint">{{ t('dashboard.orderTrendHint') }}</span></h3>
          <SourceTag :live="statsLive" />
        </div>
        <div class="card-body"><ChartBox :option="trendOption" height="240px" /></div>
      </div>

      <div class="card">
        <div class="card-head">
          <h3>{{ t('dashboard.slaMetrics') }}</h3>
          <SourceTag :live="boardLive || statsLive" />
        </div>
        <div class="card-body"><ChartBox :option="slaOption" height="240px" /></div>
      </div>
    </div>

    <div class="grid-3">
      <StatCard
        :label="t('dashboard.slaRate')"
        :value="board ? board.slaRate : stats.slaRate"
        unit="%"
        tone="down"
        :foot="t('dashboard.slaRateFoot')"
      />
      <StatCard :label="t('dashboard.slowPay')" :value="board ? board.slowPayCount : stats.slowPay" unit=" 单" tone="warn" :foot="t('dashboard.slowPayFoot')" />
      <StatCard :label="t('dashboard.monthComp')" :value="money(board ? board.monthCompAmount : stats.monthComp)" tone="up" :foot="t('dashboard.monthCompFoot')" />
    </div>
    </template>

    <template v-else>
      <div class="stat-grid">
        <div v-for="i in 4" :key="'sk-s' + i" class="stat sk-pulse"><div class="label">占位</div><div class="value">占位</div></div>
      </div>
      <div class="grid-2">
        <div class="card sk-pulse" style="height: 320px"></div>
        <div class="card sk-pulse" style="height: 320px"></div>
      </div>
      <div class="grid-3">
        <div v-for="i in 3" :key="'sk-g' + i" class="stat sk-pulse"><div class="label">占位</div><div class="value">占位</div></div>
      </div>
    </template>

    <div class="card">
      <div class="card-head">
        <h3>{{ t('dashboard.recentOrders') }}</h3>
        <div class="links">
          <SourceTag :live="ordersLive" />
          <el-link type="primary" :underline="false" @click="router.push('/order')">{{ t('dashboard.allOrders') }} →</el-link>
        </div>
      </div>
      <el-table v-loading="loading" :data="orders" style="width: 100%" size="default">
        <el-table-column prop="id" :label="t('dashboard.colOrderId')" min-width="150" />
        <el-table-column prop="customer" :label="t('dashboard.colCustomer')" width="90" />
        <el-table-column prop="pkgName" :label="t('dashboard.colPackage')" min-width="120" />
        <el-table-column prop="community" :label="t('dashboard.colCommunity')" min-width="110" />
        <el-table-column prop="sales" :label="t('dashboard.colSales')" width="80" />
        <el-table-column :label="t('dashboard.colAmount')" width="100">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column :label="t('dashboard.colStatus')" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column :label="t('dashboard.colTime')" width="150">
          <template #default="{ row }">{{ row.time || fmtTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty><EmptyState icon="📋" :title="t('dashboard.emptyTitle')" :desc="t('dashboard.emptyDesc')" /></template>
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

.grid-2 {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 16px;
}

.card + .card,
.grid-2 + .card,
.grid-3 + .card {
  margin-top: 16px;
}

.links {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 1100px) {
  .grid-2,
  .grid-3 {
    grid-template-columns: 1fr;
  }
}
</style>
