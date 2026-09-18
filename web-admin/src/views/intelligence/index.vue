<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import DetailDrawer from '@/components/DetailDrawer.vue'
import {
  intelligenceSegments,
  intelligenceChurn,
  intelligenceCampaigns,
  intelligenceAutoTrigger,
  intelligenceSegmentCustomers
} from '@/api/business'
import { loadResource } from '@/composables/useResource'
import {
  demoIntelligenceSegments,
  demoIntelligenceChurn,
  demoIntelligenceCampaigns,
  demoSegmentCustomers
} from '@/mock/fallback'

const PALETTE = ['#4f46e5', '#22c55e', '#f59e0b', '#ef4444', '#06b6d4', '#a855f7']

const segData = ref(null)
const churnData = ref(null)
const campaigns = ref([])

const segLive = ref(false)
const churnLive = ref(false)
const campLive = ref(false)
const loading = ref(true)

const segSegments = computed(() => segData.value?.segments || [])
const segTotal = computed(() => segData.value?.total || 0)
const churnList = computed(() => churnData.value?.list || [])
const churnTotal = computed(() => churnData.value?.total || 0)

// ---- 分群饼图
const segmentPie = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
  legend: { bottom: 0, type: 'scroll' },
  color: PALETTE,
  series: [{
    type: 'pie',
    radius: ['42%', '70%'],
    center: ['50%', '44%'],
    avoidLabelOverlap: true,
    itemStyle: { borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%' },
    data: segSegments.value.map((x) => ({ name: x.segmentLabel, value: x.count }))
  }]
}))

// ---- 流失风险柱状图（按风险分倒序取前 10）
const churnBar = computed(() => {
  const list = [...churnList.value].sort((a, b) => b.riskScore - a.riskScore).slice(0, 10)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 24, top: 16, bottom: 28 },
    xAxis: { type: 'value', name: '风险分' },
    yAxis: { type: 'category', data: list.map((x) => x.name).reverse() },
    series: [{
      type: 'bar',
      data: list.map((x) => ({
        value: x.riskScore,
        itemStyle: { color: x.riskScore >= 85 ? '#ef4444' : x.riskScore >= 60 ? '#f59e0b' : '#22c55e', borderRadius: [0, 4, 4, 0] }
      })).reverse()
    }]
  }
})

const SEG_COLOR = {
  LEAD: '#94a3b8', GROWING: '#22c55e', STABLE: '#06b6d4',
  HIGH_VALUE: '#4f46e5', RENEW: '#f59e0b', CHURN_RISK: '#ef4444'
}
const riskType = (s) => (s >= 85 ? 'danger' : s >= 60 ? 'warning' : 'success')

// ---- 分群客户下钻（US-2.2）：点击分群饼图 / 流失柱图，查看该分群客户明细
const detail = ref([])
const detailLoading = ref(false)
const detailVisible = ref(false)
const detailTitle = ref('')
const detailColumns = [
  { prop: 'id', label: '客户ID', width: 130 },
  { prop: 'name', label: '姓名', width: 100 },
  { prop: 'levelLabel', label: '等级', width: 90 },
  { prop: 'status', label: '状态', width: 90 },
  { prop: 'orderCnt', label: '订单数', width: 90, align: 'right' },
  { prop: 'contractDaysLeft', label: '合约剩余', width: 110, format: (r) => (r.contractDaysLeft >= 9999 ? '—' : r.contractDaysLeft + ' 天') },
  { prop: 'lastActiveText', label: '最近活跃', width: 130 },
  { prop: 'segmentLabel', label: '分群', width: 110 }
]

const segLabelToCode = computed(() => {
  const m = {}
  ;(segSegments.value || []).forEach((s) => { m[s.segmentLabel] = s.segment })
  return m
})

async function drillBySegment(segment, segmentLabel) {
  if (!segment) return
  detailTitle.value = `分群客户明细 · ${segmentLabel || segment}`
  detailLoading.value = true
  detailVisible.value = true
  const r = await loadResource(() => intelligenceSegmentCustomers(segment), () => demoSegmentCustomers(segment))
  const d = r.data || {}
  detail.value = d.rows || []
  detailLoading.value = false
}

function onSegmentPieClick(params) {
  drillBySegment(segLabelToCode.value[params?.name], params?.name)
}

function onChurnBarClick(params) {
  const cust = churnList.value.find((c) => c.name === params?.name)
  if (cust) drillBySegment(cust.segment, cust.segmentLabel)
}

// ---- 营销自动化一键触发
const triggering = ref(false)
const triggerResult = ref(null)
const triggerDry = ref(false)

async function onTrigger() {
  triggering.value = true
  triggerResult.value = null
  try {
    const r = await intelligenceAutoTrigger(triggerDry.value)
    triggerResult.value = r
    if (triggerDry.value) {
      ElMessage.info(`演练：将命中 ${r.matched} 人，其中发券 ${r.granted}、推送 ${r.pushed}，跳过 ${r.skipped}`)
    } else {
      ElMessage.success(`触发完成：命中 ${r.matched} 人，发券 ${r.granted}，推送 ${r.pushed}，跳过 ${r.skipped}`)
      await loadCampaigns()
    }
  } catch (e) {
    // 后端不可达：演示结果
    triggerResult.value = {
      dryRun: triggerDry.value, matched: 6, granted: 2, pushed: 4, skipped: 1,
      details: [
        { campaignId: 'MK_CHURN', name: '流失预警挽留', trigger: 'CHURN_RISK', matched: 0, acted: 0, skipped: 0 },
        { campaignId: 'MK_RENEW', name: '临期客户续约推送', trigger: 'RENEW', matched: 3, acted: 3, skipped: 1 },
        { campaignId: 'MK_VIP', name: '高价值客户专属提速', trigger: 'HIGH_VALUE', matched: 3, acted: 2, skipped: 0 }
      ]
    }
    ElMessage.warning('后端不可达，已展示演示触发结果')
  } finally {
    triggering.value = false
  }
}

async function loadSegments() {
  const r = await loadResource(() => intelligenceSegments(), () => demoIntelligenceSegments())
  segData.value = r.data
  segLive.value = r.live
}
async function loadChurn() {
  const r = await loadResource(() => intelligenceChurn(50), () => demoIntelligenceChurn())
  churnData.value = r.data
  churnLive.value = r.live
}
async function loadCampaigns() {
  const r = await loadResource(() => intelligenceCampaigns(), () => demoIntelligenceCampaigns())
  campaigns.value = r.data
  campLive.value = r.live
}

onMounted(async () => {
  loading.value = true
  await Promise.all([loadSegments(), loadChurn(), loadCampaigns()])
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">数据智能 · 客户分群 / 流失预警 / 营销自动化</h2>
        <p class="page-sub">基于真实业务数据自动派生客户分层、识别高危流失客户，并以规则引擎驱动精准营销触达</p>
      </div>
      <SourceTag :live="segLive && churnLive && campLive" />
    </div>

    <!-- 一、客户分群 -->
    <div class="card">
      <div class="card-head">
        <h3>一、客户分群<span class="hint">点击分群下钻客户明细</span></h3>
        <SourceTag :live="segLive" />
      </div>
      <div class="seg-body">
        <div class="seg-chart">
          <ChartBox :option="segmentPie" height="320px" @chart-click="onSegmentPieClick" />
        </div>
        <div class="seg-side">
          <div class="mini-stats">
            <StatCard label="在册客户" :value="segTotal" foot="全量分层基数" />
            <StatCard label="分群数" :value="segSegments.length" unit="类" tone="down" foot="6 大生命周期分群" />
          </div>
          <el-table :data="segSegments" size="small" style="width: 100%">
            <el-table-column prop="segmentLabel" label="分群" min-width="120" />
            <el-table-column label="人数" width="90" align="right">
              <template #default="{ row }"><b>{{ row.count }}</b></template>
            </el-table-column>
            <el-table-column label="占比" min-width="160">
              <template #default="{ row }">
                <el-progress
                  :percentage="segTotal ? Math.round((row.count / segTotal) * 100) : 0"
                  :color="SEG_COLOR[row.segment]" :stroke-width="14" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 二、流失预警 -->
    <div class="card">
      <div class="card-head">
        <h3>二、流失预警（高危客户 Top 10）</h3>
        <SourceTag :live="churnLive" />
      </div>
      <div class="churn-body">
        <div class="churn-chart">
          <ChartBox :option="churnBar" height="320px" @chart-click="onChurnBarClick" />
        </div>
        <div class="churn-table">
          <el-table v-loading="loading" :data="churnList" size="small" style="width: 100%" max-height="360">
            <el-table-column prop="name" label="客户" width="90" />
            <el-table-column label="等级" width="70">
              <template #default="{ row }">{{ row.levelLabel }}</template>
            </el-table-column>
            <el-table-column label="分群" width="100">
              <template #default="{ row }">
                <el-tag :color="SEG_COLOR[row.segment]" size="small" effect="dark" style="border:none;color:#fff">
                  {{ row.segmentLabel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险分" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="riskType(row.riskScore)" size="small" effect="light">{{ row.riskScore }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="合约剩余" width="90">
              <template #default="{ row }">{{ row.contractDaysLeft >= 9999 ? '—' : row.contractDaysLeft + ' 天' }}</template>
            </el-table-column>
            <el-table-column label="归因" min-width="180">
              <template #default="{ row }">
                <span class="reasons">{{ (row.reasons || []).join('；') }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <p class="hint-line">共识别 <b>{{ churnTotal }}</b> 位需重点跟进客户（流失预警 + 临期待续约）。</p>
    </div>

    <DetailDrawer
      v-model:visible="detailVisible"
      :title="detailTitle"
      :columns="detailColumns"
      :rows="detail"
      :loading="detailLoading"
      :live="segLive"
    />

    <!-- 三、营销自动化 -->
    <div class="card">
      <div class="card-head">
        <h3>三、营销自动化规则引擎</h3>
        <div class="head-actions">
          <el-switch v-model="triggerDry" active-text="演练" inactive-text="正式" inline-prompt />
          <el-button type="primary" :loading="triggering" @click="onTrigger">
            {{ triggerDry ? '演练触发' : '一键触发' }}
          </el-button>
          <SourceTag :live="campLive" />
        </div>
      </div>
      <el-table :data="campaigns" size="small" style="width: 100%">
        <el-table-column prop="name" label="规则" min-width="170" />
        <el-table-column label="触发分群" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.triggerLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="动作" width="100">
          <template #default="{ row }">
            <el-tag :type="row.actionType === 'GRANT_COUPON' ? 'warning' : 'success'" size="small" effect="light">
              {{ row.actionLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="累计 / 近 30 天" width="130" align="center">
          <template #default="{ row }"><b>{{ row.execTotal || 0 }}</b> / {{ row.execRecent || 0 }}</template>
        </el-table-column>
      </el-table>

      <el-collapse v-if="triggerResult" class="trig-result">
        <el-collapse-item title="本次触发结果" name="1">
          <div class="trig-summary">
            <span>命中 <b>{{ triggerResult.matched }}</b> 人</span>
            <span>发券 <b class="up">{{ triggerResult.granted }}</b></span>
            <span>推送 <b class="up">{{ triggerResult.pushed }}</b></span>
            <span>跳过（已触达） <b class="mute">{{ triggerResult.skipped }}</b></span>
            <el-tag v-if="triggerResult.dryRun" size="small" type="info" effect="plain">演练模式</el-tag>
          </div>
          <el-table :data="triggerResult.details || []" size="small" style="width: 100%">
            <el-table-column prop="name" label="规则" min-width="170" />
            <el-table-column prop="trigger" label="触发分群" width="130" />
            <el-table-column label="命中" width="80" align="center">
              <template #default="{ row }">{{ row.matched }}</template>
            </el-table-column>
            <el-table-column label="已执行" width="80" align="center">
              <template #default="{ row }"><b class="up">{{ row.acted }}</b></template>
            </el-table-column>
            <el-table-column label="跳过" width="80" align="center">
              <template #default="{ row }"><b class="mute">{{ row.skipped }}</b></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </div>
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
.card { margin-bottom: 16px; }
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.head-actions { display: flex; align-items: center; gap: 10px; }

.seg-body, .churn-body {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 18px;
  align-items: start;
}
.seg-side .mini-stats { display: flex; gap: 12px; margin-bottom: 12px; }
.seg-side .mini-stats > * { flex: 1; }

.reasons { font-size: 12px; color: var(--bd-text-mute); }
.hint { font-size: 12px; font-weight: 400; color: var(--bd-text-mute); margin-left: 8px; }
.hint-line { margin: 10px 2px 0; font-size: 13px; color: var(--bd-text-mute); }

.trig-result { margin-top: 14px; }
.trig-summary { display: flex; flex-wrap: wrap; gap: 18px; margin-bottom: 10px; font-size: 14px; }
.trig-summary .up { color: #22c55e; }
.trig-summary .mute { color: var(--bd-text-mute); }

@media (max-width: 1100px) {
  .seg-body, .churn-body { grid-template-columns: 1fr; }
}
</style>
