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

/**
 * 图标映射：后端 sys_menu 当前未持久化图标（仅存 path / perm / type），
 * 前端按 path（目录与叶子共用同一 path 前缀）或 name 映射 emoji，保持与历史 navConfig 视觉一致。
 * 后续若后端菜单表增加 icon 字段，可直接优先使用后端下发值。
 */
const ICONS_BY_PATH = {
  '/dashboard': '📊',
  '/order': '📋',
  '/customer': '👤',
  '/package': '📦',
  '/package/marketing': '📣',
  '/package/upgrade': '⚡',
  '/community': '🏘',
  '/workorder/pool': '🔧',
  '/sla': '🛡',
  '/traffic': '📶',
  '/review': '⭐',
  '/sales': '💼',
  '/finance': '💰',
  '/system/user': '🔐',
  '/monitor': '📈'
}
const ICONS_BY_NAME = {
  套餐管理: '📦',
  小区覆盖管理: '🏘',
  安装工单: '🔧',
  权限管理: '🔐'
}
function iconOf(node) {
  return ICONS_BY_PATH[node.path] || ICONS_BY_NAME[node.name] || '📄'
}

/** 将后端菜单树 / demo 菜单树装饰为侧边栏可用的结构（剔除按钮类、补图标） */
function decorate(list) {
  return (list || [])
    .filter((m) => m.type !== 'BUTTON')
    .map((m) => ({
      id: m.id,
      name: m.name,
      path: m.path,
      perm: m.perm,
      type: m.type,
      icon: iconOf(m),
      children: m.children && m.children.length ? decorate(m.children) : undefined
    }))
}

/**
 * 动态菜单：优先使用后端 /auth/me 按当前用户角色下发的菜单树（user.menus），
 * 实现「不同角色看到不同菜单」。仅当动态菜单为空（极端异常）时，退回静态 navConfig，
 * 避免出现空白侧边栏。
 */
const menus = computed(() => {
  const dynamic = decorate(user.menus)
  if (dynamic.length) return dynamic
  return navConfig.map((item) => ({
    id: item.path,
    name: item.title,
    path: item.path,
    perm: item.perm,
    type: item.children ? 'DIR' : 'MENU',
    icon: iconOf(item),
    children: item.children
      ? item.children.map((c) => ({
          id: c.path,
          name: c.title,
          path: c.path,
          perm: c.perm,
          type: 'MENU',
          icon: iconOf(c)
        }))
      : undefined
  }))
})

const activePath = computed(() => route.path)

function go(path) {
  if (path && path !== route.path) router.push(path)
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
        <template v-for="item in menus" :key="item.id || item.path">
          <el-sub-menu v-if="item.children" :index="item.id || item.name">
            <template #title>
              <span class="mi">{{ item.icon }}</span>
              <span class="mt">{{ item.name }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.id || child.path"
              :index="child.path"
              @click="go(child.path)"
            >
              {{ child.name }}
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item v-else :index="item.path" @click="go(item.path)">
            <span class="mi">{{ item.icon }}</span>
            <template #title>{{ item.name }}</template>
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
