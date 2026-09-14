/**
 * 资源加载：优先真实接口，失败则降级到演示数据。
 * 返回 { data, live }，live=false 时页面显示「演示数据」标记。
 */
export async function loadResource(fetcher, fallback) {
  try {
    const data = await fetcher()
    if (data === null || data === undefined) throw new Error('empty response')
    return { data, live: true }
  } catch (e) {
    const data = typeof fallback === 'function' ? fallback() : fallback
    return { data, live: false, error: e }
  }
}

/** 换算百分比，容错除零 */
export function pct(used, total) {
  if (!total) return 0
  return Math.min(100, Math.round((used / total) * 100))
}

/** 金额格式化：¥1,286.00 */
export function money(v, digits = 0) {
  const n = Number(v || 0)
  return (
    '¥' +
    n.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
  )
}

/** 时间戳(ms) → yyyy-MM-dd HH:mm */
export function fmtTime(ms) {
  if (!ms) return '—'
  const d = new Date(Number(ms))
  const p = (x) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(
    d.getMinutes()
  )}`
}

/** 今日 yyyy-MM-dd */
export function today() {
  const d = new Date()
  const p = (x) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
