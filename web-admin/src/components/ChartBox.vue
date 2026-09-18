<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '280px' }
})
const emit = defineEmits(['chart-click'])

const el = ref(null)
let chart = null
let ro = null

function render() {
  if (!chart) return
  chart.setOption(props.option, true)
}

/** 是否含可绘制的序列数据（无数据时展示空态而非空白坐标轴） */
const hasData = computed(() => {
  const s = props.option && props.option.series
  if (!s) return false
  const arr = Array.isArray(s) ? s : [s]
  return arr.some((ser) => Array.isArray(ser.data) && ser.data.length)
})

onMounted(() => {
  if (!el.value) return
  chart = echarts.init(el.value)
  render()
  chart.on('click', (params) => emit('chart-click', params))
  ro = new ResizeObserver(() => chart && chart.resize())
  ro.observe(el.value)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  if (ro) ro.disconnect()
  if (chart) chart.dispose()
  chart = null
})
</script>

<template>
  <div class="chart-wrap" :style="{ height }">
    <div ref="el" style="width: 100%; height: 100%"></div>
    <EmptyState
      v-if="!hasData"
      class="chart-empty"
      icon="📊"
      title="暂无图表数据"
      desc="后端接通后将展示趋势与指标"
      compact
    />
  </div>
</template>

<style scoped>
.chart-wrap {
  position: relative;
  width: 100%;
}

/* 空态覆盖在图表区域中央 */
.chart-empty {
  position: absolute;
  inset: 0;
  padding: 0;
}
</style>
