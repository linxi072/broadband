<script setup>
defineProps({
  /** 图标（emoji 或 字符） */
  icon: { type: String, default: '📭' },
  /** 主标题 */
  title: { type: String, default: '暂无数据' },
  /** 补充说明 */
  desc: { type: String, default: '' },
  /** 纵向内边距微调 */
  compact: { type: Boolean, default: false }
})
</script>

<template>
  <div class="empty-state" :class="{ compact }">
    <div class="es-icon">{{ icon }}</div>
    <div class="es-title">{{ title }}</div>
    <div v-if="desc" class="es-desc">{{ desc }}</div>
    <div v-if="$slots.action" class="es-action">
      <slot name="action" />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 44px 16px;
  text-align: center;
  user-select: none;
}

.empty-state.compact {
  padding: 28px 16px;
}

.es-icon {
  font-size: 36px;
  line-height: 1;
  margin-bottom: 12px;
  opacity: 0.72;
  filter: saturate(0.9);
}

.es-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--bd-text-sub);
  letter-spacing: 0.01em;
}

.es-desc {
  margin-top: 7px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--bd-text-mute);
  max-width: 340px;
}

.es-action {
  margin-top: 16px;
}
</style>
