<script setup>
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
import { useAppStore } from '@/store/app'

const app = useAppStore()
</script>

<template>
  <div class="layout">
    <aside class="aside" :style="{ width: app.collapsed ? '64px' : '224px' }">
      <Sidebar />
    </aside>
    <div class="body">
      <header class="header">
        <Navbar />
      </header>
      <main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100%;
  background: var(--bd-bg);
}

.aside {
  flex: 0 0 auto;
  height: 100%;
  background: #1e1b4b;
  transition: width 0.18s ease;
  overflow: hidden;
}

.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.header {
  flex: 0 0 auto;
  height: var(--bd-navbar-h);
  background: #fff;
  border-bottom: 1px solid var(--bd-border);
  padding: 0;
}

.main {
  flex: 1;
  overflow: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
