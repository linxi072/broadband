<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import ChartBox from '@/components/ChartBox.vue'
import { monitorOverview } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoNodes, demoSlowApis } from '@/mock/fallback'

const tab = ref('app')
const data = ref(null)
const live = ref(false)
const loading = ref(true)

const app = computed(
  () =>
    (data.value && data.value.app) || {
      status: '健康',
      cpu: 38,
      mem: 71,
      rt: 126,
      errorRate: 0.02,
      qps: 842,
      uptimeMin: 0
    }
)

const jvm = computed(
  () =>
    (data.value && data.value.jvm) || {
      heapUsedMB: 312,
      heapMaxMB: 2048,
      nonHeapMB: 96,
      threads: 42,
      peakThreads: 58,
      gcCount: 12,
      gcTimeMs: 340,
      javaVersion: '21',
      osName: 'macOS',
      availableProcessors: 8
    }
)

const db = computed(
  () => (data.value && data.value.db) || { driver: 'HikariCP', active: 1, idle: 9, total: 10, max: 10, queryAvgMs: 8 }
)

const endpoints = computed(() => (data.value && data.value.endpoints) || [])
const slowApis = computed(() => (data.value && data.value.slowApis) || demoSlowApis)
const nodes = computed(() => (data.value && data.value.nodes) || demoNodes)

const qpsOption = computed(() => {
  const e = endpoints.value.length ? endpoints.value : slowApis.value.map((s) => ({ path: s.api, avgMs: s.avg, count: s.calls }))
  return {
    grid: { left: 8, right: 16, top: 24, bottom: 40, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: e.map((x) => String(x.path).replace(/^(GET|POST|PUT|DELETE)\s/, '')),
      axisLabel: { color: '#6b7280', fontSize: 10, rotate: 24, interval: 0 }
    },
    yAxis: [
      { type: 'value', name: '调用', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#9ca3af', fontSize: 11 } },
      { type: 'value', name: 'ms', splitLine: { show: false }, axisLabel: { color: '#9ca3af', fontSize: 11 } }
    ],
    series: [
      { name: '调用次数', type: 'bar', barWidth: 18, data: e.map((x) => x.count), itemStyle: { color: '#4f46e5', borderRadius: [4, 4, 0, 0] } },
      { name: '平均耗时', type: 'line', yAxisIndex: 1, smooth: true, data: e.map((x) => x.avgMs), itemStyle: { color: '#f59e0b' } }
    ]
  }
})

const heapPct = computed(() => Math.round((jvm.value.heapUsedMB / Math.max(1, jvm.value.heapMaxMB)) * 100))
const slowType = (s) => ({ 正常: 'success', 偏慢: 'warning', 超时: 'danger' }[s] || 'info')

onMounted(async () => {
  const r = await loadResource(() => monitorOverview(), null)
  data.value = r.data
  live.value = r.live
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">性能监控</h2>
        <p class="page-sub">应用/JVM/数据库运行时指标与接口调用统计（由后端 Actuator 口径自采）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div class="stat is-down"><div class="label">应用状态</div><div class="value">{{ app.status }}</div></div>
      <div class="stat" :class="{ 'is-warn': app.cpu > 70 }"><div class="label">CPU</div><div class="value">{{ app.cpu }}<small> %</small></div></div>
      <div class="stat" :class="{ 'is-warn': app.mem > 80 }"><div class="label">内存</div><div class="value">{{ app.mem }}<small> %</small></div></div>
      <div class="stat"><div class="label">平均响应</div><div class="value">{{ app.rt }}<small> ms</small></div></div>
      <div class="stat"><div class="label">错误率</div><div class="value">{{ app.errorRate }}<small> %</small></div></div>
      <div class="stat"><div class="label">QPS</div><div class="value">{{ app.qps }}</div></div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="应用概览" name="app">
        <div class="card">
          <div class="card-head"><h3>接口调用 / 响应时间</h3><SourceTag :live="live" /></div>
          <div class="card-body"><ChartBox :option="qpsOption" height="280px" /></div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="接口监控" name="api">
        <div class="card">
          <div class="card-head">
            <h3>接口调用统计</h3>
            <span class="hint">运行时计数（内存态，重启归零）</span>
          </div>
          <el-table v-loading="loading" :data="endpoints" style="width: 100%">
            <el-table-column prop="path" label="接口" min-width="240" />
            <el-table-column prop="count" label="调用次数" width="120" />
            <el-table-column prop="avgMs" label="平均耗时(ms)" width="140" />
            <el-table-column prop="maxMs" label="峰值(ms)" width="120" />
            <template #empty>暂无调用记录</template>
          </el-table>
        </div>

        <div class="card">
          <div class="card-head"><h3>慢接口 TOP5</h3><SourceTag :live="live" /></div>
          <el-table :data="slowApis" size="small">
            <el-table-column prop="api" label="接口" min-width="240" />
            <el-table-column prop="calls" label="调用次数" width="120" />
            <el-table-column prop="avg" label="平均耗时" width="130">
              <template #default="{ row }">{{ row.avg }} ms</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="slowType(row.status)" size="small" effect="light">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="JVM / 数据库" name="jvm">
        <div class="cols">
          <div class="card">
            <div class="card-head"><h3>JVM</h3></div>
            <div class="card-body">
              <p class="row"><span>堆内存</span><b>{{ jvm.heapUsedMB }} / {{ jvm.heapMaxMB }} MB</b></p>
              <el-progress :percentage="heapPct" :stroke-width="12" :color="heapPct > 80 ? '#dc2626' : '#4f46e5'" />
              <el-descriptions :column="2" border size="small" style="margin-top: 14px">
                <el-descriptions-item label="非堆">{{ jvm.nonHeapMB }} MB</el-descriptions-item>
                <el-descriptions-item label="线程数">{{ jvm.threads }}（峰值 {{ jvm.peakThreads }}）</el-descriptions-item>
                <el-descriptions-item label="GC 次数">{{ jvm.gcCount }}</el-descriptions-item>
                <el-descriptions-item label="GC 耗时">{{ jvm.gcTimeMs }} ms</el-descriptions-item>
                <el-descriptions-item label="JDK">{{ jvm.javaVersion }}</el-descriptions-item>
                <el-descriptions-item label="CPU 核数">{{ jvm.availableProcessors }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </div>

          <div class="card">
            <div class="card-head"><h3>数据库连接池</h3></div>
            <div class="card-body">
              <p class="row"><span>连接池</span><b>{{ db.driver }}</b></p>
              <p class="row"><span>活跃连接</span><b>{{ db.active }} / {{ db.max }}</b></p>
              <el-progress :percentage="Math.round((db.active / Math.max(1, db.max)) * 100)" :stroke-width="12" color="#10b981" />
              <el-descriptions :column="2" border size="small" style="margin-top: 14px">
                <el-descriptions-item label="空闲">{{ db.idle }}</el-descriptions-item>
                <el-descriptions-item label="总数">{{ db.total }}</el-descriptions-item>
                <el-descriptions-item label="平均往返">{{ db.queryAvgMs }} ms</el-descriptions-item>
                <el-descriptions-item label="库">MySQL 8.4 · broadband</el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="服务节点" name="nodes">
        <div class="card">
          <div class="card-head"><h3>节点状态</h3><SourceTag :live="false" label="单机部署口径" /></div>
          <el-table :data="nodes" style="width: 100%">
            <el-table-column prop="name" label="节点" min-width="170" />
            <el-table-column prop="addr" label="地址" width="160" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === '在线' ? 'success' : 'danger'" size="small" effect="light">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="CPU" width="140">
              <template #default="{ row }">
                <el-progress :percentage="row.cpu" :stroke-width="8" :show-text="false" color="#4f46e5" />
                <span class="mono">{{ row.cpu }}%</span>
              </template>
            </el-table-column>
            <el-table-column label="内存" width="140">
              <template #default="{ row }">
                <el-progress :percentage="row.mem" :stroke-width="8" :show-text="false" color="#10b981" />
                <span class="mono">{{ row.mem }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="qps" label="QPS" width="90" />
            <el-table-column label="响应" width="100">
              <template #default="{ row }">{{ row.rt }} ms</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="告警规则" name="alert">
        <div class="card">
          <div class="card-head"><h3>建议告警阈值</h3><span class="hint">规划：接入 Prometheus 后自动触发</span></div>
          <el-table
            :data="[
              { item: 'CPU 使用率', rule: '> 80% 持续 5 分钟', act: '通知值班' },
              { item: '堆内存使用率', rule: '> 85%', act: '通知 + 触发 Full GC 排查' },
              { item: '接口错误率', rule: '> 1%', act: '通知' },
              { item: '接口 P99 耗时', rule: '> 1s', act: '通知 + 定位慢 SQL' },
              { item: '数据库连接池', rule: '活跃 / 上限 > 90%', act: '通知' },
              { item: 'SLA 达标率', rule: '< 95%', act: '通知装维主管' }
            ]"
            style="width: 100%"
          >
            <el-table-column prop="item" label="监控项" min-width="160" />
            <el-table-column prop="rule" label="阈值规则" min-width="200" />
            <el-table-column prop="act" label="动作" min-width="200" />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
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
  margin: 0 0 8px;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 12px;
  font-size: 13px;
}

.row span {
  color: var(--bd-text-sub);
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: var(--bd-text-sub);
}

@media (max-width: 1100px) {
  .cols {
    grid-template-columns: 1fr;
  }
}
</style>
