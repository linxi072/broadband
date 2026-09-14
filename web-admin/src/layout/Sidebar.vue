<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { navConfig } from '@/router/routes'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const app = useAppStore()

/** 按权限码过滤菜单：无权限的叶子 / 空分组不渲染 */
const menus = computed(() =>
  navConfig
    .map((item) => {
      if (item.children) {
        const children = item.children.filter((c) => !c.perm || user.perms.includes(c.perm))
        return children.length ? { ...item, children } : null
      }
      return !item.perm || user.perms.includes(item.perm) ? item : null
    })
    .filter(Boolean)
)

const activePath = computed(() => route.path)

function go(path) {
  if (path !== route.path) router.push(path)
}
</script>

<template>
  <div class="sidebar">
    <div class="brand" :class="{ collapsed: app.collapsed }">
      <span class="logo">宽</span>
      <span v-show="!app.collapsed" class="brand-text">
        <b>宽带运营后台</b>
        <i>Broadband Admin</i>
      </span>
    </div>

    <el-scrollbar class="menu-scroll">
      <el-menu
        :default-active="activePath"
        :collapse="app.collapsed"
        :collapse-transition="false"
        background-color="#1e1b4b"
        text-color="#c7c9e0"
        active-text-color="#ffffff"
        unique-opened
      >
        <template v-for="item in menus" :key="item.path || item.title">
          <el-sub-menu v-if="item.children" :index="item.title">
            <template #title>
              <span class="mi">{{ item.icon }}</span>
              <span class="mt">{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
              @click="go(child.path)"
            >
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item v-else :index="item.path" @click="go(item.path)">
            <span class="mi">{{ item.icon }}</span>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>

    <div v-show="!app.collapsed" class="side-foot">
      <span class="dot" :class="user.demoMode ? 'demo' : 'live'"></span>
      {{ user.demoMode ? '演示模式（后端未连接）' : '已连接后端 :8082' }}
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1e1b4b;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand.collapsed {
  justify-content: center;
  padding: 0;
}

.logo {
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--bd-primary);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
  overflow: hidden;
  white-space: nowrap;
}

.brand-text b {
  color: #fff;
  font-size: 14px;
  letter-spacing: 0.02em;
}

.brand-text i {
  color: #8b8fb8;
  font-size: 10px;
  font-style: normal;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.menu-scroll {
  flex: 1;
  overflow: hidden;
}

.sidebar :deep(.el-menu) {
  border-right: none;
}

.sidebar :deep(.el-menu-item),
.sidebar :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  font-size: 13.5px;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: var(--bd-primary) !important;
  font-weight: 600;
}

.sidebar :deep(.el-menu-item:hover),
.sidebar :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}

.sidebar :deep(.el-sub-menu .el-menu-item) {
  padding-left: 44px !important;
  min-width: 0;
}

.mi {
  display: inline-block;
  width: 20px;
  margin-right: 8px;
  font-size: 14px;
  text-align: center;
}

.side-foot {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  font-size: 11px;
  color: #8b8fb8;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  white-space: nowrap;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot.live {
  background: #22c55e;
}

.dot.demo {
  background: #f59e0b;
}
</style>
