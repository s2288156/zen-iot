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
