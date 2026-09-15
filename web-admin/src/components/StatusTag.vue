<script setup>
import { computed } from 'vue'
import { statusMeta } from '@/utils/status'

const props = defineProps({
  /** 业务状态（中文标签或英文 code） */
  status: { type: [String, Number], default: '' },
  /** 是否显示前置状态点 */
  dot: { type: Boolean, default: true },
  size: { type: String, default: 'small' },
  /** 覆盖文本（默认用 statusMeta.label） */
  label: { type: String, default: '' }
})

const meta = computed(() => statusMeta(props.status))
</script>

<template>
  <el-tag :type="meta.type" :size="size" effect="light" class="status-tag">
    <span v-if="dot" class="st-dot" :class="`is-${meta.type}`"></span>
    <slot>{{ label || meta.label }}</slot>
  </el-tag>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border-radius: 6px;
  font-weight: 500;
  padding: 0 9px;
  height: 22px;
  line-height: 1;
}

.st-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex: 0 0 auto;
}

/* 语义点色（与 el-tag light 主题色呼应） */
.st-dot.is-success { background: #16a34a; }
.st-dot.is-info { background: #6b7280; }
.st-dot.is-warning { background: #d97706; }
.st-dot.is-danger { background: #dc2626; }
.st-dot.is-primary { background: #4f46e5; }
</style>
