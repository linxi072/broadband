<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { orderList, runDispatch, checkCommunity } from '@/api/business'
import { loadResource, money } from '@/composables/useResource'
import { demoOrders } from '@/mock/fallback'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const router = useRouter()
const rows = ref([])
const live = ref(false)
const loading = ref(true)
const keyword = ref('')
const status = ref('')

const STATUS = ['待受理', '已支付', '安装中', '已完成', '已取消']
const STATUS_KEYS = { '待受理': 'pending', '已支付': 'paid', '安装中': 'installing', '已完成': 'done', '已取消': 'cancelled' }

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

async function onDispatch(row) {
  try {
    await runDispatch()
    ElMessage.success(t('order.msgDispatched', { id: row.id }))
    router.push('/workorder/dispatch')
  } catch (e) {
    router.push('/workorder/dispatch')
  }
}

async function onCheck(row) {
  if (!row.community) return ElMessage.warning(t('order.warnNoCommunity'))
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
        <h2 class="page-title">{{ t('order.title') }}</h2>
        <p class="page-sub">{{ t('order.sub') }}</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat"><div class="label">{{ t('order.statTotal') }}</div><div class="value">{{ summary.total }}<small> 单</small></div></div>
      <div class="stat is-up"><div class="label">{{ t('order.statAmount') }}</div><div class="value">{{ money(summary.amount) }}</div></div>
      <div class="stat is-warn"><div class="label">{{ t('order.statPending') }}</div><div class="value">{{ summary.pending }}<small> 单</small></div></div>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" :placeholder="t('order.placeholderKeyword')" clearable style="width: 240px" />
        <el-select v-model="status" :placeholder="t('order.placeholderStatus')" clearable style="width: 140px">
          <el-option v-for="s in STATUS" :key="s" :label="t('order.status.' + STATUS_KEYS[s])" :value="s" />
        </el-select>
        <div class="spacer"></div>
        <el-button type="primary" @click="runDispatch()">{{ t('order.dispatchBtn') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="id" :label="t('order.colOrderId')" min-width="160" />
        <el-table-column prop="customer" :label="t('order.colCustomer')" width="90" />
        <el-table-column prop="phone" :label="t('order.colPhone')" width="130" />
        <el-table-column prop="pkgName" :label="t('order.colPackage')" min-width="120" />
        <el-table-column prop="community" :label="t('order.colCommunity')" min-width="110">
          <template #default="{ row }">
            <el-link v-if="row.community" type="primary" :underline="false" @click="onCheck(row)">
              {{ row.community }}
            </el-link>
            <span v-else class="mute">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销售" width="80" />
        <el-table-column :label="t('order.colAmount')" width="100">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column :label="t('order.colStatus')" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="time" :label="t('order.colTime')" width="150" />
        <el-table-column :label="t('order.colAction')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onDispatch(row)">{{ t('order.dispatch') }}</el-button>
            <el-button link type="primary" size="small" @click="onCheck(row)">{{ t('order.checkCoverage') }}</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState icon="📋" :title="t('order.emptyTitle')" :desc="t('order.emptyDesc')" /></template>
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
