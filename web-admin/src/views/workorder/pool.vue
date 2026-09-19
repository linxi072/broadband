<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { workOrderList } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoWorkOrders } from '@/mock/fallback'

const router = useRouter()
const rows = ref([])
const live = ref(false)
const loading = ref(true)
const status = ref('')
const keyword = ref('')

const STATUS = [
  { value: 'PENDING', label: '待派单', type: 'warning' },
  { value: 'ASSIGNED', label: '已派单', type: 'primary' },
  { value: 'DONE', label: '已完成', type: 'success' },
  { value: 'EXCEPTION', label: '异常拦截', type: 'danger' }
]

const filtered = computed(() =>
  rows.value.filter((r) => {
    const kw = keyword.value.trim()
    const okKw =
      !kw || String(r.id).includes(kw) || String(r.community || '').includes(kw) || String(r.worker || '').includes(kw)
    return okKw && (!status.value || r.status === status.value)
  })
)

const meta = (s) => STATUS.find((x) => x.value === s) || { label: s || '—', type: 'info' }
const count = (s) => rows.value.filter((r) => r.status === s).length

async function load() {
  loading.value = true
  const r = await loadResource(() => workOrderList({ size: 200 }), demoWorkOrders)
  const data = r.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || demoWorkOrders
  live.value = r.live
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">工单池</h2>
        <p class="page-sub">新装 / 移机 / 报修工单的派单状态与师傅归属（数据源 work_order 表）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat is-warn"><div class="label">待派单</div><div class="value">{{ count('PENDING') }}<small> 单</small></div></div>
      <div class="stat"><div class="label">已派单</div><div class="value">{{ count('ASSIGNED') }}<small> 单</small></div></div>
      <div class="stat is-down"><div class="label">已完成</div><div class="value">{{ count('DONE') }}<small> 单</small></div></div>
      <div class="stat"><div class="label">异常拦截</div><div class="value">{{ count('EXCEPTION') }}<small> 单</small></div></div>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="工单号 / 小区 / 师傅" clearable style="width: 220px" />
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="s in STATUS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <div class="spacer"></div>
        <el-button type="primary" @click="router.push('/workorder/dispatch')">去派单</el-button>
      </div>

      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="id" label="工单号" min-width="120" />
        <el-table-column prop="customer" label="客户" width="100" />
        <el-table-column prop="pkgDesc" label="办理套餐" min-width="130" />
        <el-table-column prop="community" label="小区" min-width="120" />
        <el-table-column prop="address" label="门牌" width="110" />
        <el-table-column prop="timeSlot" label="预约时段" width="150" />
        <el-table-column label="相邻性" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.adjacent ? 'success' : 'info'" effect="plain">
              {{ row.adjacent ? '相邻' : '非相邻' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="worker" label="指派师傅" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="meta(row.status).type" size="small" effect="light">{{ meta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <template #empty><EmptyState icon="🔧" title="暂无工单" desc="可先执行一次派单，订单将自动进入工单池" /></template>
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
</style>
