<script setup>
import { computed } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import EmptyState from '@/components/EmptyState.vue'

/**
 * 统一的「图表点击 → 明细抽屉」组件（US-2.2 报表下钻标准化）。
 * 销售 / 财务 / 运营（数据智能）看板共用此组件，保证下钻交互一致。
 *
 * columns: [{ prop, label, width?, align?, format?(row) => string }]
 *   - 若提供 format，则用 #default 插槽渲染返回值
 */
const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '明细下钻' },
  columns: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  live: { type: Boolean, default: false }
})
const emit = defineEmits(['update:visible'])

const open = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v)
})
</script>

<template>
  <el-drawer v-model="open" :title="title" size="58%" :append-to-body="true">
    <div class="drawer-head">
      <SourceTag :live="live" />
      <span class="count">共 {{ rows.length }} 条记录</span>
    </div>
    <el-table v-loading="loading" :data="rows" empty-text="该维度暂无下钻数据" style="width: 100%">
      <el-table-column
        v-for="c in columns"
        :key="c.prop"
        :prop="c.prop"
        :label="c.label"
        :width="c.width"
        :align="c.align || 'left'"
        :min-width="c.minWidth"
      >
        <template v-if="c.format" #default="{ row }">{{ c.format(row) }}</template>
      </el-table-column>
      <template #empty>
        <EmptyState icon="🔍" title="暂无明细记录" desc="该维度暂无下钻数据" compact />
      </template>
    </el-table>
  </el-drawer>
</template>

<style scoped>
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.count {
  font-size: 12px;
  color: var(--bd-text-mute);
}
</style>
