<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { orderList, runDispatch, checkCommunity } from '@/api/business'
import { loadResource, money } from '@/composables/useResource'
import { demoOrders } from '@/mock/fallback'

const router = useRouter()
const rows = ref([])
const live = ref(false)
const loading = ref(true)
const keyword = ref('')
const status = ref('')

const STATUS = ['待受理', '已支付', '安装中', '已完成', '已取消']

const filtered = computed(() =>
  rows.value.filter((r) => {
    const kw = keyword.value.trim()
    const okKw =
      !kw ||
      String(r.id).includes(kw) ||
      String(r.customer || '').includes(kw) ||
      String(r.phone || '').includes(kw)
    const okStatus = !status.value || r.status === status.value
    return okKw && okStatus
  })
)

const summary = computed(() => {
  const total = filtered.value.length
  const amount = filtered.value.reduce((s, r) => s + Number(r.amount || 0), 0)
  const pending = filtered.value.filter((r) => r.status === '待受理').length
  return { total, amount, pending }
})

const statusType = (s) => {
  if (['已支付', '已完成'].includes(s)) return 'success'
  if (s === '安装中') return 'primary'
  if (s === '待受理') return 'warning'
  return 'info'
}

async function onDispatch(row) {
  try {
    await runDispatch()
    ElMessage.success(`已触发派单调度（订单 ${row.id} 所在批次）`)
    router.push('/workorder/dispatch')
  } catch (e) {
    router.push('/workorder/dispatch')
  }
}

async function onCheck(row) {
  if (!row.community) return ElMessage.warning('该订单未记录小区')
  try {
    const res = await checkCommunity(row.community)
    ElMessage.info(`${row.community}：${res.message || res.portStatus}`)
  } catch (e) {
    /* 已全局提示 */
  }
}

onMounted(async () => {
  const r = await loadResource(() => orderList({ size: 200 }), demoOrders)
  const data = r.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || demoOrders
  live.value = r.live
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">订单管理</h2>
        <p class="page-sub">下单受理、派单触发与订单履约跟踪</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat"><div class="label">订单总数</div><div class="value">{{ summary.total }}<small> 单</small></div></div>
      <div class="stat is-up"><div class="label">订单金额合计</div><div class="value">{{ money(summary.amount) }}</div></div>
      <div class="stat is-warn"><div class="label">待受理</div><div class="value">{{ summary.pending }}<small> 单</small></div></div>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="订单号 / 客户 / 手机号" clearable style="width: 240px" />
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="s in STATUS" :key="s" :label="s" :value="s" />
        </el-select>
        <div class="spacer"></div>
        <el-button type="primary" @click="runDispatch()">执行派单</el-button>
      </div>

      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="id" label="订单号" min-width="160" />
        <el-table-column prop="customer" label="客户" width="90" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="pkgName" label="套餐" min-width="120" />
        <el-table-column prop="community" label="小区" min-width="110">
          <template #default="{ row }">
            <el-link v-if="row.community" type="primary" :underline="false" @click="onCheck(row)">
              {{ row.community }}
            </el-link>
            <span v-else class="mute">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销售" width="80" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="150" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onDispatch(row)">派单</el-button>
            <el-button link type="primary" size="small" @click="onCheck(row)">查覆盖</el-button>
          </template>
        </el-table-column>
        <template #empty>暂无订单</template>
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
}

.mute {
  color: var(--bd-text-mute);
}
</style>
