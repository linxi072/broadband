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

/** 与后端连通性探测（登录页用于显示「后端在线/离线」） */
export function ping() {
  return silent({ url: '/sla/rules', method: 'get' })
}
