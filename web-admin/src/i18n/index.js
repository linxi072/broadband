// 轻量 i18n 框架（US-3.4 · v1.20 脚手架）
// 设计目标：零三方依赖、可离线构建；以 key 路径查字典，支持 {param} 占位符；
// 选择持久化到 localStorage，刷新后保持；未翻译 key 回退中文并最终回退原 key（便于发现漏翻）。
import { reactive, computed } from 'vue'
import zhCN from './zh-CN'
import enUS from './en-US'

const messages = {
  'zh-CN': zhCN,
  'en-US': enUS
}

const STORAGE_KEY = 'bb_locale'

const state = reactive({
  locale: localStorage.getItem(STORAGE_KEY) || 'zh-CN'
})

function lookup(dict, key) {
  if (!dict) return undefined
  return key.split('.').reduce((o, k) => (o != null && typeof o === 'object' ? o[k] : undefined), dict)
}

function translate(key, params) {
  const dict = messages[state.locale] || messages['zh-CN']
  let val = lookup(dict, key)
  if (val == null) val = lookup(messages['zh-CN'], key)
  if (val == null) return key
  if (params && typeof val === 'string') {
    return val.replace(/\{(\w+)\}/g, (m, p) => (params[p] != null ? String(params[p]) : m))
  }
  return val
}

function setLocale(l) {
  if (!messages[l]) return
  state.locale = l
  localStorage.setItem(STORAGE_KEY, l)
  document.documentElement.setAttribute('lang', l === 'zh-CN' ? 'zh-CN' : 'en')
}

// 初始化 document lang
document.documentElement.setAttribute('lang', state.locale === 'zh-CN' ? 'zh-CN' : 'en')

export function useI18n() {
  return {
    locale: computed(() => state.locale),
    t: translate,
    setLocale
  }
}

export { translate as t, setLocale }
