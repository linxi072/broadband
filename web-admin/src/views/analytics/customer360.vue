<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import { analyticsCustomer360 } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoAnalyticsCustomer360 } from '@/mock/fallback'

const customerId = ref('c001')
const data = ref(null)
const live = ref(false)
const loading = ref(false)

const basic = computed(() => data.value?.basic || {})
const recentOrders = computed(() => data.value?.recentOrders || [])
const recentReviews = computed(() => data.value?.recentReviews || [])
const activeRefunds = computed(() => data.value?.activeRefunds || [])
const riskReasons = computed(() => data.value?.riskReasons || [])

const riskType = (s) => (s >= 85 ? 'danger' : s >= 60 ? 'warning' : 'success')
const ORDER_STATUS = { NEW_INSTALL: '新装', RENEW: '续约', UPGRADE: '升级', DONE: '已完成', PAID: '已支付', INSTALLING: '安装中' }

async function load() {
  if (!customerId.value) return ElMessage.warning('请输入客户ID')
  loading.value = true
  const r = await loadResource(
    () => analyticsCustomer360(customerId.value),
    () => demoAnalyticsCustomer360(customerId.value)
  )
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
        <h2 class="page-title">客户 360 · 生命周期全景</h2>
        <p class="page-sub">基于 customer / biz_order / points_account / review / order_refund 真实聚合（T-04 数据分析深化）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="toolbar">
      <el-input v-model="customerId" placeholder="客户ID，如 c001" style="width: 240px" @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">查询</el-button>
    </div>

    <div v-if="data" v-loading="loading">
      <div class="card">
        <div class="card-head"><h3>基础档案 & 风险</h3></div>
        <div class="profile">
          <div class="basic">
            <p>
              <b>{{ basic.name }}</b>
              <el-tag size="small" effect="plain">{{ basic.level }}</el-tag>
              <el-tag size="small" :type="basic.status === 'ACTIVE' ? 'success' : 'info'" effect="light">
                {{ basic.status === 'ACTIVE' ? '正常' : '异常' }}
              </el-tag>
            </p>
            <p class="muted">ID：{{ basic.id }} · 小区：{{ basic.communityId }} · 入网：{{ basic.createdTime ? new Date(basic.createdTime).toLocaleDateString('zh-CN') : '—' }}</p>
          </div>
          <div class="risk">
            <div class="risk-score">
              <span>流失风险分</span>
              <el-tag :type="riskType(data.riskScore)" size="large" effect="dark">{{ data.riskScore }}</el-tag>
            </div>
            <el-tag size="small" effect="plain">{{ data.segmentLabel }}</el-tag>
            <div class="reasons">
              <span v-for="(r, i) in riskReasons" :key="i" class="reason">{{ r }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="mini-stats">
        <StatCard label="订单数" :value="data.orderCount" unit="单" />
        <StatCard label="累计消费" :value="'¥' + data.totalSpent.toLocaleString()" />
        <StatCard label="积分余额" :value="data.pointsBalance" unit="分" tone="down" />
        <StatCard label="未结投诉" :value="data.complaintOpen" unit="件" />
        <StatCard label="距上次下单" :value="data.daysSinceLastOrder" unit="天" tone="down" />
      </div>

      <div class="card">
        <div class="card-head"><h3>近期订单</h3></div>
        <el-table :data="recentOrders" size="small" style="width: 100%">
          <el-table-column prop="id" label="订单号" min-width="160" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ ORDER_STATUS[row.orderType] || row.orderType }}</template>
          </el-table-column>
          <el-table-column prop="packageName" label="套餐" min-width="150" show-overflow-tooltip />
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ row }"><b>¥{{ row.amount }}</b></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="下单时间" min-width="150">
            <template #default="{ row }">{{ row.createdTime ? new Date(row.createdTime).toLocaleDateString('zh-CN') : '—' }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card">
        <div class="card-head"><h3>近期评价 & 在途退款</h3></div>
        <el-table :data="recentReviews" size="small" style="width: 100%">
          <el-table-column label="评分" width="120">
            <template #default="{ row }"><el-rate :model-value="row.score" disabled /></template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="240" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ row.status === 'CLOSED' ? '已闭环' : '处理中' }}</template>
          </el-table-column>
        </el-table>
        <p v-if="!activeRefunds.length" class="hint-line">当前无在途退款。</p>
      </div>
    </div>
    <el-empty v-else description="请输入客户ID查询" />
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
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.card { margin-bottom: 16px; }
.card-head { margin-bottom: 12px; }
.muted { font-size: 13px; color: var(--bd-text-mute); }
.profile { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; align-items: start; }
.risk-score { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.risk-score span { font-size: 13px; color: var(--bd-text-mute); }
.reasons { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; }
.reason { font-size: 12px; color: var(--bd-text-mute); }
.mini-stats { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.mini-stats > * { flex: 1; min-width: 150px; }
.hint-line { font-size: 13px; color: var(--bd-text-mute); margin-top: 8px; }
@media (max-width: 1000px) {
  .profile { grid-template-columns: 1fr; }
}
</style>
