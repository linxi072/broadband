import request, { silent } from './request'

/** 账号密码登录，返回 { token, expiresIn, user } */
export function login(payload) {
  return request({ url: '/auth/login', method: 'post', data: payload })
}

/** 当前登录者：用户信息 + 角色 + 权限码 + 菜单树 */
export function getMe() {
  return request({ url: '/auth/me', method: 'get' })
}

export function logout() {
  return silent({ url: '/auth/logout', method: 'post' })
}

/**
 * 自服务改密（T-02 安全治理 · 首登强制改密）。
 * 后端 POST /api/auth/change-password，入参 { oldPassword, newPassword }，
 * 成功返回 { ok: true, mustChangePassword: 0 }（服务端同时清零强制改密标记，过滤器随即放行）。
 */
export function changePassword(payload) {
  return request({ url: '/auth/change-password', method: 'post', data: payload })
}

/** 与后端连通性探测（登录页用于显示「后端在线/离线」） */
export function ping() {
  return silent({ url: '/sla/rules', method: 'get' })
}
