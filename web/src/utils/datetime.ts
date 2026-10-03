/** 空值在表格里的占位符：antd 的 `—` 惯例，比空单元格更容易看出「这行确实没有值」 */
export const EMPTY_TEXT = '—'

/**
 * 把后端时间字符串格式化成 `YYYY-MM-DD HH:mm:ss`（浏览器本地时区）。
 *
 * 后端有两种口径，`new Date()` 都能正确解析，故不需要分支：
 * `user.create_time` 一类的 `LocalDateTime` 序列化成 `2026-09-24T05:28:11`（无偏移，按本地时区解读，
 * 与服务器写入时的墙钟一致）；`SessionView.issueTime` 一类的 `Instant` 序列化成
 * `2026-10-01T02:24:00.819657898Z`（UTC，转本地显示）。
 *
 * 项目没有直接依赖 dayjs（antd 内部那份不对外暴露），四则运算足够，不值得为此加一个依赖。
 */
export function formatDateTime(value: string | null | undefined): string {
  if (value === null || value === undefined || value === '') return EMPTY_TEXT
  const date = new Date(value)
  // 后端换了时间格式或库里存了脏值：原样透出，好过显示 `Invalid Date` 让人以为是前端崩了
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number): string => String(n).padStart(2, '0')
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}
