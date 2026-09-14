<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import { workerList, saveWorkerCapacity, getCapacity } from '@/api/business'
import { loadResource, today } from '@/composables/useResource'
import { demoWorkers, demoCapacity } from '@/mock/fallback'

const workers = ref(demoWorkers)
const workersLive = ref(false)
const timeSlot = ref(`${today()}#AM`)
const board = ref([])
const live = ref(false)
const loading = ref(true)

const drawer = ref(false)
const saving = ref(false)
const edit = reactive({ workerId: '', workerName: '', slots: [] })

const slots = [
  { key: 'AM', label: '09:00-12:00' },
  { key: 'PM', label: '14:00-17:00' },
  { key: 'EV', label: '19:00-21:00' }
]

/** 全局默认容量（后端 CapacityPolicy 常量口径） */
const globalCap = reactive({ adj: 4, adjMin: 3, non: 2, nonMin: 1, slotsPerDay: 3 })

const rows = computed(() => {
  if (board.value.length) {
    return board.value.map((b) => ({
      workerId: b.workerId,
      workerName: b.workerName,
      timeSlot: b.timeSlot,
      adjUsed: b.adjUsed,
      adjCap: b.adjCap,
      nonUsed: b.nonUsed,
      nonCap: b.nonCap,
      full: b.full,
      mode: '单独配置'
    }))
  }
  return demoCapacity.map((c) => ({
    ...c,
    adjUsed: Math.min(c.adjCap, 2),
    nonUsed: Math.min(c.nonCap, 1),
    full: false
  }))
})

const levelOf = (id) => (workers.value.find((w) => w.id === id) || {}).level || '—'

async function load() {
  loading.value = true
  const [w, b] = await Promise.all([
    loadResource(() => workerList(), demoWorkers),
    loadResource(() => getCapacity(timeSlot.value), [])
  ])
  const wl = Array.isArray(w.data) ? w.data : (w.data && w.data.records) || demoWorkers
  workers.value = wl
  workersLive.value = w.live
  board.value = Array.isArray(b.data) ? b.data : []
  live.value = b.live
  loading.value = false
}

function openEdit(row) {
  edit.workerId = row.workerId
  edit.workerName = row.workerName || row.workerId
  edit.slots = slots.map((s) => ({
    key: s.key,
    label: s.label,
    adjCap: row.adjCap || globalCap.adj,
    nonCap: row.nonCap || globalCap.non,
    enabled: true
  }))
  drawer.value = true
}

async function submit() {
  saving.value = true
  const r = await loadResource(
    () =>
      saveWorkerCapacity({
        workerId: edit.workerId,
        day: today(),
        slots: edit.slots.map((s) => ({
          slotKey: s.key,
          adjacentCap: s.adjCap,
          nonAdjacentCap: s.nonCap,
          enabled: s.enabled
        }))
      }),
    null
  )
  saving.value = false
  if (r.live) {
    ElMessage.success(`${edit.workerName} 容量已保存`)
    drawer.value = false
    load()
  } else {
    ElMessage.warning('容量写入接口不可达：未落库')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">师傅容量配置</h2>
        <p class="page-sub">
          相邻小区每时段 3–4 单、非相邻 1–2 单；未单独配置的师傅继承全局默认（数据源 worker_capacity 表）
        </p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat is-warn">
        <div class="label">全局默认 · 相邻</div>
        <div class="value">{{ globalCap.adjMin }}–{{ globalCap.adj }}<small> 单/时段</small></div>
      </div>
      <div class="stat is-warn">
        <div class="label">全局默认 · 非相邻</div>
        <div class="value">{{ globalCap.nonMin }}–{{ globalCap.non }}<small> 单/时段</small></div>
      </div>
      <div class="stat">
        <div class="label">每日时段数</div>
        <div class="value">{{ globalCap.slotsPerDay }}<small> 段</small></div>
      </div>
      <div class="stat is-down">
        <div class="label">已单配师傅</div>
        <div class="value">{{ rows.filter((r) => r.mode === '单独配置').length }}<small> / {{ workers.length }} 人</small></div>
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <h3 style="margin: 0; font-size: 15px">师傅容量明细（每师傅 × 时段上限）</h3>
        <el-select v-model="timeSlot" style="width: 180px" @change="load">
          <el-option :label="`${today()} 上午`" :value="`${today()}#AM`" />
          <el-option :label="`${today()} 下午`" :value="`${today()}#PM`" />
          <el-option :label="`${today()} 晚间`" :value="`${today()}#EV`" />
        </el-select>
        <div class="spacer"></div>
        <SourceTag :live="workersLive" label="师傅" />
        <el-button size="small" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column prop="workerName" label="师傅" width="120" />
        <el-table-column label="技能等级" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="levelOf(row.workerId) === '高级' ? 'success' : 'info'" effect="light">
              {{ levelOf(row.workerId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时段" width="140">
          <template #default="{ row }">{{ row.timeSlot }}</template>
        </el-table-column>
        <el-table-column label="相邻上限" width="180">
          <template #default="{ row }">
            <span class="mono">{{ row.adjUsed }} / {{ row.adjCap }}</span>
            <el-progress
              :percentage="Math.min(100, Math.round((row.adjUsed / Math.max(1, row.adjCap)) * 100))"
              :stroke-width="8"
              :color="row.adjUsed >= row.adjCap ? '#dc2626' : '#4f46e5'"
              :show-text="false"
            />
          </template>
        </el-table-column>
        <el-table-column label="非相邻上限" width="180">
          <template #default="{ row }">
            <span class="mono">{{ row.nonUsed }} / {{ row.nonCap }}</span>
            <el-progress
              :percentage="Math.min(100, Math.round((row.nonUsed / Math.max(1, row.nonCap)) * 100))"
              :stroke-width="8"
              :color="row.nonUsed >= row.nonCap ? '#dc2626' : '#10b981'"
              :show-text="false"
            />
          </template>
        </el-table-column>
        <el-table-column prop="mode" label="配置方式" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.full ? 'danger' : 'success'" size="small" effect="light">
              {{ row.full ? '已满' : '可接单' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>暂无容量配置</template>
      </el-table>
    </div>

    <div class="card">
      <div class="card-head"><h3>配置建议</h3></div>
      <div class="card-body">
        <ul class="tips">
          <li>初级师傅建议相邻上限下调为 2，防止新人工单过载。</li>
          <li>同一小区聚集 3 单以上时，优先合并给同一师傅（相邻路线），减少往返。</li>
          <li>容量已满时算法不再派单，工单进入异常拦截并给出改派 / 调整时段建议。</li>
        </ul>
      </div>
    </div>

    <el-drawer v-model="drawer" :title="`${edit.workerName} · 按时段设置容量`" size="420px">
      <el-table :data="edit.slots" size="small">
        <el-table-column prop="label" label="时间段" width="120" />
        <el-table-column label="相邻上限" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.adjCap" :min="0" :max="8" size="small" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="非相邻上限" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.nonCap" :min="0" :max="6" size="small" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="启用" width="70">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" size="small" />
          </template>
        </el-table-column>
      </el-table>
      <el-button type="primary" style="width: 100%; margin-top: 16px" :loading="saving" @click="submit">
        保存容量
      </el-button>
    </el-drawer>
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
  color: var(--bd-text-sub);
}

.tips {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--bd-text-sub);
  line-height: 2;
}
</style>
