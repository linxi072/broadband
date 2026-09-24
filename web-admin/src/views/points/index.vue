<script setup>
import { computed, onMounted, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { pointsBalance, pointsTasks, pointsMall, pointsSign, pointsRedeem } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoPointsBalance, demoPointsTasks, demoPointsMall } from '@/mock/fallback'

const CUSTOMER = 'C-DMO01'

const balance = ref(null)
const tasks = ref([])
const mall = ref([])
const live = ref(false)
const loading = ref(true)

const stats = computed(() => {
  const b = balance.value || {}
  return [
    { label: '当前积分', value: b.balance ?? '—', cls: 'is-up' },
    { label: '累计获得', value: b.totalEarned ?? '—', cls: '' },
    { label: '累计消耗', value: b.totalSpent ?? '—', cls: 'is-warn' },
    { label: '连续签到', value: `${b.signStreak ?? 0} 天`, cls: '' }
  ]
})

const TYPE_MAP = { DAILY: '每日', ONE_TIME: '一次性', INVITE: '邀请' }

async function onSign() {
  const r = await loadResource(() => pointsSign(CUSTOMER), null)
  if (r.live) {
    const earned = r.data && r.data.earned ? r.data.earned : 5
    ElMessage.success(`签到成功，获得 ${earned} 积分`)
    load()
  } else {
    ElMessage.warning('签到接口不可达：仅本地演示')
  }
}

async function onRedeem(item) {
  const r = await loadResource(() => pointsRedeem(CUSTOMER, item.id), null)
  if (r.live) {
    const code = r.data && r.data.couponCode ? `（券码 ${r.data.couponCode}）` : ''
    ElMessage.success(`兑换成功，扣除 ${item.cost} 积分${code}`)
    load()
  } else {
    ElMessage.warning('兑换接口不可达：仅本地演示')
  }
}

async function load() {
  loading.value = true
  const [b, t, m] = await Promise.all([
    loadResource(() => pointsBalance(CUSTOMER), demoPointsBalance),
    loadResource(() => pointsTasks(CUSTOMER), demoPointsTasks),
    loadResource(() => pointsMall(), demoPointsMall)
  ])
  balance.value = b.data
  tasks.value = Array.isArray(t.data) ? t.data : []
  mall.value = Array.isArray(m.data) ? m.data : []
  live.value = b.live && t.live && m.live
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">积分成长体系</h2>
        <p class="page-sub">积分余额、成长任务与积分商城运营（数据源 points_* 表）</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="stat-grid">
      <div v-for="s in stats" :key="s.label" class="stat" :class="s.cls">
        <div class="label">{{ s.label }}</div>
        <div class="value">{{ s.value }}</div>
      </div>
    </div>

    <div class="card">
      <div class="card-head">
        <h3>成长任务</h3>
        <el-button size="small" type="primary" @click="onSign">模拟签到</el-button>
      </div>
      <el-table v-loading="loading" :data="tasks" style="width: 100%">
        <el-table-column prop="title" label="任务" min-width="120" />
        <el-table-column prop="desc" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ TYPE_MAP[row.type] || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="90" align="right" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.done ? 'success' : 'warning'" effect="light">
              {{ row.done ? '已完成' : '未完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState icon="🎯" title="暂无成长任务" desc="配置签到、完善资料等积分任务以激活用户成长体系" />
        </template>
      </el-table>
    </div>

    <div class="card">
      <div class="card-head">
        <h3>积分商城</h3>
        <span class="hint">共 {{ mall.length }} 件兑换物</span>
      </div>
      <div v-loading="loading" class="mall-grid">
        <div v-for="item in mall" :key="item.id" class="mall-item">
          <div class="mall-name">{{ item.name }}</div>
          <div class="mall-meta">
            <span class="mall-cost"><b>{{ item.cost }}</b> 积分</span>
            <span class="mall-stock">库存 {{ item.stock }}</span>
          </div>
          <div class="mall-tag">
            <el-tag size="small" effect="plain">
              {{ item.couponType || item.couponValue || TYPE_MAP[item.type] || item.type || '通用' }}
            </el-tag>
            <el-button size="small" type="primary" :disabled="item.stock === 0" @click="onRedeem(item)">兑换</el-button>
          </div>
        </div>
        <EmptyState
          v-if="!mall.length && !loading"
          icon="🛍"
          title="商城暂无兑换物"
          desc="上架提速包、会员卡等积分兑换商品"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.mall-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  padding: 16px 18px;
}
.mall-item {
  padding: 14px;
  border: 1px solid var(--bd-border);
  border-radius: var(--bd-radius);
  background: var(--bd-card);
}
.mall-name {
  font-weight: 600;
  margin-bottom: 8px;
}
.mall-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--bd-text-sub);
  margin-bottom: 10px;
}
.mall-tag {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.mall-cost b {
  color: var(--bd-primary);
  font-size: 15px;
}
</style>
