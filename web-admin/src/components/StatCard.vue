<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '—' },
  unit: { type: String, default: '' },
  foot: { type: String, default: '' },
  /** 语义色：normal | up | down | warn */
  tone: { type: String, default: 'normal' }
})
</script>

<template>
  <div class="stat" :class="`is-${tone}`">
    <div class="label">{{ label }}</div>
    <div class="value">
      {{ value }}<small v-if="unit">{{ unit }}</small>
    </div>
    <div v-if="foot" class="foot">{{ foot }}</div>
  </div>
</template>
