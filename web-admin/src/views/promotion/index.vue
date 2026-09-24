<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { promotionList } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoPromotions } from '@/mock/fallback'

const rows = ref([])
const live = ref(false)
const loading = ref(true)
const typeFilter = ref('')

const TYPES = [
  { v: 'NEW', label: '新装' },
  { v: 'RENEW', label: '续约' },
  { v: 'BUNDLE', label: '融合' }
]

const TYPEMAP = { NEW: '新装', RENEW: '续约', BUNDLE: '融合' }

const filtered = computed(() =>
  rows.value.filter((r) => !typeFilter.value || r.type === typeFilter.value)
)

function ruleText(r) {
  try {
    const o = JSON.parse(r.ruleJson || '{}')
    const map = { cut: '直降', months: '期', giftMonths: '赠月', gift: '赠' }
    return Object.entries(o)
      .map(([k, v]) => `${map[k] || k}${v}`)
      .join('，')
  } catch (e) {
    return r.ruleJson || '—'
  }
}

async function load() {
  loading.value = true
  const r = await loadResource(() => promotionList(), demoPromotions)
  rows.value = Array.isArray(r.data) ? r.data : []
  live.value = r.live
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">优惠活动专区</h2>
        <p class="page-sub">营销活动配置与运营效果总览（数据源 promotion 表）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="typeFilter" placeholder="全部类型" clearable style="width: 150px">
          <el-option v-for="t in TYPES" :key="t.v" :label="t.label" :value="t.v" />
        </el-select>
        <div class="spacer"></div>
        <el-button size="small" @click="load">刷新</el-button>
      </div>

      <div v-loading="loading" class="promo-grid">
        <div v-for="p in filtered" :key="p.id" class="promo-card">
          <div class="promo-top">
            <span class="promo-type">{{ TYPEMAP[p.type] || p.type }}</span>
            <StatusTag :status="p.status" />
          </div>
          <div class="promo-title">{{ p.title }}</div>
          <div class="promo-sub">{{ p.subtitle }}</div>
          <div class="promo-rule">规则：{{ ruleText(p) }}</div>
          <div class="promo-date">📅 {{ p.startDate }} ~ {{ p.endDate }}</div>
        </div>
        <EmptyState
          v-if="!filtered.length && !loading"
          icon="🎉"
          title="暂无活动"
          desc="创建限时直降、续约送时长等营销活动"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.promo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  padding: 16px 18px;
}
.promo-card {
  padding: 16px;
  border: 1px solid var(--bd-border);
  border-radius: var(--bd-radius);
  background: var(--bd-card);
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}
.promo-card:hover {
  box-shadow: var(--sh-md);
  transform: translateY(-2px);
}
.promo-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.promo-type {
  font-size: 12px;
  font-weight: 600;
  color: var(--bd-primary);
  background: var(--bd-primary-light);
  padding: 2px 8px;
  border-radius: 6px;
}
.promo-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}
.promo-sub {
  font-size: 13px;
  color: var(--bd-text-sub);
  margin-bottom: 10px;
}
.promo-rule {
  font-size: 12px;
  color: var(--bd-text-mute);
  margin-bottom: 6px;
}
.promo-date {
  font-size: 12px;
  color: var(--bd-text-sub);
}
</style>
