<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import {
  adminBroadbandList, adminBroadbandStats, adminBroadbandLogs,
  adminBroadbandPause, adminBroadbandResume
} from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoBroadbandList, demoBroadbandStats, demoBroadbandLogs } from '@/mock/fallback'
import { useHouseholdStore } from '@/store/household'

const household = useHouseholdStore()

const rows = ref([])
const total = ref(0)
const page = ref(1)
const stats = ref({})
const live = ref(false)
const loading = ref(true)

const statusFilter = ref('')
const keyword = ref('')

// 抽屉：暂停/恢复记录
const logVisible = ref(false)
const logRows = ref([])
const logTotal = ref(0)
const logLoading = ref(false)
const logCustomer = ref(null)

// 操作对话框
const actionVisible = ref(false)
const actionType = ref('PAUSE') // PAUSE / RESUME
const actionTarget = ref(null)
const actionReason = ref('')
const acting = ref(false)

// 宽带服务状态：页面内局部定义，避免与 status.js 中已被「客户分群·活跃」占用的 active 冲突
const BB_STATUS = {
  ACTIVE: { type: 'success', label: '在用' },
  SUSPENDED: { type: 'warning', label: '已暂停' },
  CLOSED: { type: 'info', label: '已销户' }
}
const statusMeta = (s) => BB_STATUS[s] || { type: 'info', label: s || '—' }
const logTypeMeta = (t) => (t === 'PAUSE' ? { type: 'warning', label: '暂停' } : { type: 'success', label: '恢复' })

function buildParams() {
  const params = { page: page.value, size: 20 }
  if (statusFilter.value) params.status = statusFilter.value
  if (keyword.value) params.keyword = keyword.value
  // 多住宅隔离（US-3.1）：按当前选中住宅过滤客户台账
  if (household.currentId) params.householdId = household.currentId
  return params
}

async function load() {
  loading.value = true
  const params = buildParams()
  const r = await loadResource(() => adminBroadbandList(params), () => demoBroadbandList(params))
  rows.value = r.data?.list || []
  total.value = r.data?.total || 0
  live.value = r.live
  loading.value = false
}

async function loadStats() {
  const r = await loadResource(() => adminBroadbandStats(), () => demoBroadbandStats())
  stats.value = r.data || {}
}

async function openLogs(row) {
  logCustomer.value = row
  logVisible.value = true
  logLoading.value = true
  const r = await loadResource(
    () => adminBroadbandLogs({ customerId: row.id, page: 1, size: 20 }),
    () => demoBroadbandLogs({ customerId: row.id })
  )
  logRows.value = r.data?.list || []
  logTotal.value = r.data?.total || 0
  logLoading.value = false
}

function openAction(row, type) {
  actionTarget.value = row
  actionType.value = type
  actionReason.value = ''
  actionVisible.value = true
}

async function confirmAction() {
  if (!actionReason.value || !actionReason.value.trim()) {
    ElMessage.warning('请填写操作原因（便于审计追溯）')
    return
  }
  acting.value = true
  const target = actionTarget.value
  try {
    if (actionType.value === 'PAUSE') {
      await adminBroadbandPause({ customerId: target.id, reason: actionReason.value.trim() })
      ElMessage.success(`已暂停 ${target.customerName} 的宽带服务`)
    } else {
      await adminBroadbandResume({ customerId: target.id, reason: actionReason.value.trim() })
      ElMessage.success(`已恢复 ${target.customerName} 的宽带服务`)
    }
  } catch (e) {
    ElMessage.warning('后端不可达，操作已在演示态记录（未落库）')
  } finally {
    acting.value = false
    actionVisible.value = false
    await Promise.all([load(), loadStats()])
  }
}

onMounted(async () => {
  await Promise.all([load(), loadStats()])
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">宽带服务管理 · 暂停与恢复</h2>
        <p class="page-sub">复用 customer 既有 status 字段（ACTIVE/SUSPENDED/CLOSED）驱动状态机，每次操作留痕审计（v1.17 新模块）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="mini-stats">
      <StatCard label="客户总数" :value="stats.total || 0" unit="户" />
      <StatCard label="在用" :value="stats.active || 0" unit="户" />
      <StatCard label="已暂停" :value="stats.suspended || 0" unit="户" tone="down" />
      <StatCard label="已销户" :value="stats.closed || 0" unit="户" />
    </div>

    <div class="card">
      <div class="card-head">
        <div class="filters">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px" @change="load">
            <el-option label="在用" value="ACTIVE" />
            <el-option label="已暂停" value="SUSPENDED" />
            <el-option label="已销户" value="CLOSED" />
          </el-select>
          <el-input
            v-model="keyword"
            placeholder="客户姓名 / 手机号"
            clearable
            style="width: 200px"
            @keyup.enter="load"
            @clear="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" size="small" style="width: 100%">
        <el-table-column prop="customerName" label="客户" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="level" label="分层" width="80" />
        <el-table-column prop="packageName" label="套餐" min-width="150" show-overflow-tooltip />
        <el-table-column prop="communityName" label="小区" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small" effect="light">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE'"
              link type="warning" size="small"
              @click="openAction(row, 'PAUSE')"
            >暂停</el-button>
            <el-button
              v-else-if="row.status === 'SUSPENDED'"
              link type="success" size="small"
              @click="openAction(row, 'RESUME')"
            >恢复</el-button>
            <el-tag v-else type="info" size="small" effect="plain">终态</el-tag>
            <el-button link type="primary" size="small" @click="openLogs(row)">记录</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 暂停/恢复操作对话框 -->
    <el-dialog v-model="actionVisible" :title="actionType === 'PAUSE' ? '暂停宽带服务' : '恢复宽带服务'" width="440px">
      <div v-if="actionTarget" class="act-body">
        <p>客户：<b>{{ actionTarget.customerName }}</b> · {{ actionTarget.phone }} · {{ actionTarget.packageName }}</p>
        <p class="muted">当前状态：{{ statusMeta(actionTarget.status).label }}</p>
        <el-form label-width="80px">
          <el-form-item :label="actionType === 'PAUSE' ? '暂停原因' : '恢复原因'">
            <el-input
              v-model="actionReason"
              type="textarea"
              :rows="3"
              :placeholder="actionType === 'PAUSE' ? '如：长期外出 / 欠费催停 / 客户申请' : '如：已返深 / 欠费结清 / 客户申请'"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button
          :type="actionType === 'PAUSE' ? 'warning' : 'success'"
          :loading="acting"
          @click="confirmAction"
        >确认{{ actionType === 'PAUSE' ? '暂停' : '恢复' }}</el-button>
      </template>
    </el-dialog>

    <!-- 暂停/恢复记录抽屉 -->
    <el-drawer v-model="logVisible" :title="(logCustomer ? logCustomer.customerName + ' · ' : '') + '暂停/恢复记录'" size="520px">
      <el-table v-loading="logLoading" :data="logRows" size="small" style="width: 100%">
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="logTypeMeta(row.type).type" size="small" effect="light">{{ logTypeMeta(row.type).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="reason" label="原因" min-width="140" show-overflow-tooltip />
        <el-table-column label="时间" min-width="140">
          <template #default="{ row }">{{ new Date(row.createdTime).toLocaleString('zh-CN') }}</template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination :page-size="20" :total="logTotal" layout="total" disabled />
      </div>
    </el-drawer>
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
.mini-stats { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.mini-stats > * { flex: 1; min-width: 150px; }
.card { margin-bottom: 16px; }
.card-head { margin-bottom: 12px; }
.filters { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.act-body p { margin: 0 0 8px; }
.muted { font-size: 13px; color: var(--bd-text-mute); }
</style>
