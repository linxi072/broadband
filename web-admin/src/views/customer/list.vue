<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { customerList, upgradeOptions, trafficUsage } from '@/api/business'
import { loadResource, money, pct } from '@/composables/useResource'
import { demoCustomers } from '@/mock/fallback'
import { useI18n } from '@/i18n'

const { t } = useI18n()
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
const LEVEL_KEYS = { '五星': 'fiveStar', '四星': 'fourStar', '三星': 'threeStar' }

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
        <h2 class="page-title">{{ t('customer.title') }}</h2>
        <p class="page-sub">{{ t('customer.sub') }}</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" :placeholder="t('customer.placeholderKeyword')" clearable style="width: 220px" />
        <el-select v-model="level" :placeholder="t('customer.placeholderLevel')" clearable style="width: 140px">
          <el-option v-for="l in LEVELS" :key="l" :label="t('customer.level.' + LEVEL_KEYS[l])" :value="l" />
        </el-select>
        <div class="spacer"></div>
        <span class="count">{{ filtered.length }} {{ t('customer.countSuffix') }}</span>
      </div>

      <el-table v-loading="loading" :data="filtered" style="width: 100%">
        <el-table-column prop="id" :label="t('customer.colId')" width="90" />
        <el-table-column prop="name" :label="t('customer.colName')" width="100" />
        <el-table-column prop="phone" :label="t('customer.colPhone')" width="130" />
        <el-table-column :label="t('customer.colLevel')" width="90">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" size="small" effect="light">{{ row.level || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pkgName" :label="t('customer.colPackage')" min-width="150" />
        <el-table-column :label="t('customer.colMonthlyFee')" width="100">
          <template #default="{ row }">{{ money(row.monthlyFee) }}</template>
        </el-table-column>
        <el-table-column prop="contractEnd" :label="t('customer.colContractEnd')" width="120" />
        <el-table-column :label="t('customer.colStatus')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '待续约' ? 'warning' : 'success'" size="small" effect="light">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('customer.colTags')" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="tg in row.tags || []" :key="tg" size="small" effect="plain" style="margin-right: 4px">
              {{ tg }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('customer.colAction')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">{{ t('customer.detail') }}</el-button>
          </template>
        </el-table-column>
        <template #empty><EmptyState icon="👥" :title="t('customer.emptyTitle')" :desc="t('customer.emptyDesc')" /></template>
      </el-table>
    </div>

    <el-drawer v-model="drawer" :title="t('customer.drawerTitle')" size="440px">
      <div v-if="detail" v-loading="detailLoading" class="detail">
        <div class="dhead">
          <el-avatar :size="44" style="background: var(--bd-primary)">{{ detail.name?.slice(0, 1) }}</el-avatar>
          <div>
            <b>{{ detail.name }}</b>
            <p>{{ detail.phone }} ｜ {{ detail.level || '—' }}</p>
          </div>
          <el-button size="small" type="primary" link @click="go360(detail)">{{ t('customer.go360') }} →</el-button>
          <SourceTag :live="detailLive" />
        </div>

        <el-descriptions :column="1" border size="small" style="margin-top: 16px">
          <el-descriptions-item :label="t('customer.descId')">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item :label="t('customer.descPackage')">{{ detail.pkgName }}</el-descriptions-item>
          <el-descriptions-item :label="t('customer.descMonthlyFee')">{{ money(detail.monthlyFee) }}</el-descriptions-item>
          <el-descriptions-item :label="t('customer.descContractEnd')">{{ detail.contractEnd }}</el-descriptions-item>
          <el-descriptions-item :label="t('customer.descStatus')">{{ detail.status }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="sec">{{ t('customer.trafficTitle') }}</h4>
        <template v-if="detail.traffic">
          <div class="tline">
            <span>{{ detail.traffic.mobileUsed }}G / {{ detail.traffic.mobileTotal }}G</span>
            <el-tag :type="detail.traffic.warn ? 'danger' : 'success'" size="small" effect="light">
              {{ detail.traffic.warn ? t('customer.trafficWarn') : t('customer.trafficNormal') }}
            </el-tag>
          </div>
          <el-progress
            :percentage="pct(detail.traffic.mobileUsed, detail.traffic.mobileTotal)"
            :color="detail.traffic.warn ? '#dc2626' : '#4f46e5'"
            :stroke-width="10"
          />
        </template>
        <el-empty v-else :description="t('customer.trafficEmpty')" :image-size="60" />

        <h4 class="sec">{{ t('customer.upgradeTitle') }}</h4>
        <template v-if="upgradeInfo">
          <p class="hint-line">
            当前套餐 {{ upgradeInfo.current?.name || '—' }} ｜ 月费
            {{ money(upgradeInfo.current?.fee) }} ｜ 剩余合约
            <b>{{ upgradeInfo.current?.contractLeftMonths ?? 0 }}</b> 个月
          </p>
          <el-table :data="(upgradeInfo.options || []).filter((o) => o.type === 'bandwidth')" size="small" max-height="200">
            <el-table-column prop="name" :label="t('customer.upgradeName')" />
            <el-table-column :label="t('customer.upgradeExtra')" width="90">
              <template #default="{ row }">+{{ money(row.extraFee) }}</template>
            </el-table-column>
          </el-table>
        </template>
        <el-empty v-else :description="t('customer.upgradeEmpty')" :image-size="60" />
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
