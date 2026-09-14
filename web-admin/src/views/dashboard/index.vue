<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import SourceTag from '@/components/SourceTag.vue'
import { dashboardStats, dashboardRecentOrders, slaBoard, slaCompensations } from '@/api/business'
import { loadResource, money, fmtTime } from '@/composables/useResource'
import { demoDashboard, demoOrders } from '@/mock/fallback'

const router = useRouter()

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
      name: '订单量',
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
      data: ['SLA 达标率', '履约率', '平均响应(分)'],
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

const statusType = (s) => {
  if (['已支付', '已完成', 'PAID', 'DONE'].includes(s)) return 'success'
  if (['安装中', 'ASSIGNED', '处理中'].includes(s)) return 'primary'
  if (['待受理', 'PENDING'].includes(s)) return 'warning'
  return 'info'
}

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
        <h2 class="page-title">数据看板</h2>
        <p class="page-sub">运营总览：订单、营收、装维时效与服务承诺履约情况</p>
      </div>
      <SourceTag :live="statsLive" />
    </div>

    <div class="stat-grid">
      <StatCard label="本月订单" :value="stats.monthOrders" unit=" 单" foot="含新装 / 移机 / 续费" />
      <StatCard label="本月营收" :value="money(stats.revenue)" tone="up" foot="已支付口径" />
      <StatCard label="待安装工单" :value="stats.pendingInstall" unit=" 单" tone="warn" foot="等待派单或上门" />
      <StatCard label="履约率" :value="stats.fulfillmentRate" unit="%" tone="down" foot="按承诺时限完成占比" />
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-head">
          <h3>订单趋势 <span class="hint">近 7 日</span></h3>
          <SourceTag :live="statsLive" />
        </div>
        <div class="card-body"><ChartBox :option="trendOption" height="240px" /></div>
      </div>

      <div class="card">
        <div class="card-head">
          <h3>服务履约指标</h3>
          <SourceTag :live="boardLive || statsLive" />
        </div>
        <div class="card-body"><ChartBox :option="slaOption" height="240px" /></div>
      </div>
    </div>

    <div class="grid-3">
      <StatCard
        label="SLA 达标率"
        :value="board ? board.slaRate : stats.slaRate"
        unit="%"
        tone="down"
        foot="承诺时限内完成"
      />
      <StatCard label="慢必赔单" :value="board ? board.slowPayCount : stats.slowPay" unit=" 单" tone="warn" foot="超时触发赔付" />
      <StatCard label="本月赔付" :value="money(board ? board.monthCompAmount : stats.monthComp)" tone="up" foot="流量券 + 话费" />
    </div>

    <div class="card">
      <div class="card-head">
        <h3>最近订单</h3>
        <div class="links">
          <SourceTag :live="ordersLive" />
          <el-link type="primary" :underline="false" @click="router.push('/order')">全部订单 →</el-link>
        </div>
      </div>
      <el-table :data="orders" style="width: 100%" size="default">
        <el-table-column prop="id" label="订单号" min-width="150" />
        <el-table-column prop="customer" label="客户" width="90" />
        <el-table-column prop="pkgName" label="套餐" min-width="120" />
        <el-table-column prop="community" label="小区" min-width="110" />
        <el-table-column prop="sales" label="销售" width="80" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="150">
          <template #default="{ row }">{{ row.time || fmtTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty>暂无订单数据</template>
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
