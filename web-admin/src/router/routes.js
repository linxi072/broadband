/**
 * 导航与路由「单一事实来源」。
 * 后端 sys_menu 表按同样的 path / perm 初始化，保证前端菜单与 RBAC 权限树一一对应。
 *
 * 字段：title 名称 ｜ path 路径 ｜ icon 图标 ｜ perm 权限码 ｜ component 懒加载组件
 */

export const navConfig = [
  {
    title: '数据看板',
    path: '/dashboard',
    icon: '📊',
    perm: 'dashboard:view',
    component: () => import('@/views/dashboard/index.vue')
  },
  {
    title: '订单管理',
    path: '/order',
    icon: '📋',
    perm: 'order:view',
    component: () => import('@/views/order/list.vue')
  },
  {
    title: '客户管理',
    path: '/customer',
    icon: '👤',
    perm: 'customer:view',
    component: () => import('@/views/customer/list.vue')
  },
  {
    title: '套餐管理',
    icon: '📦',
    children: [
      {
        title: '套餐列表',
        path: '/package',
        perm: 'package:view',
        component: () => import('@/views/package/list.vue')
      },
      {
        title: '新增 / 编辑套餐',
        path: '/package/edit',
        perm: 'package:edit',
        routePath: '/package/edit/:id?',
        component: () => import('@/views/package/edit.vue')
      },
      {
        title: '营销看板',
        path: '/package/marketing',
        perm: 'package:view',
        component: () => import('@/views/package/marketing.vue')
      }
    ]
  },
  {
    title: '套餐升级',
    path: '/package/upgrade',
    icon: '⚡',
    perm: 'upgrade:view',
    component: () => import('@/views/package/upgrade.vue')
  },
  {
    title: '小区覆盖管理',
    icon: '🏘',
    children: [
      {
        title: '小区列表',
        path: '/community',
        perm: 'community:view',
        component: () => import('@/views/community/list.vue')
      },
      {
        title: '新增 / 编辑覆盖',
        path: '/community/edit',
        perm: 'community:edit',
        routePath: '/community/edit/:id?',
        component: () => import('@/views/community/edit.vue')
      }
    ]
  },
  {
    title: '安装工单',
    icon: '🔧',
    children: [
      {
        title: '工单池',
        path: '/workorder/pool',
        perm: 'workorder:view',
        component: () => import('@/views/workorder/pool.vue')
      },
      {
        title: '派单调度',
        path: '/workorder/dispatch',
        perm: 'dispatch:run',
        component: () => import('@/views/workorder/dispatch.vue')
      },
      {
        title: '容量配置',
        path: '/workorder/capacity',
        perm: 'capacity:config',
        component: () => import('@/views/workorder/capacity.vue')
      },
      {
        title: '调度规则',
        path: '/workorder/rules',
        perm: 'capacity:config',
        component: () => import('@/views/workorder/rules.vue')
      }
    ]
  },
  {
    title: '装维 SLA 与赔付',
    path: '/sla',
    icon: '🛡',
    perm: 'sla:view',
    component: () => import('@/views/sla/index.vue')
  },
  {
    title: '流量监控',
    path: '/traffic',
    icon: '📶',
    perm: 'traffic:view',
    component: () => import('@/views/traffic/index.vue')
  },
  {
    title: '投诉与评价',
    path: '/review',
    icon: '⭐',
    perm: 'review:view',
    component: () => import('@/views/review/index.vue')
  },
  {
    title: '销售管理',
    path: '/sales',
    icon: '💼',
    perm: 'sales:view',
    component: () => import('@/views/sales/index.vue')
  },
  {
    title: '财务管理',
    path: '/finance',
    icon: '💰',
    perm: 'finance:view',
    component: () => import('@/views/finance/index.vue')
  },
  {
    title: '权限管理',
    icon: '🔐',
    children: [
      {
        title: '用户管理',
        path: '/system/user',
        perm: 'system:user',
        component: () => import('@/views/system/user.vue')
      },
      {
        title: '角色管理',
        path: '/system/role',
        perm: 'system:role',
        component: () => import('@/views/system/role.vue')
      },
      {
        title: '菜单权限',
        path: '/system/menu',
        perm: 'system:menu',
        component: () => import('@/views/system/menu.vue')
      },
      {
        title: '操作日志',
        path: '/system/log',
        perm: 'system:log',
        component: () => import('@/views/system/log.vue')
      }
    ]
  },
  {
    title: '性能监控',
    path: '/monitor',
    icon: '📈',
    perm: 'monitor:view',
    component: () => import('@/views/monitor/index.vue')
  }
]

/** 拍平为路由表（父级仅用于分组，不产生嵌套 router-view） */
export const flatRoutes = navConfig
  .flatMap((item) => (item.children ? item.children : [item]))
  .map((leaf) => ({
    path: leaf.routePath || leaf.path,
    name: leaf.path.replace(/\//g, '_').replace(/:/g, ''),
    component: leaf.component,
    meta: { title: leaf.title, perm: leaf.perm }
  }))

/** 首页路径（登录后跳转） */
export const HOME_PATH = '/dashboard'
