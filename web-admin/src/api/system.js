import request, { silent } from './request'

/* 权限管理（RBAC）—— 对应后端 system 模块 */
export function userList(params) {
  return request({ url: '/system/users', method: 'get', params })
}
export function createUser(data) {
  return request({ url: '/system/users', method: 'post', data })
}
export function updateUser(id, data) {
  return request({ url: `/system/users/${id}`, method: 'put', data })
}
export function deleteUser(id) {
  return request({ url: `/system/users/${id}`, method: 'delete' })
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
export function createRole(data) {
  return request({ url: '/system/roles', method: 'post', data })
}
export function updateRole(id, data) {
  return request({ url: `/system/roles/${id}`, method: 'put', data })
}
export function deleteRole(id) {
  return request({ url: `/system/roles/${id}`, method: 'delete' })
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
export function createMenu(data) {
  return request({ url: '/system/menus', method: 'post', data })
}
export function updateMenu(id, data) {
  return request({ url: `/system/menus/${id}`, method: 'put', data })
}
export function deleteMenu(id) {
  return request({ url: `/system/menus/${id}`, method: 'delete' })
}

export function operLogs(params) {
  return silent({ url: '/system/logs', method: 'get', params })
}

/* ----------------------------- 导入 / 导出（CSV，带鉴权） ----------------------------- */

function authHeaders() {
  const token = localStorage.getItem('bd_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/** 下载 CSV 导出文件（带 token，触发浏览器下载） */
export async function downloadCsv(url, filename) {
  const base = import.meta.env.VITE_API_BASE || '/api'
  const resp = await fetch(base + url, { headers: authHeaders() })
  if (!resp.ok) throw new Error('导出失败：HTTP ' + resp.status)
  const blob = await resp.blob()
  const href = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = href
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(href)
}

/** 上传 CSV 文件导入（multipart/form-data，带 token） */
export async function uploadCsv(url, file) {
  const base = import.meta.env.VITE_API_BASE || '/api'
  const form = new FormData()
  form.append('file', file)
  const resp = await fetch(base + url, { method: 'POST', headers: authHeaders(), body: form })
  if (!resp.ok) throw new Error('导入失败：HTTP ' + resp.status)
  return resp.json()
}

export function exportUsers() {
  return downloadCsv('/system/users/export', '用户数据.csv')
}
export function importUsers(file) {
  return uploadCsv('/system/users/import', file)
}
export function exportRoles() {
  return downloadCsv('/system/roles/export', '角色数据.csv')
}
export function importRoles(file) {
  return uploadCsv('/system/roles/import', file)
}
export function exportMenus() {
  return downloadCsv('/system/menus/export', '菜单数据.csv')
}

/* ----------------------------- 部门管理（按区域划分） ----------------------------- */

export function departmentList() {
  return request({ url: '/system/departments', method: 'get' })
}
export function departmentTree() {
  return request({ url: '/system/departments/tree', method: 'get' })
}
export function createDepartment(data) {
  return request({ url: '/system/departments', method: 'post', data })
}
export function updateDepartment(id, data) {
  return request({ url: `/system/departments/${id}`, method: 'put', data })
}
export function deleteDepartment(id) {
  return request({ url: `/system/departments/${id}`, method: 'delete' })
}
