<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import { upgradeOptions, submitUpgrade, adminUpgradeOrders, customerList } from '@/api/business'
import { loadResource, money, fmtTime } from '@/composables/useResource'
import { demoUpgradeOrders, demoCustomers } from '@/mock/fallback'

const tab = ref('config')

const customers = ref(demoCustomers)
const orders = ref(demoUpgradeOrders)
const ordersLive = ref(false)
const loadingOrders = ref(true)

const form = reactive({ customerId: 'demo', targetBand: '', addons: [], effectType: 'immediate' })

const options = ref(null)
const optionsLive = ref(false)
const optionsLoading = ref(false)
const preview = ref(null)
const submitting = ref(false)

const bandOptions = computed(() => ((options.value && options.value.options) || []).filter((o) => o.type === 'bandwidth'))
const addonOptions = computed(() => ((options.value && options.value.options) || []).filter((o) => o.type === 'addon'))
const current = computed(() => (options.value && options.value.current) || {})
const remainMonths = computed(() => Number(current.value.contractLeftMonths || 0))

/** 本地实时补差预览（与 PackageUpgradeEngine 同口径：一次性补差 = 月差 × 剩余合约月数） */
const localPreview = computed(() => {
  const base = Number(current.value.fee || 0)
  const band = bandOptions.value.find((o) => o.key === form.targetBand)
  const addonFee = addonOptions.value
    .filter((o) => form.addons.includes(o.key))
    .reduce((s, o) => s + Number(o.extraFee || 0), 0)
  const monthDiff = Number(band ? band.extraFee : 0) + addonFee
  return {
    currentFee: base,
    monthDiff,
    newFee: base + monthDiff,
    oneTimeDiff: monthDiff * remainMonths.value
  }
})

const show = computed(() => preview.value || localPreview.value)

const effectLabel = (t) => (t === 'nextMonth' ? '次月生效' : '立即生效')
const statusType = (s) => ({ 待审核: 'warning', 已生效: 'success', 已驳回: 'danger' }[s] || 'info')

async function loadOptions() {
  optionsLoading.value = true
  const r = await loadResource(() => upgradeOptions(form.customerId), null)
  options.value = r.data
  optionsLive.value = r.live
  if (r.data && r.data.options && r.data.options.length) {
    form.targetBand = r.data.options.find((o) => o.type === 'bandwidth')?.key || ''
  }
  preview.value = null
  optionsLoading.value = false
}

async function submit() {
  if (!form.targetBand && !form.addons.length) return ElMessage.warning('请选择升档或加购项')
  submitting.value = true
  const r = await loadResource(
    () =>
      submitUpgrade({
        customerId: form.customerId,
        targetBand: form.targetBand,
        addons: form.addons,
        effectType: form.effectType
      }),
    null
  )
  submitting.value = false
  if (r.live && r.data) {
    preview.value = r.data
    ElMessage.success('升级申请已提交并落库')
    loadOrders()
  } else {
    preview.value = null
    ElMessage.warning('提交接口不可达，仅展示本地补差预览')
  }
}

async function loadOrders() {
  const r = await loadResource(() => adminUpgradeOrders(), demoUpgradeOrders)
  const data = r.data
  orders.value = Array.isArray(data) ? data : (data && data.records) || demoUpgradeOrders
  ordersLive.value = r.live
  loadingOrders.value = false
}

onMounted(async () => {
  const c = await loadResource(() => customerList({ size: 100 }), demoCustomers)
  const list = Array.isArray(c.data) ? c.data : (c.data && c.data.records) || demoCustomers
  if (list.length) {
    customers.value = list
    form.customerId = list[0].id
  }
  await loadOptions()
  await loadOrders()
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">套餐升级管理</h2>
        <p class="page-sub">
          补差规则：一次性补差 = 月差 ×（剩余合约天数 ÷ 30），按天折算；生效策略支持「立即 / 次月」
        </p>
      </div>
      <SourceTag :live="optionsLive" />
    </div>

    <el-tabs v-model="tab" class="tabs">
      <el-tab-pane label="升级配置" name="config">
        <div class="cols">
          <div class="card">
            <div class="card-head">
              <h3>升档 / 加购配置</h3>
              <SourceTag :live="optionsLive" />
            </div>
            <div v-loading="optionsLoading" class="card-body">
              <el-form label-width="88px" label-position="left">
                <el-form-item label="客户">
                  <el-select v-model="form.customerId" style="width: 100%" @change="loadOptions">
                    <el-option
                      v-for="c in customers"
                      :key="c.id"
                      :label="`${c.name}（${c.id}）`"
                      :value="c.id"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="当前套餐">
                  <span class="cur">
                    {{ current.name || '—' }} ｜ 月费 {{ money(current.fee) }} ｜ 剩余合约
                    <b>{{ remainMonths }}</b> 个月
                  </span>
                </el-form-item>

                <el-form-item label="升档目标">
                  <el-radio-group v-model="form.targetBand">
                    <el-radio-button v-for="b in bandOptions" :key="b.key" :label="b.key">
                      {{ b.name }}<template v-if="b.extraFee"> +{{ b.extraFee }}</template>
                    </el-radio-button>
                  </el-radio-group>
                  <div v-if="!bandOptions.length" class="mute">当前套餐未配置带宽选项</div>
                </el-form-item>

                <el-form-item label="增值加购">
                  <el-checkbox-group v-model="form.addons">
                    <el-checkbox v-for="a in addonOptions" :key="a.key" :label="a.key">
                      {{ a.name }}<template v-if="a.extraFee"> +{{ a.extraFee }}</template>
                    </el-checkbox>
                  </el-checkbox-group>
                  <div v-if="!addonOptions.length" class="mute">当前套餐未配置增值选项</div>
                </el-form-item>

                <el-form-item label="生效方式">
                  <el-radio-group v-model="form.effectType">
                    <el-radio-button label="immediate">立即生效</el-radio-button>
                    <el-radio-button label="nextMonth">次月生效</el-radio-button>
                  </el-radio-group>
                </el-form-item>

                <el-button type="primary" :loading="submitting" @click="submit">提交升级申请</el-button>
              </el-form>
            </div>
          </div>

          <div>
            <div class="card">
              <div class="card-head">
                <h3>补差预览</h3>
                <span class="hint">{{ preview ? '服务端返回' : '本地同口径估算' }}</span>
              </div>
              <div class="card-body">
                <div class="calc">
                  <div class="row"><span>当前月费</span><b>{{ money(show.currentFee) }}</b></div>
                  <div class="row"><span>月补差</span><b class="up">+{{ money(show.monthDiff) }}</b></div>
                  <div class="row"><span>升级后月费</span><b>{{ money(show.newFee) }}</b></div>
                  <div class="row big">
                    <span>一次性补差</span>
                    <b class="up">{{ money(show.oneTimeDiff) }}</b>
                  </div>
                  <p class="formula">
                    口径：{{ money(show.monthDiff) }} × {{ remainMonths }} 个月（剩余合约）
                  </p>
                  <p class="formula">生效方式：{{ effectLabel(form.effectType) }}</p>
                </div>
              </div>
            </div>

            <div class="card">
              <div class="card-head"><h3>升级统计</h3></div>
              <div class="card-body stat-grid">
                <StatCard label="申请单数" :value="orders.length" unit=" 单" />
                <StatCard
                  label="待审核"
                  :value="orders.filter((o) => o.status === '待审核').length"
                  unit=" 单"
                  tone="warn"
                />
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="进度跟踪" name="track">
        <div class="card">
          <div class="card-head">
            <h3>升级申请单</h3>
            <SourceTag :live="ordersLive" />
          </div>
          <el-table v-loading="loadingOrders" :data="orders" style="width: 100%">
            <el-table-column prop="id" label="申请单号" min-width="150" />
            <el-table-column prop="customer" label="客户" width="100" />
            <el-table-column label="原套餐 → 新套餐" min-width="200">
              <template #default="{ row }">
                {{ row.fromPkg || row.fromPackageId }} → {{ row.toPkg || row.targetBandKey }}
              </template>
            </el-table-column>
            <el-table-column label="月补差" width="100">
              <template #default="{ row }">+{{ money(row.monthDiff) }}</template>
            </el-table-column>
            <el-table-column label="一次性补差" width="120">
              <template #default="{ row }">
                <b class="up">{{ money(row.oneTimeDiff) }}</b>
              </template>
            </el-table-column>
            <el-table-column label="生效" width="100">
              <template #default="{ row }">{{ effectLabel(row.effectType) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small" effect="light">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="150">
              <template #default="{ row }">{{ row.time || fmtTime(row.createdTime) }}</template>
            </el-table-column>
            <template #empty>暂无升级申请单</template>
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

.tabs {
  margin-top: 4px;
}

.cols {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
  align-items: start;
}

.cur {
  font-size: 13px;
  color: var(--bd-text-sub);
}

.mute {
  font-size: 12px;
  color: var(--bd-text-mute);
}

.calc .row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed var(--bd-border);
  font-size: 13px;
}

.calc .row span {
  color: var(--bd-text-sub);
}

.calc .row.big b {
  font-size: 20px;
}

.up {
  color: var(--bd-up);
}

.formula {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--bd-text-mute);
}

.stat-grid {
  grid-template-columns: 1fr 1fr;
}

@media (max-width: 1100px) {
  .cols {
    grid-template-columns: 1fr;
  }
}
</style>
