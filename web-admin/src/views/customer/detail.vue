<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { customer360 } from '@/api/business'
import { loadResource, money, pct, fmtTime } from '@/composables/useResource'
import { demoCustomer360 } from '@/mock/fallback'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)

const loading = ref(true)
const live = ref(false)
const data = ref(null)
const notFound = ref(false)

const profile = computed(() => data.value?.profile)
const orders = computed(() => data.value?.orders || [])
const contracts = computed(() => data.value?.contracts || [])
const traffic = computed(() => data.value?.traffic)
const reviews = computed(() => data.value?.reviews || [])
const workOrders = computed(() => data.value?.workOrders || [])
const upgradeOrders = computed(() => data.value?.upgradeOrders || [])
const summary = computed(() => data.value?.summary || {})
const lifecycle = computed(() => data.value?.lifecycle || {})
const touchRecords = computed(() => data.value?.touchRecords || [])

const TOUCH_LABEL = { order: '下单', review: '评价', complaint: '投诉', install: '安装', upgrade: '升级', contract: '合约' }
const TOUCH_COLOR = { order: '#4f46e5', review: '#22c55e', complaint: '#ef4444', install: '#f59e0b', upgrade: '#a855f7', contract: '#06b6d4' }
const TOUCH_KEYS = { order: 'order', review: 'review', complaint: 'complaint', install: 'install', upgrade: 'upgrade', contract: 'contract' }
const touchLabel = (tt) => t('customer360.touch.' + (TOUCH_KEYS[tt] || 'event'))
const touchColor = (t) => TOUCH_COLOR[t] || '#4f46e5'

const levelType = (l) => ({ 五星: 'danger', 四星: 'warning', 三星: 'info', VIP: 'danger', GOLD: 'warning', SILVER: 'info' }[l] || 'info')
const statusType = (s) => ({ 在用: 'success', 暂停: 'warning', 已销户: 'info', 生效中: 'success', 待续约: 'warning' }[s] || 'info')

const tab = ref('profile')
const activeTab = computed(() => tab.value)

onMounted(async () => {
  const r = await loadResource(() => customer360(id.value), () => demoCustomer360(id.value))
  data.value = r.data
  live.value = r.live
  notFound.value = !profile.value
  loading.value = false
})

function back() {
  router.push('/customer')
}

// 生命周期标签下钻：跳转到「触达记录」页签，查看驱动该阶段的全部互动明细
function drillLifecycle() {
  tab.value = 'touch'
}

const trendMax = computed(() => {
  const t = traffic.value?.dailyTrend
  return t && t.length ? Math.max(...t) : 1
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div class="hleft">
        <el-button text :icon="'←'" @click="back">{{ t('customer360.back') }}</el-button>
        <h2 class="page-title">{{ t('customer360.title') }}</h2>
      </div>
      <SourceTag :live="live" />
    </div>

    <div v-loading="loading" class="body">
      <el-empty v-if="notFound" :description="t('customer360.notFound')" />

      <template v-else-if="profile">
        <!-- 客户档案头卡 -->
        <div class="hero card">
          <el-avatar :size="56" style="background: var(--bd-primary)">{{ profile.name?.slice(0, 1) }}</el-avatar>
          <div class="hinfo">
            <div class="hline">
              <b>{{ profile.name }}</b>
              <el-tag :type="levelType(profile.level)" size="small" effect="light">{{ profile.levelLabel }}</el-tag>
              <el-tag :type="statusType(profile.statusLabel)" size="small" effect="light">{{ profile.statusLabel }}</el-tag>
              <el-tag v-if="lifecycle.stageLabel" :type="lifecycle.color" size="small" effect="dark" class="lifecycle-tag" @click="drillLifecycle">{{ lifecycle.stageLabel }}<span class="drill">下钻 ›</span></el-tag>
              <span v-for="tg in profile.tags || []" :key="tg" class="ctag">{{ tg }}</span>
            </div>
            <p>📱 {{ profile.phone }} ｜ 🏠 {{ profile.communityName || '—' }} {{ profile.address || '' }}</p>
            <p class="sub">当前套餐：{{ profile.pkgName || '—' }} ｜ 月费 {{ money(profile.pkgMonthlyFee) }} ｜ 合约：{{ profile.contractEnd || '—' }}（{{ profile.contractStatus || '—' }}）</p>
          </div>
        </div>

        <!-- 汇总指标 -->
        <div class="stats">
          <div class="stat"><span class="num">{{ summary.orderCount ?? 0 }}</span><span class="lab">{{ t('customer360.statOrderCount') }}</span></div>
          <div class="stat"><span class="num">{{ money(summary.paidAmount) }}</span><span class="lab">{{ t('customer360.statPaidAmount') }}</span></div>
          <div class="stat"><span class="num">{{ summary.workOrderCount ?? 0 }}</span><span class="lab">{{ t('customer360.statWorkOrder') }}</span></div>
          <div class="stat"><span class="num">{{ summary.reviewCount ?? 0 }}</span><span class="lab">{{ t('customer360.statReview') }}</span></div>
          <div class="stat"><span class="num">{{ summary.avgScore ?? 0 }}<i>分</i></span><span class="lab">{{ t('customer360.statAvgScore') }}</span></div>
          <div class="stat"><span class="num">{{ summary.upgradeCount ?? 0 }}</span><span class="lab">{{ t('customer360.statUpgrade') }}</span></div>
        </div>

        <!-- 分栏 -->
        <el-tabs v-model="activeTab" class="tabs">
          <el-tab-pane :label="t('customer360.tabProfile')" name="profile">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item :label="t('customer360.descId')">{{ profile.id }}</el-descriptions-item>
              <el-descriptions-item :label="t('customer360.descName')">{{ profile.name }}</el-descriptions-item>
              <el-descriptions-item :label="t('customer360.descPhone')">{{ profile.phone }}</el-descriptions-item>
              <el-descriptions-item :label="t('customer360.descLevel')">{{ profile.levelLabel }}</el-descriptions-item>
              <el-descriptions-item :label="t('customer360.descPackage')">{{ profile.pkgName || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="t('customer360.descMonthlyFee')">{{ money(profile.pkgMonthlyFee) }}</el-descriptions-item>
              <el-descriptions-item :label="t('customer360.descCommunity')">{{ profile.communityName || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="t('customer360.descAddress')">{{ profile.address || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="t('customer360.descStatus')">{{ profile.statusLabel }}</el-descriptions-item>
              <el-descriptions-item :label="t('customer360.descContractEnd')">{{ profile.contractEnd || '—' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane :label="t('customer360.tabOrders')" name="orders">
            <el-table :data="orders" size="small" :empty-text="t('customer360.emptyOrders')">
              <el-table-column prop="id" :label="t('customer360.colOrderId')" min-width="160" />
              <el-table-column prop="packageName" :label="t('customer360.colPackage')" min-width="140" />
              <el-table-column :label="t('customer360.colAmount')" width="100"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
              <el-table-column prop="orderTypeLabel" :label="t('customer360.colType')" width="90" />
              <el-table-column prop="statusLabel" :label="t('customer360.colStatus')" width="100" />
              <el-table-column prop="createdAt" :label="t('customer360.colTime')" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('customer360.tabContracts')" name="contracts">
            <el-table :data="contracts" size="small" :empty-text="t('customer360.emptyContracts')">
              <el-table-column prop="id" :label="t('customer360.colContractId')" min-width="140" />
              <el-table-column prop="packageId" :label="t('customer360.colPackageId')" min-width="120" />
              <el-table-column :label="t('customer360.colMonthlyFee')" width="100"><template #default="{ row }">{{ money(row.monthlyFee) }}</template></el-table-column>
              <el-table-column prop="startDate" :label="t('customer360.colStartDate')" width="120" />
              <el-table-column prop="endDate" :label="t('customer360.colEndDate')" width="120" />
              <el-table-column prop="statusLabel" :label="t('customer360.colStatus')" width="100" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('customer360.tabTraffic')" name="traffic">
            <div v-if="traffic" class="traffic">
              <div class="tline">
                <span>{{ traffic.mobileUsed }}G / {{ traffic.mobileTotal }}G（手机）｜ 峰值 {{ traffic.broadbandPeak || '—' }} ｜ 当月时长 {{ traffic.broadbandHours || 0 }}h</span>
                <el-tag :type="traffic.mobileUsed >= traffic.mobileTotal ? 'danger' : 'success'" size="small" effect="light">
                  {{ pct(traffic.mobileUsed, traffic.mobileTotal) }}%
                </el-tag>
              </div>
              <el-progress :percentage="pct(traffic.mobileUsed, traffic.mobileTotal)"
                           :color="traffic.mobileUsed >= traffic.mobileTotal ? '#dc2626' : '#4f46e5'" :stroke-width="10" />
              <h4 class="sec">{{ t('customer360.trafficTrendTitle') }}</h4>
              <div class="bars">
                <div v-for="(v, i) in (traffic.dailyTrend || [])" :key="i" class="bar">
                  <div class="bcol" :style="{ height: (v / trendMax * 60) + 'px' }"></div>
                  <span class="blab">{{ v }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else :description="t('customer360.emptyTraffic')" :image-size="60" />
          </el-tab-pane>

          <el-tab-pane :label="t('customer360.tabWork')" name="work">
            <el-table :data="workOrders" size="small" :empty-text="t('customer360.emptyWork')">
              <el-table-column prop="id" :label="t('customer360.colWorkId')" min-width="120" />
              <el-table-column prop="statusLabel" :label="t('customer360.colStatus')" width="90" />
              <el-table-column prop="timeSlot" :label="t('customer360.colTimeSlot')" min-width="130" />
              <el-table-column prop="packageDesc" :label="t('customer360.colBusiness')" width="90" />
              <el-table-column prop="workerId" :label="t('customer360.colWorker')" width="100" />
              <el-table-column :label="t('customer360.colSpeed')" width="120">
                <template #default="{ row }">{{ row.downSpeed ?? '—' }} / {{ row.upSpeed ?? '—' }}</template>
              </el-table-column>
              <el-table-column prop="completeTime" :label="t('customer360.colCompleteTime')" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('customer360.tabReviews')" name="reviews">
            <el-table :data="reviews" size="small" :empty-text="t('customer360.emptyReviews')">
              <el-table-column prop="typeLabel" :label="t('customer360.colReviewType')" width="80" />
              <el-table-column :label="t('customer360.colScore')" width="80"><template #default="{ row }">{{ row.score }}★</template></el-table-column>
              <el-table-column :label="t('customer360.colTags')" min-width="160">
                <template #default="{ row }">
                  <el-tag v-for="tg in (row.tags || [])" :key="tg" size="small" effect="plain" style="margin-right:4px">{{ tg }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="content" :label="t('customer360.colContent')" min-width="200" show-overflow-tooltip />
              <el-table-column prop="statusLabel" :label="t('customer360.colReviewStatus')" width="90" />
              <el-table-column prop="createdAt" :label="t('customer360.colReviewTime')" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('customer360.tabUpgrade')" name="upgrade">
            <el-table :data="upgradeOrders" size="small" :empty-text="t('customer360.emptyUpgrade')">
              <el-table-column prop="id" :label="t('customer360.colApplyId')" min-width="150" />
              <el-table-column prop="fromPackageId" :label="t('customer360.colFromPackage')" min-width="120" />
              <el-table-column prop="targetBandKey" :label="t('customer360.colTarget')" min-width="120" />
              <el-table-column :label="t('customer360.colMonthDiff')" width="90"><template #default="{ row }">+{{ money(row.monthDiff) }}</template></el-table-column>
              <el-table-column :label="t('customer360.colOneTimeDiff')" width="110"><template #default="{ row }">{{ money(row.oneTimeDiff) }}</template></el-table-column>
              <el-table-column prop="statusLabel" :label="t('customer360.colApplyStatus')" width="90" />
              <el-table-column prop="createdAt" :label="t('customer360.colApplyTime')" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('customer360.tabTouch')" name="touch">
            <div v-if="lifecycle.reasons && lifecycle.reasons.length" class="lifecycle-reasons">
              <el-tag :type="lifecycle.color" size="small" effect="dark">{{ lifecycle.stageLabel }}</el-tag>
              <span v-for="(r, i) in lifecycle.reasons" :key="i" class="reason">{{ r }}</span>
            </div>
            <el-timeline v-if="touchRecords.length" class="touch-timeline">
              <el-timeline-item
                v-for="(rec, i) in touchRecords" :key="i"
                :timestamp="rec.time ? fmtTime(rec.time) : (rec.timeText || '')"
                :color="touchColor(rec.type)"
                placement="top"
              >
                <div class="touch-item">
                  <el-tag size="small" effect="plain" :style="{ color: touchColor(rec.type), borderColor: touchColor(rec.type) }">{{ touchLabel(rec.type) }}</el-tag>
                  <b class="touch-title">{{ rec.title }}</b>
                  <p v-if="rec.detail" class="touch-detail">{{ rec.detail }}</p>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else :description="t('customer360.emptyTouch')" :image-size="60" />
          </el-tab-pane>
        </el-tabs>
      </template>
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
.hleft {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.page-title {
  margin: 0;
  font-size: 18px;
}
.hero {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
}
.hinfo { flex: 1; min-width: 0; }
.hline {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.hline b { font-size: 16px; }
.hinfo p { margin: 4px 0 0; font-size: 13px; color: var(--bd-text-sub); }
.hinfo .sub { font-size: 12px; }
.ctag {
  font-size: 11px;
  color: var(--bd-primary);
  background: rgba(79, 70, 229, 0.08);
  border-radius: 4px;
  padding: 1px 7px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin: 16px 0;
}
.stat {
  background: #fff;
  border: 1px solid var(--bd-border, #ececf5);
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.stat .num { font-size: 20px; font-weight: 700; color: var(--bd-primary); }
.stat .num i { font-size: 12px; font-style: normal; color: var(--bd-text-sub); }
.stat .lab { font-size: 12px; color: var(--bd-text-sub); }
.body { min-height: 200px; }
.traffic { max-width: 560px; }
.tline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
}
.sec { margin: 18px 0 10px; font-size: 13px; color: var(--bd-text-sub); font-weight: 600; }
.bars { display: flex; align-items: flex-end; gap: 10px; height: 80px; }
.bar { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.bcol { width: 22px; background: var(--bd-primary); border-radius: 4px 4px 0 0; }
.blab { font-size: 11px; color: var(--bd-text-sub); }
.tabs { margin-top: 4px; }

.lifecycle-reasons {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.lifecycle-tag { cursor: pointer; }
.lifecycle-tag .drill {
  margin-left: 6px;
  font-size: 11px;
  opacity: 0.85;
}
.reason {
  font-size: 12px;
  color: var(--bd-text-sub);
  background: #f5f6fb;
  border-radius: 4px;
  padding: 2px 8px;
}
.touch-timeline { padding: 8px 4px 0; max-width: 760px; }
.touch-item { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.touch-title { font-size: 13px; }
.touch-detail { margin: 2px 0 0; font-size: 12px; color: var(--bd-text-sub); width: 100%; }
</style>
