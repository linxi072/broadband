import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: localStorage.getItem('bd_sidebar_collapsed') === 'true',
    openGroups: {}
  }),
  actions: {
    toggleSidebar() {
      this.collapsed = !this.collapsed
      localStorage.setItem('bd_sidebar_collapsed', String(this.collapsed))
    },
    setGroupOpen(path, open) {
      this.openGroups[path] = open
    }
  }
})
