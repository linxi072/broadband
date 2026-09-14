import request, { silent } from './request'

/* 权限管理（RBAC）—— 对应后端 system 模块 */
export function userList(params) {
  return request({ url: '/system/users', method: 'get', params })
}
export function createUser(data) {
  return request({ url: '/system/users', method: 'post', data })
}
export function updateUserStatus(id, status) {
  return request({ url: `/system/users/${id}/status`, method: 'put', data: { status } })
}
export function assignRoles(id, roleIds) {
  return request({ url: `/system/users/${id}/roles`, method: 'put', data: { roleIds } })
}
export function resetPassword(id, password) {
  return request({ url: `/system/users/${id}/password`, method: 'put', data: { password } })
}

export function roleList() {
  return request({ url: '/system/roles', method: 'get' })
}
export function roleMenus(roleId) {
  return request({ url: `/system/roles/${roleId}/menus`, method: 'get' })
}
export function saveRoleMenus(roleId, menuIds) {
  return request({ url: `/system/roles/${roleId}/menus`, method: 'put', data: { menuIds } })
}

export function menuTree() {
  return request({ url: '/system/menus/tree', method: 'get' })
}
export function operLogs(params) {
  return silent({ url: '/system/logs', method: 'get', params })
}
