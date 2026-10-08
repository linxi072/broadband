<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/store/user'
import {
  notificationUnreadCount,
  notificationList,
  notificationMarkAllRead
} from '@/api/business'
import {
  demoNotificationUnreadCount,
  demoNotificationList,
  demoNotificationMarkAllRead
} from '@/mock/fallback'
import { fmtTime } from '@/composables/useResource'
import { ElMessage } from 'element-plus'

const router = useRouter()
const user = useUserStore()
const { t } = useI18n()

const unread = ref(0)
const recent = ref([])
const live = ref(false)
const visible = ref(false)
let timer = null

const POLL_MS = 30000

function typeLabel(et) {
  const k = 'notification.typeMap.' + et
  const v = t(k)
  return String(v).startsWith('notification.typeMap.') ? et : v
}
function roleLabel(r) {
  if (r === 'ADMIN') return t('notification.roleAdmin')
  if (r === 'OPERATOR') return t('notification.roleOperator')
  return t('notification.roleAll')
}

async function load() {
  try {
    const c = await notificationUnreadCount()
    unread.value = c.count || 0
    const r = await notificationList({ unreadOnly: true, size: 5, page: 1 })
    recent.value = (r.list || []).slice(0, 5)
    live.value = true
  } catch (e) {
    // 后端不可达 / 演示态：降级到本地演示数据
    unread.value = demoNotificationUnreadCount()
    recent.value = (demoNotificationList({ unreadOnly: true, size: 5 }).list || []).slice(0, 5)
    live.value = false
  }
}

async function markAll() {
  if (!live.value) {
    demoNotificationMarkAllRead()
    await load()
    ElMessage.success(t('notification.allRead'))
    return
  }
  try {
    await notificationMarkAllRead()
    await load()
    ElMessage.success(t('notification.allRead'))
  } catch (e) {
    ElMessage.error(e.message || 'fail')
  }
}

function goAll() {
  visible.value = false
  router.push('/notification')
}

function start() {
  load()
  timer = setInterval(load, POLL_MS)
}
function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(start)
onBeforeUnmount(stop)
</script>

<template>
  <el-popover v-model:visible="visible" :width="320" trigger="click" placement="bottom-end">
    <template #reference>
      <button class="icon-btn bell" :title="t('notification.bellTitle')">
        🔔
        <span v-if="unread > 0" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
      </button>
    </template>

    <div class="bell-panel">
      <div class="bell-head">
        <span class="bell-title">{{ t('notification.recentTitle') }}</span>
        <el-button link type="primary" size="small" @click="load">{{ t('notification.refresh') }}</el-button>
      </div>

      <div v-if="recent.length" class="bell-list">
        <div v-for="row in recent" :key="row.id" class="bell-item" @click="goAll">
          <div class="bell-item-title">
            <span class="dot"></span>{{ row.title }}
          </div>
          <div class="bell-item-meta">
            <el-tag size="small" effect="plain">{{ typeLabel(row.eventType) }}</el-tag>
            <span class="bell-item-role">{{ roleLabel(row.targetRole) }}</span>
            <span class="bell-item-time">{{ fmtTime(row.createdTime) }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else :description="t('notification.noUnread')" :image-size="48" />

      <div class="bell-foot">
        <el-button link type="primary" size="small" @click="goAll">{{ t('notification.viewAll') }}</el-button>
        <el-button
          v-if="unread > 0"
          link
          type="success"
          size="small"
          @click="markAll"
        >{{ t('notification.markAllRead') }}</el-button>
      </div>
    </div>
  </el-popover>
</template>

<style scoped>
.bell {
  position: relative;
}
.badge {
  position: absolute;
  top: -4px;
  right: -6px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
  border: 1px solid #fff;
}
.bell-panel {
  display: flex;
  flex-direction: column;
}
.bell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.bell-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--bd-text);
}
.bell-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}
.bell-item {
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.bell-item:hover {
  background: var(--bd-primary-light);
}
.bell-item-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--bd-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bell-item-title .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f56c6c;
  flex: 0 0 auto;
}
.bell-item-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 11px;
  color: var(--bd-text-mute);
}
.bell-item-time {
  margin-left: auto;
}
.bell-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--bd-border);
}
</style>
