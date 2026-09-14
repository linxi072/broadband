import { useUserStore } from '@/store/user'

/**
 * v-perm="'order:create'"  单权限
 * v-perm="['a','b']"       任一命中即显示
 * v-role="'ADMIN'"         角色校验（与后端 ROLE_xxx 注解口径一致）
 */
function check(value) {
  const user = useUserStore()
  if (!value) return true
  const need = Array.isArray(value) ? value : [value]
  return need.some((p) => user.perms.includes(p))
}

export function setupPermissionDirective(app) {
  app.directive('perm', {
    mounted(el, binding) {
      if (!check(binding.value)) el.parentNode && el.parentNode.removeChild(el)
    }
  })
  app.directive('role', {
    mounted(el, binding) {
      const user = useUserStore()
      const need = Array.isArray(binding.value) ? binding.value : [binding.value]
      const ok = need.some((r) => user.roles.includes(r))
      if (!ok) el.parentNode && el.parentNode.removeChild(el)
    }
  })
}
