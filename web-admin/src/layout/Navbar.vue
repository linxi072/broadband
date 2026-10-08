<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { useHouseholdStore } from '@/store/household'
import { useI18n } from '@/i18n'
import { navConfig } from '@/router/routes'
import NotificationBell from '@/components/NotificationBell.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const app = useAppStore()
const household = useHouseholdStore()
const { locale, t, setLocale } = useI18n()

onMounted(() => {
  household.load()
})

const groupTitle = computed(() => {
  const hit = navConfig.find(
    (n) => n.children && n.children.some((c) => (c.routePath || c.path) === route.path)
  )
  return hit ? hit.title : ''
})

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await user.logout()
    router.replace('/login')
  }
}
</script>

<template>
  <div class="navbar">
    <button class="icon-btn" :title="app.collapsed ? t('navbar.expand') : t('navbar.collapse')" @click="app.toggleSidebar()">
      ☰
    </button>

    <div class="crumb">
      <span class="crumb-root">{{ t('navbar.root') }}</span>
      <template v-if="groupTitle">
        <i>/</i><span>{{ groupTitle }}</span>
      </template>
      <i>/</i><b>{{ route.meta.title || '—' }}</b>
    </div>

    <div class="spacer"></div>

    <el-select
      :model-value="locale"
      size="small"
      style="width: 96px"
      @update:model-value="(v) => setLocale(v)"
    >
      <el-option :label="t('navbar.zh')" value="zh-CN" />
      <el-option :label="t('navbar.en')" value="en-US" />
    </el-select>

    <el-select
      v-model="household.currentId"
      :placeholder="t('navbar.allHousehold')"
      size="small"
      style="width: 160px"
      @change="(v) => household.setCurrent(v)"
    >
      <el-option v-for="opt in household.options" :key="opt.id" :label="opt.name" :value="opt.id" />
    </el-select>

    <el-tag v-if="user.demoMode" type="warning" effect="light" size="small" round>
      {{ t('navbar.demo') }}
    </el-tag>
    <el-tag v-else type="success" effect="light" size="small" round>{{ t('navbar.online') }}</el-tag>

    <NotificationBell />

    <el-dropdown @command="onCommand">
      <span class="user">
        <el-avatar :size="26" style="background: var(--bd-primary)">
          {{ user.displayName.slice(0, 1) }}
        </el-avatar>
        <span class="uname">{{ user.displayName }}</span>
        <span class="role">{{ user.profile?.roleNames?.[0] || '—' }}</span>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item disabled>{{ user.profile?.dept || t('navbar.dept') }}</el-dropdown-item>
          <el-dropdown-item divided command="logout">{{ t('navbar.logout') }}</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 100%;
  padding: 0 16px;
}

.icon-btn {
  width: 30px;
  height: 30px;
  border: 1px solid var(--bd-border);
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: var(--bd-text-sub);
  line-height: 1;
}

.icon-btn:hover {
  color: var(--bd-primary);
  border-color: var(--bd-primary-border);
}

.crumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--bd-text-sub);
}

.crumb i {
  font-style: normal;
  color: var(--bd-text-mute);
}

.crumb b {
  color: var(--bd-text);
}

.crumb-root {
  color: var(--bd-text-sub);
}

.spacer {
  flex: 1;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}

.user:hover {
  background: var(--bd-primary-light);
}

.uname {
  font-size: 13px;
  font-weight: 500;
}

.role {
  font-size: 11px;
  color: var(--bd-text-mute);
  border-left: 1px solid var(--bd-border);
  padding-left: 8px;
}
</style>
