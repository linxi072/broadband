<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import { reviewList, closeReview } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoReviews } from '@/mock/fallback'

const rows = ref([])
const live = ref(false)
const loading = ref(true)
const type = ref('')
const status = ref('')

const TYPES = ['评价', '投诉']
const STATUSES = ['待处理', '处理中', '已回访', '已闭环']

const filtered = computed(() =>
  rows.value.filter((r) => (!type.value || r.type === type.value) && (!status.value || r.status === status.value))
)

const stats = computed(() => {
  const list = rows.value
  const scored = list.filter((r) => Number(r.score) > 0)
  return {
    total: list.length,
    avg: scored.length
      ? (scored.reduce((s, r) => s + Number(r.score), 0) / scored.length).toFixed(1)
      : '—',
    complaints: list.filter((r) => r.type === '投诉').length,
    open: list.filter((r) => r.status === '待处理' || r.status === '处理中').length
  }
})

async function onClose(row) {
  const r = await loadResource(() => closeReview(row.id), null)
  if (r.live) {
    ElMessage.success('已闭环')
    load()
  } else {
    row.status = '已闭环'
    ElMessage.warning('闭环接口不可达：仅本地标记')
  }
}

async function load() {
  loading.value = true
  const r = await loadResource(() => reviewList({ size: 200 }), demoReviews)
  const data = r.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || demoReviews
  live.value = r.live
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">投诉与评价管理</h2>
        <p class="page-sub">服务评分、评价标签与投诉闭环处理（数据源 review 表）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat"><div class="label">评价/投诉总量</div><div class="value">{{ stats.total }}<small> 条</small></div></div>
      <div class="stat is-up"><div class="label">服务均分</div><div class="value">{{ stats.avg }}<small> / 5</small></div></div>
      <div class="stat is-warn"><div class="label">投诉工单</div><div class="value">{{ stats.complaints }}<small> 条</small></div></div>
      <div class="stat is-warn"><div class="label">待处理</div><div class="value">{{ stats.open }}<small> 条</small></div></div>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="type" placeholder="全部类型" clearable style="width: 140px">
          <el-option v-for="t in TYPES" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="s in STATUSES" :key="s" :label="s" :value="s" />
        </el-select>
        <div class="spacer"></div>
        <el-button size="small" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="id" label="编号" width="100" />
        <el-table-column prop="orderId" label="关联工单" min-width="130" />
        <el-table-column prop="customer" label="客户" width="100" />
        <el-table-column prop="worker" label="师傅" width="100" />
        <el-table-column label="评分" width="150">
          <template #default="{ row }">
            <el-rate :model-value="Number(row.score) || 0" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === '投诉' ? 'danger' : 'success'" effect="light">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="170">
          <template #default="{ row }">
            <el-tag v-for="t in row.tags || []" :key="t" size="small" effect="plain" style="margin-right: 4px">
              {{ t }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="150" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== '已闭环'"
              link
              type="primary"
              size="small"
              @click="onClose(row)"
            >
              闭环
            </el-button>
            <span v-else class="mute">已完成</span>
          </template>
        </el-table-column>
        <template #empty><EmptyState icon="⭐" title="暂无评价/投诉数据" desc="客户评分与投诉将在此汇聚并支持闭环处理" /></template>
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
  font-size: 12px;
  color: var(--bd-text-mute);
}
</style>
