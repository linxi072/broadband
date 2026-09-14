<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { adminCommunityList, checkCommunity, submitDemand } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoCommunities } from '@/mock/fallback'

const router = useRouter()
const rows = ref([])
const live = ref(false)
const loading = ref(true)
const keyword = ref('')
const status = ref('')

const result = ref(null)
const checking = ref(false)

const STATUS = [
  { value: 'AVAILABLE', label: '可安装', type: 'success' },
  { value: 'TIGHT', label: '紧张', type: 'warning' },
  { value: 'UNAVAILABLE', label: '不可安装', type: 'info' }
]

const filtered = computed(() =>
  rows.value.filter((r) => {
    const kw = keyword.value.trim()
    const okKw = !kw || String(r.name).includes(kw) || String(r.region || '').includes(kw)
    const okStatus = !status.value || r.status === status.value
    return okKw && okStatus
  })
)

const statusMeta = (s) => STATUS.find((x) => x.value === s) || { label: s || '—', type: 'info' }

const remain = (r) => Math.max(0, Number(r.portTotal || 0) - Number(r.portUsed || 0))

async function onCheck(row) {
  checking.value = true
  result.value = null
  const r = await loadResource(() => checkCommunity(row.name), null)
  if (r.live) {
    result.value = { ...r.data, live: true }
  } else {
    result.value = {
      name: row.name,
      portStatus: row.status,
      portRemaining: remain(row),
      message: '后端不可达，展示本地推演结果',
      canProceed: row.status === 'AVAILABLE',
      packages: [],
      live: false
    }
  }
  checking.value = false
}

async function onDemand(row) {
  const r = await loadResource(
    () => submitDemand({ name: row.name, contact: '运营后台', phone: '13800000000', note: '后台标记覆盖需求' }),
    null
  )
  if (r.live) ElMessage.success(`已登记「${row.name}」覆盖需求`)
  else ElMessage.warning('登记接口不可达')
}

onMounted(async () => {
  const r = await loadResource(() => adminCommunityList({ size: 200 }), demoCommunities)
  const data = r.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || demoCommunities
  live.value = r.live
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">小区覆盖管理</h2>
        <p class="page-sub">覆盖状态、端口余量与可办套餐（与小程序「可安装小区查询」同源）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat is-down">
        <div class="label">可安装</div>
        <div class="value">{{ rows.filter((r) => r.status === 'AVAILABLE').length }}<small> 个</small></div>
      </div>
      <div class="stat is-warn">
        <div class="label">端口紧张</div>
        <div class="value">{{ rows.filter((r) => r.status === 'TIGHT').length }}<small> 个</small></div>
      </div>
      <div class="stat">
        <div class="label">未覆盖</div>
        <div class="value">{{ rows.filter((r) => r.status === 'UNAVAILABLE').length }}<small> 个</small></div>
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="小区名 / 区域" clearable style="width: 220px" />
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="s in STATUS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <div class="spacer"></div>
        <el-button type="primary" @click="router.push('/community/edit')">新增小区</el-button>
      </div>

      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="name" label="小区名称" min-width="130" />
        <el-table-column prop="region" label="区域" min-width="140" />
        <el-table-column prop="street" label="街道" width="120" />
        <el-table-column label="经纬度" width="150">
          <template #default="{ row }">
            <span class="mono">{{ row.lat }}, {{ row.lng }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="carrier" label="覆盖运营商" width="110" />
        <el-table-column label="端口余量" width="110">
          <template #default="{ row }">
            <span v-if="row.portTotal">{{ remain(row) }} / {{ row.portTotal }}</span>
            <span v-else class="mute">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small" effect="light">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" :loading="checking" @click="onCheck(row)">
              校验
            </el-button>
            <el-button link type="primary" size="small" @click="router.push(`/community/edit/${row.id}`)">
              编辑
            </el-button>
          </template>
        </el-table-column>
        <template #empty>暂无小区</template>
      </el-table>
    </div>

    <el-dialog v-if="result" :model-value="true" title="可安装性校验结果" width="460px" @close="result = null">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="小区">{{ result.name }}</el-descriptions-item>
        <el-descriptions-item label="端口状态">
          <el-tag size="small" effect="light" :type="statusMeta(result.portStatus).type">
            {{ statusMeta(result.portStatus).label }}
          </el-tag>
          <span style="margin-left: 8px">余量 {{ result.portRemaining }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="结论">{{ result.message }}</el-descriptions-item>
        <el-descriptions-item label="可否下单">{{ result.canProceed ? '可以' : '拦截' }}</el-descriptions-item>
        <el-descriptions-item label="可办套餐">
          <template v-if="(result.packages || []).length">
            <el-tag v-for="p in result.packages" :key="p.id" size="small" style="margin-right: 4px">
              {{ p.name }}
            </el-tag>
          </template>
          <span v-else class="mute">—</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <SourceTag :live="result.live" />
        <el-button v-if="!result.canProceed" type="primary" @click="onDemand({ name: result.name }); result = null">
          登记覆盖需求
        </el-button>
        <el-button @click="result = null">关闭</el-button>
      </template>
    </el-dialog>
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

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}

.mute {
  color: var(--bd-text-mute);
}
</style>
