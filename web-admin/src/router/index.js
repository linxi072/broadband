import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import { flatRoutes, HOME_PATH } from './routes'
import { useUserStore } from '@/store/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: { public: true, title: '登录' }
    },
    {
      path: '/',
      component: Layout,
      redirect: HOME_PATH,
      children: [
        ...flatRoutes,
        {
          path: '/403',
          name: 'forbidden',
          component: () => import('@/views/error/403.vue'),
          meta: { title: '无访问权限' }
        },
        {
          path: '/404',
          name: 'notFound',
          component: () => import('@/views/error/404.vue'),
          meta: { title: '页面不存在' }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/404' }
  ]
})

router.beforeEach(async (to) => {
  const user = useUserStore()
  document.title = to.meta.title
    ? `${to.meta.title} · 宽带业务管理系统`
    : '宽带业务管理系统 · 运营后台'

  if (to.meta.public) {
    return user.isLogin ? { path: HOME_PATH } : true
  }

  if (!user.isLogin) {
    return { path: '/login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }

  if (!user.loaded) {
    try {
      await user.fetchProfile()
    } catch (e) {
      user.reset()
      return { path: '/login' }
    }
  }

  // 权限码校验（与后端 @PreAuthorize 同源）
  const perm = to.meta.perm
  if (perm && !user.perms.includes(perm)) {
    return { path: '/403' }
  }

  return true
})

export default router
