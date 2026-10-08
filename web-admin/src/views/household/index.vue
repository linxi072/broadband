<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import { householdList, householdStats, householdCreate } from '@/api/business'
import { loadResource } from '@/composables/useResource'
import { demoHouseholds } from '@/mock/fallback'
import { useHouseholdStore } from '@/store/household'
import { useI18n } from '@/i18n'

const household = useHouseholdStore()
const { locale, t } = useI18n()

const rows = ref([])
const total = ref(0)
const page = ref(1)
const stats = ref({})
const live = ref(false)
const loading = ref(true)

const keyword = ref('')

// 新建对话框
const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({ name: '', address: '', communityId: '', ownerName: '', ownerPhone: '' })
const formRef = ref(null)
const rules = {
  name: [{ required: true, message: () => t('household.nameRequired'), trigger: 'blur' }]
}

const H_STATUS = {
  ACTIVE: { type: 'success', key: 'household.statusActive' },
  INACTIVE: { type: 'info', key: 'household.statusInactive' }
}
const statusMeta = (s) => {
  const m = H_STATUS[s] || { type: 'info', key: null }
  return { type: m.type, label: m.key ? t(m.key) : (s || '—') }
}

function buildParams() {
  const params = { page: page.value, size: 20 }
  if (keyword.value) params.keyword = keyword.value
  return params
}

async function load() {
  loading.value = true
  const r = await loadResource(() => householdList(buildParams()), () => demoHouseholds(buildParams()))
  rows.value = r.data?.list || []
  total.value = r.data?.total || 0
  live.value = r.live
  loading.value = false
}

async function loadStats() {
  const r = await loadResource(
    () => householdStats(),
    () => ({ total: 3, active: 3, inactive: 0, customerLinked: 2 })
  )
  stats.value = r.data || {}
}

function openCreate() {
  form.value = { name: '', address: '', communityId: '', ownerName: '', ownerPhone: '' }
  dialogVisible.value = true
}

async function submitCreate() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    await householdCreate({ ...form.value })
    ElMessage.success(t('household.created'))
    dialogVisible.value = false
    await Promise.all([load(), loadStats(), household.load()])
  } catch (e) {
    ElMessage.warning(t('household.offline'))
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), loadStats(), household.load()])
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">{{ t('household.title') }}</h2>
        <p class="page-sub">{{ t('household.sub') }}</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="mini-stats">
      <StatCard :label="t('household.statTotal')" :value="stats.total || 0" unit="个" />
      <StatCard :label="t('household.statActive')" :value="stats.active || 0" unit="个" tone="up" />
      <StatCard :label="t('household.statInactive')" :value="stats.inactive || 0" unit="个" />
      <StatCard :label="t('household.statLinked')" :value="stats.customerLinked || 0" unit="户" />
    </div>

    <div class="card">
      <div class="card-head">
        <div class="filters">
          <el-input
            v-model="keyword"
            :placeholder="t('household.searchPlaceholder')"
            clearable
            style="width: 220px"
            @keyup.enter="load"
            @clear="load"
          />
          <el-button type="primary" @click="load">{{ t('common.search') }}</el-button>
          <el-button type="success" @click="openCreate">{{ t('household.create') }}</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" size="small" style="width: 100%">
        <el-table-column prop="name" :label="t('household.table.name')" min-width="160" />
        <el-table-column prop="address" :label="t('household.table.address')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="communityName" :label="t('household.table.community')" min-width="120" />
        <el-table-column prop="ownerName" :label="t('household.table.owner')" width="100" />
        <el-table-column prop="ownerPhone" :label="t('household.table.ownerPhone')" width="130" />
        <el-table-column :label="t('household.table.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small" effect="light">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('household.table.created')" min-width="160">
          <template #default="{ row }">{{ new Date(row.createdTime).toLocaleString(locale === 'zh-CN' ? 'zh-CN' : 'en-US') }}</template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 新建住宅对话框 -->
    <el-dialog v-model="dialogVisible" :title="t('household.dialogTitle')" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="84px">
        <el-form-item :label="t('household.form.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('household.form.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('household.form.address')">
          <el-input v-model="form.address" :placeholder="t('household.form.addressPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('household.form.communityId')">
          <el-input v-model="form.communityId" :placeholder="t('household.form.communityIdPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('household.form.ownerName')">
          <el-input v-model="form.ownerName" :placeholder="t('household.form.ownerNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('household.form.ownerPhone')">
          <el-input v-model="form.ownerPhone" :placeholder="t('household.form.ownerPhonePlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="success" :loading="saving" @click="submitCreate">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 18px; }
.page-sub { margin: 4px 0 0; font-size: 13px; color: var(--bd-text-mute); }
.mini-stats { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.mini-stats > * { flex: 1; min-width: 150px; }
.card { margin-bottom: 16px; }
.card-head { margin-bottom: 12px; }
.filters { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
