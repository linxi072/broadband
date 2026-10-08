<script setup>
import { onMounted, ref, computed } from 'vue'
import SourceTag from '@/components/SourceTag.vue'
import StatCard from '@/components/StatCard.vue'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { loadResource, fmtTime } from '@/composables/useResource'
import {
  notificationList,
  notificationMarkRead,
  notificationMarkAllRead,
  notificationCreate
} from '@/api/business'
import {
  demoNotificationList,
  demoNotificationMarkRead,
  demoNotificationMarkAllRead,
  demoNotificationCreate
} from '@/mock/fallback'

const { t } = useI18n()
const user = useUserStore()

const rows = ref([])
const total = ref(0)
const unread = ref(0)
const live = ref(false)
const loading = ref(true)
const unreadOnly = ref(false)

// 管理端主动推送
const broadcastVisible = ref(false)
const broadcastForm = ref({ title: '', content: '', targetRole: 'ALL' })
const broadcastLoading = ref(false)

const readCount = computed(() => Math.max(0, total.value - unread.value))

function typeLabel(et) {
  const k = 'notification.typeMap.' + et
  const v = t(k)
  return String(v).startsWith('notification.typeMap.') ? et : v
}
function roleMeta(role) {
  if (role === 'ADMIN') return { label: t('notification.roleAdmin'), type: 'warning' }
  if (role === 'OPERATOR') return { label: t('notification.roleOperator'), type: 'primary' }
  return { label: t('notification.roleAll'), type: 'info' }
}

function buildParams() {
  return { page: 1, size: 20, unreadOnly: unreadOnly.value }
}

async function load() {
  loading.value = true
  const r = await loadResource(() => notificationList(buildParams()), () => demoNotificationList(buildParams()))
  rows.value = r.data?.list || []
  total.value = r.data?.total || 0
  unread.value = r.data?.unread || 0
  live.value = r.live
  loading.value = false
}

async function markRead(id) {
  if (live.value) {
    try {
      await notificationMarkRead([id])
    } catch (e) {
      ElMessage.error(e.message || 'fail')
      return
    }
  } else {
    demoNotificationMarkRead([id])
  }
  await load()
}

async function markAll() {
  try {
    await ElMessageBox.confirm(t('notification.markAllReadConfirm'), t('notification.markAllRead'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
  } catch (e) {
    return
  }
  if (live.value) {
    try {
      await notificationMarkAllRead()
    } catch (e) {
      ElMessage.error(e.message || 'fail')
      return
    }
  } else {
    demoNotificationMarkAllRead()
  }
  await load()
  ElMessage.success(t('notification.allRead'))
}

function openBroadcast() {
  broadcastForm.value = { title: '', content: '', targetRole: 'ALL' }
  broadcastVisible.value = true
}

async function submitBroadcast() {
  if (!broadcastForm.value.title.trim()) {
    ElMessage.warning(t('notification.broadcastNamePlaceholder'))
    return
  }
  broadcastLoading.value = true
  const payload = {
    title: broadcastForm.value.title.trim(),
    content: broadcastForm.value.content.trim(),
    targetRole: broadcastForm.value.targetRole
  }
  try {
    if (live.value) {
      await notificationCreate(payload)
    } else {
      demoNotificationCreate(payload)
    }
    broadcastVisible.value = false
    ElMessage.success(t('notification.sent'))
    await load()
  } catch (e) {
    ElMessage.error(e.message || 'fail')
  } finally {
    broadcastLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="page-title">{{ t('notification.title') }}</h2>
        <p class="page-sub">{{ t('notification.sub') }}</p>
      </div>
      <SourceTag :live="live" />
    </div>

    <div class="mini-stats">
      <StatCard :label="t('notification.statTotal')" :value="total" />
      <StatCard :label="t('notification.statUnread')" :value="unread" tone="warn" />
      <StatCard :label="t('notification.statRead')" :value="readCount" tone="up" />
    </div>

    <div class="card">
      <div class="card-head">
        <div class="filters">
          <el-switch
            v-model="unreadOnly"
            :active-text="t('notification.filterUnread')"
            @change="load"
          />
          <el-button type="primary" plain @click="openBroadcast">{{ t('notification.broadcast') }}</el-button>
          <el-button type="success" plain :disabled="unread === 0" @click="markAll">
            {{ t('notification.markAllRead') }}
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" size="small" style="width: 100%">
        <el-table-column :label="t('notification.colTitle')" min-width="160">
          <template #default="{ row }">
            <span class="title" :class="{ unread: row.isRead === 0 }">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('notification.colContent')" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content }}</template>
        </el-table-column>
        <el-table-column :label="t('notification.colType')" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('notification.colTarget')" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="roleMeta(row.targetRole).type">{{ roleMeta(row.targetRole).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('notification.colStatus')" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isRead === 0" size="small" type="danger">NEW</el-tag>
            <el-tag v-else size="small" type="info">—</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('notification.colTime')" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('notification.colAction')" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.isRead === 0"
              type="primary"
              link
              size="small"
              @click="markRead(row.id)"
            >{{ t('notification.markRead') }}</el-button>
            <span v-else class="done">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && rows.length === 0" :description="t('notification.empty')">
        <template #description>
          <div>
            <p>{{ t('notification.empty') }}</p>
            <p class="empty-sub">{{ t('notification.emptyDesc') }}</p>
          </div>
        </template>
      </el-empty>
    </div>

    <el-dialog v-model="broadcastVisible" :title="t('notification.broadcastTitle')" width="460px">
      <el-form label-width="84px">
        <el-form-item :label="t('notification.colTitle')">
          <el-input v-model="broadcastForm.title" :placeholder="t('notification.broadcastNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('notification.colContent')">
          <el-input
            v-model="broadcastForm.content"
            type="textarea"
            :rows="3"
            :placeholder="t('notification.broadcastContentPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('notification.broadcastTarget')">
          <el-select v-model="broadcastForm.targetRole" style="width: 160px">
            <el-option :label="t('notification.roleAll')" value="ALL" />
            <el-option :label="t('notification.roleAdmin')" value="ADMIN" />
            <el-option :label="t('notification.roleOperator')" value="OPERATOR" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="broadcastVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="broadcastLoading" @click="submitBroadcast">
          {{ t('notification.send') }}
        </el-button>
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
.filters { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.title.unread { font-weight: 600; color: var(--bd-text); }
.title { color: var(--bd-text-sub); }
.done { color: var(--bd-text-mute); }
.empty-sub { font-size: 12px; color: var(--bd-text-mute); margin: 0; }
</style>
