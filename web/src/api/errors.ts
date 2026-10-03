/** 后端 `ApiResponse` 非成功码归一化出的错误：`code` 为后端语义码，`message` 可直接展示给用户。 */
export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/** 请求没能抵达后端（超时或网络中断）：此时没有可信的 `code`，只有面向用户的口径。 */
export class NetworkError extends Error {
  readonly timedOut: boolean

  constructor(message: string, timedOut: boolean) {
    super(message)
    this.name = 'NetworkError'
    this.timedOut = timedOut
  }
}

/**
 * 把任意 catch 到的值收成一句可直接展示的文案。
 * `ApiError`/`NetworkError` 的 message 已经是面向用户的口径（`http.ts#readEnvelope` 归一化过），
 * 其余情况一律给 `fallback`，避免把 `Error` 的英文栈信息糊到界面上。
 */
export function errorText(error: unknown, fallback = '操作失败'): string {
  if (error instanceof ApiError || error instanceof NetworkError) return error.message
  return fallback
}
