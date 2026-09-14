<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/user'

const props = defineProps({
  live: { type: Boolean, default: true },
  label: { type: String, default: '' }
})

const user = useUserStore()

const text = computed(() => {
  if (user.demoMode) return props.label || '演示数据'
  return props.live ? '实时数据' : props.label || '演示数据'
})
</script>

<template>
  <span class="src-tag" :class="live && !user.demoMode ? 'live' : 'demo'">
    <span class="d"></span>{{ text }}
  </span>
</template>

<style scoped>
.d {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  display: inline-block;
  margin-right: 2px;
}
</style>
