import axios from 'axios'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

const TOKEN_KEY = 'bd_token'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

let redirecting = false

service.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

service.interceptors.response.use(
  (res) => res.data,
  (error) => {
    const status = error.response && error.response.status
    const body = (error.response && error.response.data) || {}
    const url = (error.config && error.config.url) || ''
    const silent = error.config && error.config.silent
    const isLogin = url.indexOf('/auth/login') >= 0

    let msg = body.message || body.error || error.message || '请求失败'

    if (isLogin) {
      msg = status === 401 ? '账号或密码错误' : msg
    } else if (status === 401) {
      msg = '登录状态已失效，请重新登录'
      if (!redirecting) {
        redirecting = true
        localStorage.removeItem(TOKEN_KEY)
        setTimeout(() => {
          window.location.replace('/login')
          redirecting = false
        }, 500)
      }
    } else if (status === 403) {
      msg = body.message || '没有访问该功能的权限'
    } else if (status === 404) {
      msg = '接口不存在：' + url
    } else if (status >= 500) {
      msg = '服务端异常（' + status + '）'
    }

    if (!silent && !isLogin) ElMessage.error(msg)

    // 归一化错误，便于调用方决定是否降级到演示数据
    const normalized = new Error(msg)
    normalized.status = status || 0
    normalized.unreachable = !error.response
    normalized.raw = error
    return Promise.reject(normalized)
  }
)

/** 静默请求：失败不弹 toast（用于「读不到就降级演示数据」的场景） */
export function silent(config) {
  return service({ silent: true, ...config })
}

export default service
