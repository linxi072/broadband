import { defineStore } from 'pinia'
import { login as loginApi, getMe, logout as logoutApi } from '@/api/auth'
import { ALL_PERMS, demoMenus } from '@/mock/fallback'

const TOKEN_KEY = 'bd_token'
const DEMO_TOKEN = 'demo-local-token'
const allowDemo = import.meta.env.VITE_ALLOW_DEMO_LOGIN === 'true'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: null,
    menus: [],
    loaded: false,
    demoMode: false
  }),

  getters: {
    roles: (s) => (s.profile && s.profile.roles) || [],
    perms: (s) => (s.profile && s.profile.perms) || [],
    isLogin: (s) => !!s.token,
    displayName: (s) => (s.profile && (s.profile.name || s.profile.username)) || '未登录'
  },

  actions: {
    setToken(t) {
      this.token = t || ''
      if (t) localStorage.setItem(TOKEN_KEY, t)
      else localStorage.removeItem(TOKEN_KEY)
    },

    async login(payload) {
      try {
        const res = await loginApi(payload)
        this.setToken(res.token)
        this.demoMode = false
        await this.fetchProfile()
        return res
      } catch (e) {
        // 后端不可达 / 鉴权接口尚未部署时，允许本地演示登录，便于前端独立预览
        const canFallback = allowDemo && (e.unreachable || e.status === 404 || e.status === 0)
        if (!canFallback) throw e
        this.demoLogin(payload)
        return { token: DEMO_TOKEN, demo: true }
      }
    },

    demoLogin(payload) {
      this.demoMode = true
      this.setToken(DEMO_TOKEN)
      this.profile = {
        id: 'demo',
        username: payload.username || 'admin',
        name: '演示账号',
        dept: '运营中心',
        roles: ['ADMIN'],
        roleNames: ['超级管理员'],
        perms: ALL_PERMS
      }
      this.menus = demoMenus
      this.loaded = true
    },

    async fetchProfile() {
      if (this.demoMode) {
        this.demoLogin({ username: (this.profile && this.profile.username) || 'admin' })
        return this.profile
      }
      const me = await getMe()
      const user = me.user || me
      this.profile = {
        ...user,
        roles: me.roles || user.roles || [],
        roleNames: me.roleNames || user.roleNames || [],
        perms: me.perms || user.perms || []
      }
      this.menus = me.menus || []
      this.loaded = true
      return this.profile
    },

    async logout() {
      try {
        if (!this.demoMode) await logoutApi()
      } catch (e) {
        /* 忽略：本地清理即可 */
      }
      this.reset()
    },

    reset() {
      this.profile = null
      this.menus = []
      this.loaded = false
      this.demoMode = false
      this.setToken('')
    }
  }
})
