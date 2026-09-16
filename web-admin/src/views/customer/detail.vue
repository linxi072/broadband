<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SourceTag from '@/components/SourceTag.vue'
import { customer360 } from '@/api/business'
import { loadResource, money, pct, fmtTime } from '@/composables/useResource'
import { demoCustomer360 } from '@/mock/fallback'

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
const touchLabel = (t) => TOUCH_LABEL[t] || '事件'
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

const trendMax = computed(() => {
  const t = traffic.value?.dailyTrend
  return t && t.length ? Math.max(...t) : 1
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div class="hleft">
        <el-button text :icon="'←'" @click="back">返回客户列表</el-button>
        <h2 class="page-title">客户 360 全景</h2>
      </div>
      <SourceTag :live="live" />
    </div>

    <div v-loading="loading" class="body">
      <el-empty v-if="notFound" description="未找到该客户（id 不存在）" />

      <template v-else-if="profile">
        <!-- 客户档案头卡 -->
        <div class="hero card">
          <el-avatar :size="56" style="background: var(--bd-primary)">{{ profile.name?.slice(0, 1) }}</el-avatar>
          <div class="hinfo">
            <div class="hline">
              <b>{{ profile.name }}</b>
              <el-tag :type="levelType(profile.level)" size="small" effect="light">{{ profile.levelLabel }}</el-tag>
              <el-tag :type="statusType(profile.statusLabel)" size="small" effect="light">{{ profile.statusLabel }}</el-tag>
              <el-tag v-if="lifecycle.stageLabel" :type="lifecycle.color" size="small" effect="dark">{{ lifecycle.stageLabel }}</el-tag>
              <span v-for="t in profile.tags || []" :key="t" class="ctag">{{ t }}</span>
            </div>
            <p>📱 {{ profile.phone }} ｜ 🏠 {{ profile.communityName || '—' }} {{ profile.address || '' }}</p>
            <p class="sub">当前套餐：{{ profile.pkgName || '—' }} ｜ 月费 {{ money(profile.pkgMonthlyFee) }} ｜ 合约：{{ profile.contractEnd || '—' }}（{{ profile.contractStatus || '—' }}）</p>
          </div>
        </div>

        <!-- 汇总指标 -->
        <div class="stats">
          <div class="stat"><span class="num">{{ summary.orderCount ?? 0 }}</span><span class="lab">业务订单</span></div>
          <div class="stat"><span class="num">{{ money(summary.paidAmount) }}</span><span class="lab">累计消费</span></div>
          <div class="stat"><span class="num">{{ summary.workOrderCount ?? 0 }}</span><span class="lab">安装工单</span></div>
          <div class="stat"><span class="num">{{ summary.reviewCount ?? 0 }}</span><span class="lab">评价/投诉</span></div>
          <div class="stat"><span class="num">{{ summary.avgScore ?? 0 }}<i>分</i></span><span class="lab">平均评分</span></div>
          <div class="stat"><span class="num">{{ summary.upgradeCount ?? 0 }}</span><span class="lab">升级申请</span></div>
        </div>

        <!-- 分栏 -->
        <el-tabs v-model="activeTab" class="tabs">
          <el-tab-pane label="档案" name="profile">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="客户ID">{{ profile.id }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ profile.name }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ profile.phone }}</el-descriptions-item>
              <el-descriptions-item label="客户分层">{{ profile.levelLabel }}</el-descriptions-item>
              <el-descriptions-item label="当前套餐">{{ profile.pkgName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="套餐月费">{{ money(profile.pkgMonthlyFee) }}</el-descriptions-item>
              <el-descriptions-item label="所属小区">{{ profile.communityName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="安装地址">{{ profile.address || '—' }}</el-descriptions-item>
              <el-descriptions-item label="账户状态">{{ profile.statusLabel }}</el-descriptions-item>
              <el-descriptions-item label="合约到期">{{ profile.contractEnd || '—' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="业务订单" name="orders">
            <el-table :data="orders" size="small" empty-text="暂无订单">
              <el-table-column prop="id" label="订单号" min-width="160" />
              <el-table-column prop="packageName" label="套餐" min-width="140" />
              <el-table-column label="金额" width="100"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
              <el-table-column prop="orderTypeLabel" label="类型" width="90" />
              <el-table-column prop="statusLabel" label="状态" width="100" />
              <el-table-column prop="createdAt" label="下单时间" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="合约" name="contracts">
            <el-table :data="contracts" size="small" empty-text="暂无合约">
              <el-table-column prop="id" label="合约ID" min-width="140" />
              <el-table-column prop="packageId" label="套餐ID" min-width="120" />
              <el-table-column label="月费" width="100"><template #default="{ row }">{{ money(row.monthlyFee) }}</template></el-table-column>
              <el-table-column prop="startDate" label="生效日" width="120" />
              <el-table-column prop="endDate" label="到期日" width="120" />
              <el-table-column prop="statusLabel" label="状态" width="100" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="流量" name="traffic">
            <div v-if="traffic" class="traffic">
              <div class="tline">
                <span>{{ traffic.mobileUsed }}G / {{ traffic.mobileTotal }}G（手机）｜ 峰值 {{ traffic.broadbandPeak || '—' }} ｜ 当月时长 {{ traffic.broadbandHours || 0 }}h</span>
                <el-tag :type="traffic.mobileUsed >= traffic.mobileTotal ? 'danger' : 'success'" size="small" effect="light">
                  {{ pct(traffic.mobileUsed, traffic.mobileTotal) }}%
                </el-tag>
              </div>
              <el-progress :percentage="pct(traffic.mobileUsed, traffic.mobileTotal)"
                           :color="traffic.mobileUsed >= traffic.mobileTotal ? '#dc2626' : '#4f46e5'" :stroke-width="10" />
              <h4 class="sec">近 7 日趋势（G）</h4>
              <div class="bars">
                <div v-for="(v, i) in (traffic.dailyTrend || [])" :key="i" class="bar">
                  <div class="bcol" :style="{ height: (v / trendMax * 60) + 'px' }"></div>
                  <span class="blab">{{ v }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无流量数据" :image-size="60" />
          </el-tab-pane>

          <el-tab-pane label="安装工单" name="work">
            <el-table :data="workOrders" size="small" empty-text="暂无工单">
              <el-table-column prop="id" label="工单号" min-width="120" />
              <el-table-column prop="statusLabel" label="状态" width="90" />
              <el-table-column prop="timeSlot" label="时段" min-width="130" />
              <el-table-column prop="packageDesc" label="业务" width="90" />
              <el-table-column prop="workerId" label="师傅" width="100" />
              <el-table-column label="测速↓/↑" width="120">
                <template #default="{ row }">{{ row.downSpeed ?? '—' }} / {{ row.upSpeed ?? '—' }}</template>
              </el-table-column>
              <el-table-column prop="completeTime" label="完工时间" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="投诉与评价" name="reviews">
            <el-table :data="reviews" size="small" empty-text="暂无评价">
              <el-table-column prop="typeLabel" label="类型" width="80" />
              <el-table-column label="评分" width="80"><template #default="{ row }">{{ row.score }}★</template></el-table-column>
              <el-table-column label="标签" min-width="160">
                <template #default="{ row }">
                  <el-tag v-for="t in (row.tags || [])" :key="t" size="small" effect="plain" style="margin-right:4px">{{ t }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
              <el-table-column prop="statusLabel" label="状态" width="90" />
              <el-table-column prop="createdAt" label="时间" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="升级申请" name="upgrade">
            <el-table :data="upgradeOrders" size="small" empty-text="暂无升级申请">
              <el-table-column prop="id" label="申请号" min-width="150" />
              <el-table-column prop="fromPackageId" label="原套餐" min-width="120" />
              <el-table-column prop="targetBandKey" label="目标" min-width="120" />
              <el-table-column label="月补差" width="90"><template #default="{ row }">+{{ money(row.monthDiff) }}</template></el-table-column>
              <el-table-column label="一次性补差" width="110"><template #default="{ row }">{{ money(row.oneTimeDiff) }}</template></el-table-column>
              <el-table-column prop="statusLabel" label="状态" width="90" />
              <el-table-column prop="createdAt" label="时间" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="触达记录" name="touch">
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
            <el-empty v-else description="暂无触达记录" :image-size="60" />
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
