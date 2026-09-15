<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import ChartBox from '@/components/ChartBox.vue'
import { adminTrafficOverview, trafficUsage } from '@/api/business'
import { loadResource, pct } from '@/composables/useResource'
import { demoTrafficOverview } from '@/mock/fallback'

const overview = ref({ ...demoTrafficOverview })
const live = ref(false)
const loading = ref(true)

const customerId = ref('demo')
const usage = ref(null)
const usageLoading = ref(false)
const usageLive = ref(false)

const trendOption = computed(() => {
  const t = (usage.value && usage.value.trend) || []
  return {
    grid: { left: 8, right: 12, top: 22, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['D-6', 'D-5', 'D-4', 'D-3', 'D-2', 'D-1', '今日'],
      axisLabel: { color: '#6b7280', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: 'GB',
      nameTextStyle: { color: '#9ca3af', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#9ca3af', fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 22,
        data: t.map((v, i) => ({
          value: v,
          itemStyle: { color: i === t.length - 1 ? '#dc2626' : '#4f46e5', borderRadius: [4, 4, 0, 0] }
        }))
      }
    ]
  }
})

async function loadUsage() {
  if (!customerId.value) return ElMessage.warning('请输入客户ID')
  usageLoading.value = true
  const r = await loadResource(() => trafficUsage(customerId.value), null)
  usage.value = r.data
  usageLive.value = r.live
  usageLoading.value = false
  if (!r.live) ElMessage.warning('流量接口不可达（请确认 customerId 存在且后端在线）')
}

onMounted(async () => {
  const r = await loadResource(() => adminTrafficOverview(), demoTrafficOverview)
  overview.value = { ...demoTrafficOverview, ...(r.data || {}) }
  live.value = r.live
  loading.value = false
  await loadUsage()
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">流量监控</h2>
        <p class="page-sub">
          手机流量池与宽带时长监控；阈值规则：剩余 ≤ 5G 触发预警并推荐加购流量包
        </p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat"><div class="label">活跃客户</div><div class="value">{{ overview.activeCustomers }}<small> 户</small></div></div>
      <div class="stat"><div class="label">本月流量池</div><div class="value">{{ overview.monthPool }}</div></div>
      <div class="stat is-warn"><div class="label">超额客户</div><div class="value">{{ overview.overCustomers }}<small> 户</small></div></div>
      <div class="stat is-up"><div class="label">告警中</div><div class="value">{{ overview.alarming }}<small> 户</small></div></div>
    </div>

    <div class="cols">
      <div class="card">
        <div class="card-head">
          <h3>客户用量明细（Top）</h3>
          <SourceTag :live="live" />
        </div>
        <el-table v-loading="loading" :data="overview.top || []" style="width: 100%">
          <el-table-column prop="customer" label="客户" width="110" />
          <el-table-column prop="pkg" label="套餐" min-width="160" />
          <el-table-column label="本月用量" width="130">
            <template #default="{ row }">
              {{ row.used }}G / {{ row.total }}G
            </template>
          </el-table-column>
          <el-table-column label="使用率" min-width="180">
            <template #default="{ row }">
              <el-progress
                :percentage="pct(row.used, row.total)"
                :stroke-width="10"
                :color="pct(row.used, row.total) >= 90 ? '#dc2626' : '#4f46e5'"
              />
            </template>
          </el-table-column>
          <el-table-column label="剩余" width="90">
            <template #default="{ row }">{{ Math.max(0, row.total - row.used) }}G</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === '预警' ? 'danger' : 'success'" size="small" effect="light">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <template #empty><EmptyState icon="📶" title="暂无流量数据" desc="后端接通后将展示带宽与并发流量监控" /></template>
        </el-table>
      </div>

      <div>
        <div class="card">
          <div class="card-head">
            <h3>单客户流量查询</h3>
            <span class="hint">GET /api/traffic/usage</span>
          </div>
          <div class="card-body">
            <div class="qrow">
              <el-input v-model="customerId" placeholder="客户ID（示例 demo）" />
              <el-button type="primary" :loading="usageLoading" @click="loadUsage">查询</el-button>
            </div>

            <template v-if="usage">
              <div class="uhead">
                <b>{{ usage.pkgName }}</b>
                <SourceTag :live="usageLive" />
              </div>
              <div class="uline">
                <span>手机流量 {{ usage.mobileUsed }}G / {{ usage.mobileTotal }}G</span>
                <el-tag :type="usage.warn ? 'danger' : 'success'" size="small" effect="light">
                  {{ usage.warn ? '预警' : '正常' }}
                </el-tag>
              </div>
              <el-progress
                :percentage="usage.pct"
                :stroke-width="12"
                :color="usage.warn ? '#dc2626' : '#4f46e5'"
              />
              <p class="hint-line">
                剩余 {{ usage.remaining }}G ｜ 宽带 {{ usage.broadbandHours }} 小时 ｜ 高峰时段
                {{ usage.broadbandPeak }}
              </p>
              <ChartBox :option="trendOption" height="180px" />
            </template>
            <el-empty v-else description="暂无数据" :image-size="60" />
          </div>
        </div>

        <div class="card">
          <div class="card-head"><h3>阈值规则</h3></div>
          <div class="card-body">
            <p class="rule"><b>预警阈值</b> 剩余流量 ≤ 5G</p>
            <p class="rule"><b>触发动作</b> 客户端流量页显红 + 推荐加购流量包</p>
            <p class="rule"><b>口径</b> remaining = total − used；pct = used / total</p>
            <p class="rule"><b>趋势</b> 近 7 日按日聚合 daily_trend</p>
          </div>
        </div>
      </div>
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

.stat-grid {
  margin: 0 0 16px;
}

.cols {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 16px;
  align-items: start;
}

.qrow {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.uhead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.uline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}

.hint-line {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--bd-text-sub);
}

.rule {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--bd-text-sub);
}

.rule b {
  color: var(--bd-text);
  margin-right: 6px;
}

@media (max-width: 1100px) {
  .cols {
    grid-template-columns: 1fr;
  }
}
</style>
