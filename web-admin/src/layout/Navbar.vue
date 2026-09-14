<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { navConfig } from '@/router/routes'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const app = useAppStore()

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
    <button class="icon-btn" :title="app.collapsed ? '展开菜单' : '收起菜单'" @click="app.toggleSidebar()">
      ☰
    </button>

    <div class="crumb">
      <span class="crumb-root">运营后台</span>
      <template v-if="groupTitle">
        <i>/</i><span>{{ groupTitle }}</span>
      </template>
      <i>/</i><b>{{ route.meta.title || '—' }}</b>
    </div>

    <div class="spacer"></div>

    <el-tag v-if="user.demoMode" type="warning" effect="light" size="small" round>
      演示模式
    </el-tag>
    <el-tag v-else type="success" effect="light" size="small" round>后端在线</el-tag>

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
          <el-dropdown-item disabled>{{ user.profile?.dept || '运营中心' }}</el-dropdown-item>
          <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
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
