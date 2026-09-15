<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import { runDispatch, getCapacity } from '@/api/business'
import { loadResource, today } from '@/composables/useResource'

const running = ref(false)
const plan = ref(null)
const planLive = ref(false)

const timeSlot = ref(`${today()}#AM`)
const capacity = ref([])
const capLive = ref(false)
const capLoading = ref(false)

const SLOTS = [
  { value: `${today()}#AM`, label: '今日 上午 (AM)' },
  { value: `${today()}#PM`, label: '今日 下午 (PM)' },
  { value: `${today()}#EV`, label: '今日 晚间 (EV)' }
]

const summary = computed(() => {
  const p = plan.value
  if (!p) return null
  return {
    assigned: (p.results || []).length,
    blocked: (p.exceptions || []).length,
    logs: (p.logs || []).length
  }
})

const capSummary = computed(() => {
  const list = capacity.value
  return {
    workers: new Set(list.map((c) => c.workerId)).size,
    full: list.filter((c) => c.full).length,
    adjLeft: list.reduce((s, c) => s + Math.max(0, c.adjCap - c.adjUsed), 0),
    nonLeft: list.reduce((s, c) => s + Math.max(0, c.nonCap - c.nonUsed), 0)
  }
})

async function onRun() {
  running.value = true
  const r = await loadResource(() => runDispatch(), null)
  running.value = false
  if (r.live) {
    plan.value = r.data
    planLive.value = true
    ElMessage.success(`派单完成：成功 ${(r.data.results || []).length} 单，拦截 ${(r.data.exceptions || []).length} 单`)
    loadCapacity()
  } else {
    planLive.value = false
    ElMessage.error('派单接口不可达（需后端 :8082 在线）')
  }
}

async function loadCapacity() {
  capLoading.value = true
  const r = await loadResource(() => getCapacity(timeSlot.value), [])
  capacity.value = Array.isArray(r.data) ? r.data : []
  capLive.value = r.live
  capLoading.value = false
}

onMounted(loadCapacity)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">派单调度</h2>
        <p class="page-sub">
          相邻小区聚类 → 时段容量校验 → 贪心派单 → 超容拦截（POST /api/dispatch/run）
        </p>
      </div>
      <el-button type="primary" :loading="running" @click="onRun">执行派单</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat is-down">
        <div class="label">本次成功派单</div>
        <div class="value">{{ summary ? summary.assigned : '—' }}<small> 单</small></div>
      </div>
      <div class="stat is-warn">
        <div class="label">超容拦截</div>
        <div class="value">{{ summary ? summary.blocked : '—' }}<small> 单</small></div>
      </div>
      <div class="stat">
        <div class="label">可接单剩余容量</div>
        <div class="value">{{ capSummary.adjLeft }}<small> 相邻 / {{ capSummary.nonLeft }} 非相邻</small></div>
      </div>
      <div class="stat">
        <div class="label">容量已满师傅</div>
        <div class="value">{{ capSummary.full }}<small> / {{ capSummary.workers }} 人</small></div>
      </div>
    </div>

    <div class="card">
      <div class="card-head">
        <h3>派单结果</h3>
        <SourceTag :live="planLive" label="待执行" />
      </div>

      <el-empty v-if="!plan" description="点击右上角「执行派单」触发算法" :image-size="80" />

      <template v-else>
        <div class="card-body">
          <h4 class="sub">成功派单（{{ (plan.results || []).length }}）</h4>
          <el-table :data="plan.results || []" size="small" max-height="260">
            <el-table-column prop="workOrderId" label="工单号" min-width="120" />
            <el-table-column prop="workerId" label="指派师傅" width="120" />
            <el-table-column prop="timeSlot" label="时段" width="150" />
            <el-table-column prop="clusterId" label="聚类簇" width="130" />
            <el-table-column label="路线" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.adjacentRoute ? 'success' : 'info'" effect="plain">
                  {{ row.adjacentRoute ? '相邻合并' : '非相邻独立' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="note" label="说明" min-width="160" show-overflow-tooltip />
            <template #empty><EmptyState compact icon="🔧" title="本次无成功派单" desc="运行派单后在此查看成功明细" /></template>
          </el-table>

          <h4 class="sub">超容拦截（{{ (plan.exceptions || []).length }}）</h4>
          <el-table :data="plan.exceptions || []" size="small" max-height="220">
            <el-table-column prop="workOrderId" label="工单号" min-width="120" />
            <el-table-column prop="timeSlot" label="时段" width="150" />
            <el-table-column prop="reason" label="拦截原因" min-width="280" show-overflow-tooltip />
            <template #empty><EmptyState compact icon="🚫" title="本次无拦截" desc="所有订单均通过可安装性校验" /></template>
          </el-table>

          <el-collapse v-if="(plan.logs || []).length" style="margin-top: 14px">
            <el-collapse-item :title="`算法过程日志（${plan.logs.length} 行）`">
              <pre class="logs">{{ (plan.logs || []).join('\n') }}</pre>
            </el-collapse-item>
          </el-collapse>
        </div>
      </template>
    </div>

    <div class="card">
      <div class="toolbar">
        <h3 style="margin: 0; font-size: 15px">时段容量看板</h3>
        <el-select v-model="timeSlot" style="width: 190px" @change="loadCapacity">
          <el-option v-for="s in SLOTS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <div class="spacer"></div>
        <el-button size="small" @click="loadCapacity">刷新</el-button>
        <SourceTag :live="capLive" />
      </div>

      <el-table v-loading="capLoading" :data="capacity" style="width: 100%">
        <el-table-column prop="workerName" label="师傅" width="120" />
        <el-table-column prop="workerId" label="师傅ID" width="100" />
        <el-table-column prop="timeSlot" label="时段" width="160" />
        <el-table-column label="相邻（已用 / 上限）" min-width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.min(100, Math.round((row.adjUsed / Math.max(1, row.adjCap)) * 100))"
              :stroke-width="10"
              :color="row.adjUsed >= row.adjCap ? '#dc2626' : '#4f46e5'"
            />
            <span class="mono">{{ row.adjUsed }} / {{ row.adjCap }}</span>
          </template>
        </el-table-column>
        <el-table-column label="非相邻（已用 / 上限）" min-width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.min(100, Math.round((row.nonUsed / Math.max(1, row.nonCap)) * 100))"
              :stroke-width="10"
              :color="row.nonUsed >= row.nonCap ? '#dc2626' : '#10b981'"
            />
            <span class="mono">{{ row.nonUsed }} / {{ row.nonCap }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.full ? 'danger' : 'success'" size="small" effect="light">
              {{ row.full ? '已满' : '可接单' }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty><EmptyState icon="🗓" title="该时段暂无容量配置" desc="在「容量配置」中设置师傅时段接单上限" /></template>
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

.sub {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--bd-text-sub);
}

.sub:not(:first-child) {
  margin-top: 20px;
}

.logs {
  max-height: 240px;
  overflow: auto;
  margin: 0;
  padding: 12px;
  background: #0f172a;
  color: #d1fae5;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.7;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--bd-text-sub);
}
</style>
