<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import { slaDashboard, slaRules, slaCompensations, slaEvaluate, slaOvertimeDetail } from '@/api/business'
import { loadResource, money, fmtTime } from '@/composables/useResource'
import { demoSlaDashboard, demoSlaOvertimeDetail } from '@/mock/fallback'

const ORDER_TYPE = { NEW_INSTALL: '新装宽带', MOVE: '宽带移机', REPAIR: '故障报修', SPEED_UP: '宽带提速', RENEW: '续费' }
const COMP_TYPE = { VOUCHER: '流量券/电子券', CASH: '现金/话费', FEE_WAIVE: '费用减免' }
const EVAL_TYPE = { TIME: '时限类', SPEED: '速率类' }
const COMP_UNIT = { PER_ORDER: '每单固定', PER_OVERTIME_HOUR: '每超时小时' }
const COMP_STATUS = { PENDING: '待处理', VERIFYING: '核实中', PAID: '已赔付', REJECTED: '已驳回' }

const tab = ref('overview')

const dashboard = ref(null)
const dashboardLive = ref(false)
const rules = ref([])
const rulesLive = ref(false)
const comps = ref([])
const compsLive = ref(false)
const loading = ref(true)

const form = reactive({
  orderId: 'WO-SLA-001',
  orderType: 'NEW_INSTALL',
  custName: '演示客户',
  acceptHoursAgo: 30,
  completeHoursAgo: 0,
  speedTestMbps: 820
})
const evaluating = ref(false)
const evalResult = ref(null)

// ---- 超时热力下钻
const overtimeDetail = ref([])
const overtimeDetailLoading = ref(false)
const overtimeDetailVisible = ref(false)
const overtimeDetailTitle = ref('')
async function onHeatmapClick(params) {
  if (!params || params.seriesType !== 'heatmap') return
  const d = params.data[1]
  const h = params.data[0]
  const v = params.data[2]
  const days = (dashboard.value && dashboard.value.heatmap && dashboard.value.heatmap.days) || []
  overtimeDetailTitle.value = `${days[d] || ''} ${String(h).padStart(2, '0')}:00 超时明细（${v} 单）`
  overtimeDetailLoading.value = true
  overtimeDetailVisible.value = true
  const r = await loadResource(() => slaOvertimeDetail(d + 1, h), () => demoSlaOvertimeDetail(d + 1, h))
  overtimeDetail.value = r.data || []
  overtimeDetailLoading.value = false
}

const summary = computed(() => dashboard.value?.summary || {})
const recent = computed(() => (dashboard.value && dashboard.value.recentCompensations) || comps.value.slice(0, 6))

// ---- 图表配置
const byTypeOption = computed(() => {
  const list = dashboard.value?.byType || []
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v) => v + '%' },
    grid: { left: 44, right: 16, top: 16, bottom: 28 },
    xAxis: { type: 'category', data: list.map((x) => x.orderTypeLabel), axisLabel: { interval: 0 } },
    yAxis: { type: 'value', max: 100, name: '%', axisLabel: { formatter: '{value}' } },
    series: [{
      type: 'bar',
      data: list.map((x) => x.slaRate),
      itemStyle: { color: '#4f46e5', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', formatter: '{c}%' }
    }]
  }
})

const overtimeOption = computed(() => {
  const list = dashboard.value?.overtimeByDay || []
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['达标', '超时'] },
    grid: { left: 40, right: 16, top: 28, bottom: 24 },
    xAxis: { type: 'category', data: list.map((x) => x.date) },
    yAxis: { type: 'value' },
    series: [
      { name: '达标', type: 'bar', stack: 't', data: list.map((x) => x.met), itemStyle: { color: '#22c55e' } },
      { name: '超时', type: 'bar', stack: 't', data: list.map((x) => x.overtime), itemStyle: { color: '#ef4444' } }
    ]
  }
})

const compTrendOption = computed(() => {
  const list = dashboard.value?.compTrend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['赔付金额(元)', '赔付单数'] },
    grid: { left: 50, right: 50, top: 28, bottom: 24 },
    xAxis: { type: 'category', data: list.map((x) => x.month) },
    yAxis: [
      { type: 'value', name: '元' },
      { type: 'value', name: '单' }
    ],
    series: [
      { name: '赔付金额(元)', type: 'line', smooth: true, data: list.map((x) => x.compAmount), itemStyle: { color: '#f59e0b' }, areaStyle: { opacity: 0.12 } },
      { name: '赔付单数', type: 'bar', yAxisIndex: 1, data: list.map((x) => x.compCount), itemStyle: { color: '#4f46e5', borderRadius: [4, 4, 0, 0] } }
    ]
  }
})

// ---- 超时热力：星期 × 时段（0~23 时）的超时工单分布 ----
const heatmapOption = computed(() => {
  const hm = dashboard.value?.heatmap
  if (!hm || !Array.isArray(hm.values)) return { series: [] }
  const days = hm.days || []
  const hours = hm.hours || []
  const values = hm.values
  const data = []
  let max = 0
  for (let d = 0; d < days.length; d++) {
    for (let h = 0; h < hours.length; h++) {
      const v = (values[d] && values[d][h]) || 0
      if (v > max) max = v
      data.push([h, d, v])
    }
  }
  return {
    tooltip: {
      position: 'top',
      formatter: (p) => `${days[p.data[1]]} ${String(hours[p.data[0]]).padStart(2, '0')}:00 超时 ${p.data[2]} 单`
    },
    grid: { left: 48, right: 16, top: 12, bottom: 58 },
    xAxis: { type: 'category', data: hours.map((h) => String(h).padStart(2, '0')), splitArea: { show: true }, axisLabel: { interval: 2 } },
    yAxis: { type: 'category', data: days, splitArea: { show: true } },
    visualMap: {
      min: 0, max: Math.max(max, 1), calculable: true, orient: 'horizontal',
      left: 'center', bottom: 0, show: false,
      inRange: { color: ['#eef2ff', '#a5b4fc', '#4f46e5', '#dc2626'] }
    },
    series: [{
      name: '超时', type: 'heatmap', data,
      label: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      emphasis: { itemStyle: { shadowBlur: 6, shadowColor: 'rgba(0,0,0,0.3)' } }
    }]
  }
})

async function onEvaluate() {
  evaluating.value = true
  const now = Date.now()
  const payload = {
    orderId: form.orderId,
    orderType: form.orderType,
    custName: form.custName,
    acceptTime: now - Number(form.acceptHoursAgo) * 3600 * 1000,
    completeTime: now - Number(form.completeHoursAgo) * 3600 * 1000,
    speedTestMbps: Number(form.speedTestMbps)
  }
  const r = await loadResource(() => slaEvaluate(payload), null)
  evaluating.value = false
  if (r.live) {
    evalResult.value = r.data
    ElMessage.success('评估完成（已落库 sla_record / compensation）')
    loadDashboard()
    loadComps()
  } else {
    evalResult.value = null
    ElMessage.error('评估接口不可达')
  }
}

async function loadDashboard() {
  const r = await loadResource(() => slaDashboard(), () => demoSlaDashboard())
  dashboard.value = r.data
  dashboardLive.value = r.live
}

async function loadRules() {
  const r = await loadResource(() => slaRules(), [])
  rules.value = Array.isArray(r.data) ? r.data : []
  rulesLive.value = r.live
}

async function loadComps() {
  const r = await loadResource(() => slaCompensations(), [])
  comps.value = Array.isArray(r.data) ? r.data : []
  compsLive.value = r.live
}

onMounted(async () => {
  loading.value = true
  await Promise.all([loadDashboard(), loadRules(), loadComps()])
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">装维 SLA 与赔付</h2>
        <p class="page-sub">
          当日装 / 当日修时限承诺 + 装机网速达标校验，超时或不达标自动触发赔付
        </p>
      </div>
      <SourceTag :live="dashboardLive" />
    </div>

    <el-tabs v-model="tab">
      <!-- 履约看板 -->
      <el-tab-pane label="履约看板" name="overview">
        <div class="stat-grid">
          <StatCard label="SLA 达标率" :value="summary.slaRate ?? 0" unit="%" tone="down" foot="承诺时限内完成" />
          <StatCard label="评估总数" :value="summary.total ?? 0" foot="累计评估工单" />
          <StatCard label="超时工单" :value="summary.overtime ?? 0" tone="warn" foot="触发赔付" />
          <StatCard label="待处理赔付" :value="summary.pendingCompCount ?? 0" tone="warn" foot="核实 / 待赔付" />
          <StatCard label="平均响应" :value="summary.avgResponseMin ?? 0" unit=" min" foot="受理 → 完工" />
          <StatCard label="累计赔付" :value="money(summary.totalCompAmount)" tone="up" foot="流量券 + 话费" />
        </div>

        <div class="charts">
          <div class="card chart-card">
            <div class="card-head"><h3>分业务类型达标率</h3></div>
            <ChartBox :option="byTypeOption" height="280px" />
          </div>
          <div class="card chart-card">
            <div class="card-head"><h3>近 14 日 超时 vs 达标</h3></div>
            <ChartBox :option="overtimeOption" height="280px" />
          </div>
          <div class="card chart-card span2">
            <div class="card-head"><h3>超时热力（星期 × 时段）<span class="hint">点击单元格下钻明细</span></h3></div>
            <ChartBox :option="heatmapOption" height="300px" @chart-click="onHeatmapClick" />
          </div>
          <div class="card chart-card span2">
            <div class="card-head"><h3>近 6 月 赔付趋势</h3></div>
            <ChartBox :option="compTrendOption" height="280px" />
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <h3>最近赔付工单</h3>
            <SourceTag :live="dashboardLive || compsLive" />
          </div>
          <el-table v-loading="loading" :data="recent" style="width: 100%">
            <el-table-column prop="orderId" label="工单号" min-width="130" />
            <el-table-column prop="custName" label="客户" width="110" />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">{{ ORDER_TYPE[row.orderType] || row.orderType || '—' }}</template>
            </el-table-column>
            <el-table-column label="赔付形式" width="130">
              <template #default="{ row }">{{ COMP_TYPE[row.compType] || row.compType || '—' }}</template>
            </el-table-column>
            <el-table-column label="赔付额度" width="110">
              <template #default="{ row }"><b class="up">{{ row.compAmount }}</b></template>
            </el-table-column>
            <el-table-column prop="reason" label="触发原因" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PAID' ? 'success' : 'warning'" size="small" effect="light">
                  {{ COMP_STATUS[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ fmtTime(row.createdTime) }}</template>
            </el-table-column>
            <template #empty><EmptyState icon="🛡" title="暂无赔付工单" desc="可到「评估工具」发起一次超时评估" /></template>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 赔付规则 -->
      <el-tab-pane label="赔付规则" name="rules">
        <div class="card">
          <div class="card-head">
            <h3>SLA 规则配置</h3>
            <SourceTag :live="rulesLive" />
          </div>
          <el-table :data="rules" style="width: 100%">
            <el-table-column prop="slaName" label="规则名" min-width="160" />
            <el-table-column label="业务类型" width="120">
              <template #default="{ row }">{{ ORDER_TYPE[row.orderType] || row.orderType }}</template>
            </el-table-column>
            <el-table-column label="评估方式" width="100">
              <template #default="{ row }">{{ EVAL_TYPE[row.evalType] || row.evalType }}</template>
            </el-table-column>
            <el-table-column label="承诺 / 达标线" width="140">
              <template #default="{ row }">
                <span v-if="row.evalType === 'TIME'">{{ row.promisedHours }} 小时（宽限 {{ row.graceMinutes }} 分）</span>
                <span v-else>≥ {{ row.minSpeedMbps }} Mbps</span>
              </template>
            </el-table-column>
            <el-table-column label="赔付" width="160">
              <template #default="{ row }">
                {{ row.compAmount }}
                <span class="mute">{{ COMP_TYPE[row.compType] }}</span>
                <br />
                <span class="mute">{{ COMP_UNIT[row.compUnit] }}</span>
              </template>
            </el-table-column>
            <el-table-column label="封顶" width="100">
              <template #default="{ row }">{{ row.maxCompAmount ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="启用" width="80">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="light">
                  {{ row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <template #empty><EmptyState icon="⚙️" title="暂无规则" desc="配置 SLA 赔付规则与触发阈值" /></template>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 评估工具 -->
      <el-tab-pane label="评估工具" name="eval">
        <div class="cols">
          <div class="card">
            <div class="card-head"><h3>发起 SLA 评估</h3><span class="hint">POST /api/sla/evaluate</span></div>
            <div class="card-body">
              <el-form label-width="110px" label-position="left">
                <el-form-item label="工单号">
                  <el-input v-model="form.orderId" />
                </el-form-item>
                <el-form-item label="业务类型">
                  <el-select v-model="form.orderType" style="width: 100%">
                    <el-option v-for="(v, k) in ORDER_TYPE" :key="k" :label="v" :value="k" />
                  </el-select>
                </el-form-item>
                <el-form-item label="客户">
                  <el-input v-model="form.custName" />
                </el-form-item>
                <el-form-item label="受理时间">
                  <el-input-number v-model="form.acceptHoursAgo" :min="0" :max="240" />
                  <span class="tip">小时前</span>
                </el-form-item>
                <el-form-item label="完工时间">
                  <el-input-number v-model="form.completeHoursAgo" :min="0" :max="240" />
                  <span class="tip">小时前（0=刚刚）</span>
                </el-form-item>
                <el-form-item label="装机测速">
                  <el-input-number v-model="form.speedTestMbps" :min="0" :max="5000" :step="10" />
                  <span class="tip">Mbps（速率类规则用）</span>
                </el-form-item>
                <el-button type="primary" :loading="evaluating" @click="onEvaluate">执行评估</el-button>
              </el-form>
            </div>
          </div>

          <div class="card">
            <div class="card-head"><h3>评估结果</h3></div>
            <div class="card-body">
              <el-empty v-if="!evalResult" description="填写左侧参数后执行评估" :image-size="70" />
              <template v-else>
                <p class="summary">{{ evalResult.summary }}</p>
                <el-descriptions :column="1" border size="small" style="margin-top: 12px">
                  <el-descriptions-item label="记录ID">{{ evalResult.record?.id }}</el-descriptions-item>
                  <el-descriptions-item label="判定">
                    <el-tag
                      size="small"
                      effect="light"
                      :type="evalResult.record?.slaStatus === 'MET' ? 'success' : 'danger'"
                    >
                      {{ evalResult.record?.slaStatus === 'MET' ? '达标' : '超时' }}
                    </el-tag>
                    <span v-if="evalResult.record?.overtimeMinutes" style="margin-left: 8px">
                      超时 {{ evalResult.record.overtimeMinutes }} 分钟
                    </span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="evalResult.compensation" label="赔付">
                    <b class="up">{{ evalResult.compensation.compAmount }}</b>
                    {{ COMP_TYPE[evalResult.compensation.compType] }}
                    <br />
                    {{ evalResult.compensation.reason }}
                  </el-descriptions-item>
                </el-descriptions>
              </template>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="overtimeDetailVisible" :title="overtimeDetailTitle" size="48%" :append-to-body="true">
      <el-table v-loading="overtimeDetailLoading" :data="overtimeDetail" empty-text="该时段无超时工单">
        <el-table-column prop="orderId" label="工单号" min-width="140" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ row.orderTypeLabel || row.orderType || '—' }}</template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" width="100" />
        <el-table-column prop="community" label="小区" min-width="120" />
        <el-table-column label="受理时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.acceptTime) }}</template>
        </el-table-column>
        <el-table-column label="完工时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.completeTime) }}</template>
        </el-table-column>
      </el-table>
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
  margin: 0 0 12px;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
}

.charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.span2 { grid-column: span 2; }

.cols {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 16px;
  align-items: start;
}

.tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--bd-text-mute);
}

.mute {
  font-size: 12px;
  color: var(--bd-text-mute);
}

.up {
  color: var(--bd-up);
}

.hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--bd-text-mute);
  margin-left: 8px;
}

.summary {
  margin: 0;
  padding: 10px 12px;
  background: var(--bd-primary-light);
  border-radius: 8px;
  font-size: 13px;
  color: #3730a3;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .charts { grid-template-columns: 1fr; }
  .span2 { grid-column: span 1; }
  .cols { grid-template-columns: 1fr; }
}
</style>
