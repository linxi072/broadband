<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { customerList, upgradeOptions, trafficUsage } from '@/api/business'
import { loadResource, money, pct } from '@/composables/useResource'
import { demoCustomers } from '@/mock/fallback'

const rows = ref([])
const live = ref(false)
const loading = ref(true)
const keyword = ref('')
const level = ref('')
const router = useRouter()

function go360(row) {
  router.push('/customer/' + (row.id || ''))
}

const drawer = ref(false)
const detail = ref(null)
const detailLoading = ref(false)
const detailLive = ref(false)
const upgradeInfo = ref(null)

const LEVELS = ['五星', '四星', '三星']

const filtered = computed(() =>
  rows.value.filter((r) => {
    const kw = keyword.value.trim()
    const okKw = !kw || String(r.name).includes(kw) || String(r.phone).includes(kw)
    const okLevel = !level.value || r.level === level.value
    return okKw && okLevel
  })
)

const levelType = (l) => ({ 五星: 'danger', 四星: 'warning', 三星: 'info' }[l] || 'info')

async function openDetail(row) {
  drawer.value = true
  detail.value = row
  detailLoading.value = true
  upgradeInfo.value = null

  const [up, tr] = await Promise.all([
    loadResource(() => upgradeOptions(row.id || 'demo'), null),
    loadResource(() => trafficUsage(row.id || 'demo'), null)
  ])
  detailLive.value = up.live || tr.live
  if (up.data) upgradeInfo.value = up.data
  if (tr.data) detail.value = { ...detail.value, traffic: tr.data }
  detailLoading.value = false
}

onMounted(async () => {
  const r = await loadResource(() => customerList({ size: 200 }), demoCustomers)
  const data = r.data
  rows.value = Array.isArray(data) ? data : (data && data.records) || demoCustomers
  live.value = r.live
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">客户管理</h2>
        <p class="page-sub">客户分层、合约状态与流量/升级情况</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="客户姓名 / 手机号" clearable style="width: 220px" />
        <el-select v-model="level" placeholder="全部等级" clearable style="width: 140px">
          <el-option v-for="l in LEVELS" :key="l" :label="l" :value="l" />
        </el-select>
        <div class="spacer"></div>
        <span class="count">共 {{ filtered.length }} 位客户</span>
      </div>

      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="id" label="客户ID" width="90" />
        <el-table-column prop="name" label="客户" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="等级" width="90">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" size="small" effect="light">{{ row.level || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pkgName" label="当前套餐" min-width="150" />
        <el-table-column label="月费" width="100">
          <template #default="{ row }">{{ money(row.monthlyFee) }}</template>
        </el-table-column>
        <el-table-column prop="contractEnd" label="合约到期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '待续约' ? 'warning' : 'success'" size="small" effect="light">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="t in row.tags || []" :key="t" size="small" effect="plain" style="margin-right: 4px">
              {{ t }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState icon="👥" title="暂无客户数据" desc="录入宽带客户档案，支撑订单与售后关联" /></template>
      </el-table>
    </div>

    <el-drawer v-model="drawer" title="客户详情" size="440px">
      <div v-if="detail" v-loading="detailLoading" class="detail">
        <div class="dhead">
          <el-avatar :size="44" style="background: var(--bd-primary)">{{ detail.name?.slice(0, 1) }}</el-avatar>
          <div>
            <b>{{ detail.name }}</b>
            <p>{{ detail.phone }} ｜ {{ detail.level || '—' }}</p>
          </div>
          <el-button size="small" type="primary" link @click="go360(detail)">查看 360 全景 →</el-button>
          <SourceTag :live="detailLive" />
        </div>

        <el-descriptions :column="1" border size="small" style="margin-top: 16px">
          <el-descriptions-item label="客户ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="当前套餐">{{ detail.pkgName }}</el-descriptions-item>
          <el-descriptions-item label="月费">{{ money(detail.monthlyFee) }}</el-descriptions-item>
          <el-descriptions-item label="合约到期">{{ detail.contractEnd }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="sec">流量使用</h4>
        <template v-if="detail.traffic">
          <div class="tline">
            <span>{{ detail.traffic.mobileUsed }}G / {{ detail.traffic.mobileTotal }}G</span>
            <el-tag :type="detail.traffic.warn ? 'danger' : 'success'" size="small" effect="light">
              {{ detail.traffic.warn ? '预警' : '正常' }}
            </el-tag>
          </div>
          <el-progress
            :percentage="pct(detail.traffic.mobileUsed, detail.traffic.mobileTotal)"
            :color="detail.traffic.warn ? '#dc2626' : '#4f46e5'"
            :stroke-width="10"
          />
        </template>
        <el-empty v-else description="未取到流量数据（需 customerId 命中）" :image-size="60" />

        <h4 class="sec">升档选项</h4>
        <template v-if="upgradeInfo">
          <p class="hint-line">
            当前套餐 {{ upgradeInfo.current?.name || '—' }} ｜ 月费
            {{ money(upgradeInfo.current?.fee) }} ｜ 剩余合约
            <b>{{ upgradeInfo.current?.contractLeftMonths ?? 0 }}</b> 个月
          </p>
          <el-table :data="(upgradeInfo.options || []).filter((o) => o.type === 'bandwidth')" size="small" max-height="200">
            <el-table-column prop="name" label="可升档" />
            <el-table-column label="月加价" width="90">
              <template #default="{ row }">+{{ money(row.extraFee) }}</template>
            </el-table-column>
          </el-table>
        </template>
        <el-empty v-else description="未取到升档信息" :image-size="60" />
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
}

.count {
  font-size: 12px;
  color: var(--bd-text-sub);
}

.dhead {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dhead b {
  font-size: 15px;
}

.dhead p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--bd-text-sub);
}

.dhead > :last-child {
  margin-left: auto;
}

.sec {
  margin: 20px 0 10px;
  font-size: 13px;
  color: var(--bd-text-sub);
  font-weight: 600;
}

.tline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}

.hint-line {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--bd-text-sub);
}
</style>
