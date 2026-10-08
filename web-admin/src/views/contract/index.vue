<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import { adminContractList, adminContractStats, adminContractRenew } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoContractList, demoContractStats } from '@/mock/fallback'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const stats = ref({})
const live = ref(false)
const loading = ref(true)

const statusFilter = ref('')
const expiringDays = ref(null)
const keyword = ref('')

const renewVisible = ref(false)
const renewTarget = ref(null)
const renewMonths = ref(12)
const renewing = ref(false)

// 合约状态在页面内局部定义：共享 status.js 的 active 已被「客户分群·活跃」占用，避免语义冲突
const CONTRACT_STATUS = {
  ACTIVE: { type: 'success', label: '生效中' },
  EXPIRED: { type: 'danger', label: '已过期' },
  TERMINATED: { type: 'info', label: '已终止' }
}
const statusMeta = (s) => CONTRACT_STATUS[s] || { type: 'info', label: s || '—' }

// 剩余天数语义：负数=已过期，0~30=临期，>30=正常
const daysLeftType = (d) => (d == null ? 'info' : d < 0 ? 'danger' : d <= 30 ? 'warning' : 'success')
const daysLeftText = (d) => (d == null ? '—' : d < 0 ? '已过期' : d + ' 天')

function buildParams() {
  const params = { page: page.value, size: 20 }
  if (statusFilter.value) params.status = statusFilter.value
  if (expiringDays.value) params.expiringDays = expiringDays.value
  if (keyword.value) params.keyword = keyword.value
  return params
}

async function load() {
  loading.value = true
  const params = buildParams()
  const r = await loadResource(() => adminContractList(params), () => demoContractList(params))
  rows.value = r.data?.list || []
  total.value = r.data?.total || 0
  live.value = r.live
  loading.value = false
}

async function loadStats() {
  const r = await loadResource(() => adminContractStats(), () => demoContractStats())
  stats.value = r.data || {}
}

function openRenew(row) {
  renewTarget.value = row
  renewMonths.value = 12
  renewVisible.value = true
}

async function confirmRenew() {
  renewing.value = true
  try {
    await adminContractRenew({ contractId: renewTarget.value.id, months: renewMonths.value })
    ElMessage.success(`已续约 ${renewMonths.value} 个月`)
  } catch (e) {
    ElMessage.warning('后端不可达，已模拟续约成功（演示）')
  } finally {
    renewing.value = false
    renewVisible.value = false
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
        <h2 class="page-title">合约管理 · 台账与续约</h2>
        <p class="page-sub">基于 customer_contract 真实数据，提供合约台账、到期预警与续约办理（v1.16 新模块）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="mini-stats">
      <StatCard label="合约总数" :value="stats.total || 0" unit="份" />
      <StatCard label="生效中" :value="stats.active || 0" unit="份" />
      <StatCard label="临期（30天）" :value="stats.expiring || 0" unit="份" />
      <StatCard label="已过期" :value="stats.expired || 0" unit="份" tone="down" />
      <StatCard label="生效月费合计" :value="'¥' + (stats.monthlyFeeSum || 0).toLocaleString()" />
    </div>

    <div class="card">
      <div class="card-head">
        <div class="filters">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px" @change="load">
            <el-option label="生效中" value="ACTIVE" />
            <el-option label="已过期" value="EXPIRED" />
            <el-option label="已终止" value="TERMINATED" />
          </el-select>
          <el-select v-model="expiringDays" placeholder="到期筛选" clearable style="width: 140px" @change="load">
            <el-option label="30 天内到期" :value="30" />
            <el-option label="60 天内到期" :value="60" />
            <el-option label="90 天内到期" :value="90" />
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
        <el-table-column prop="id" label="合约号" min-width="110" />
        <el-table-column prop="customerName" label="客户" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="packageName" label="套餐" min-width="150" show-overflow-tooltip />
        <el-table-column prop="communityName" label="小区" min-width="120" />
        <el-table-column label="月费" width="90" align="right">
          <template #default="{ row }"><b>¥{{ row.monthlyFee }}</b></template>
        </el-table-column>
        <el-table-column prop="endDate" label="到期日" width="120" />
        <el-table-column label="剩余" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="daysLeftType(row.daysLeft)" size="small" effect="light">
              {{ daysLeftText(row.daysLeft) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small" effect="light">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openRenew(row)">续约</el-button>
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

    <el-dialog v-model="renewVisible" title="合约续约" width="420px">
      <div v-if="renewTarget" class="renew-body">
        <p>合约：<b>{{ renewTarget.id }}</b> · {{ renewTarget.customerName }} · {{ renewTarget.packageName }}</p>
        <p class="muted">当前到期日：{{ renewTarget.endDate }}（{{ daysLeftText(renewTarget.daysLeft) }}）</p>
        <el-form label-width="90px">
          <el-form-item label="续约月数">
            <el-select v-model="renewMonths">
              <el-option :value="6" label="6 个月" />
              <el-option :value="12" label="12 个月" />
              <el-option :value="24" label="24 个月" />
              <el-option :value="36" label="36 个月" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="renewVisible = false">取消</el-button>
        <el-button type="primary" :loading="renewing" @click="confirmRenew">确认续约</el-button>
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
.renew-body p { margin: 0 0 8px; }
.muted { font-size: 13px; color: var(--bd-text-mute); }
</style>
