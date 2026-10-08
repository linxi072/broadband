import { defineStore } from 'pinia'
import { householdList } from '@/api/business'
import { demoHouseholds } from '@/mock/fallback'
import { loadResource } from '@/composables/useResource'

/**
 * 多住宅切换（US-3.1）全局状态：
 * - currentId：当前选中的住宅（'' 表示「全部住宅」）
 * - households：住宅列表（供顶栏切换器与选项使用）
 * 选择持久化到 localStorage，刷新后保持。
 */
export const useHouseholdStore = defineStore('household', {
  state: () => ({
    currentId: localStorage.getItem('bd_household_id') || '',
    households: []
  }),
  getters: {
    current: (s) => s.households.find((h) => h.id === s.currentId) || null,
    options: (s) => [{ id: '', name: '全部住宅', all: true }, ...s.households]
  },
  actions: {
    setCurrent(id) {
      this.currentId = id || ''
      localStorage.setItem('bd_household_id', this.currentId)
    },
    async load() {
      const r = await loadResource(
        () => householdList({ page: 1, size: 200 }),
        () => demoHouseholds({})
      )
      const data = r.data
      this.households = Array.isArray(data) ? data : (data?.list || [])
    }
  }
})
