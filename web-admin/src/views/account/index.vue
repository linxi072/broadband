<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { accountSummary, accountBills } from '@/api/business'
import { loadResource, money, fmtTime } from '@/composables/useResource'
import { demoAccountSummary, demoAccountBills } from '@/mock/fallback'

const CUSTOMER = 'C-DMO01'

const summary = ref(null)
const bills = ref([])
const live = ref(false)
const loading = ref(true)

const stats = computed(() => {
  const s = summary.value || {}
  return [
    { label: '客户', value: s.name || '—' },
    { label: '会员等级', value: s.level || '—', cls: 'is-up' },
    { label: '当前积分', value: s.points ?? '—', cls: '' },
    { label: '月消费', value: s.monthConsume != null ? money(s.monthConsume) : '—', cls: 'is-warn' },
    { label: '合约到期', value: s.contractEnd || '—' }
  ]
})

async function load() {
  loading.value = true
  const [s, b] = await Promise.all([
    loadResource(() => accountSummary(CUSTOMER), demoAccountSummary),
    loadResource(() => accountBills(CUSTOMER), demoAccountBills)
  ])
  summary.value = s.data
  bills.value = Array.isArray(b.data) ? b.data : []
  live.value = s.live && b.live
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">账户与账单中心</h2>
        <p class="page-sub">客户账户概览与账单流水（数据源 account_summary / account_bill 表）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div v-for="s in stats" :key="s.label" class="stat" :class="s.cls || ''">
        <div class="label">{{ s.label }}</div>
        <div class="value">{{ s.value }}</div>
      </div>
    </div>

    <div class="card">
      <div class="card-head">
        <h3>账单流水</h3>
        <span class="hint">共 {{ bills.length }} 笔</span>
      </div>
      <el-table v-loading="loading" :data="bills" style="width: 100%">
        <el-table-column prop="packageName" label="业务" min-width="160" />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.orderType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.statusText || row.status" />
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.createdTime) }}</template>
        </el-table-column>
        <template #empty>
          <EmptyState icon="💳" title="暂无账单" desc="客户办理、升级、调测等流水将在此汇总" />
        </template>
      </el-table>
    </div>
  </div>
</template>
