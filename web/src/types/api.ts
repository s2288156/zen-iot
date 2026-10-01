/**
 * 后端统一响应体，对应 `common-security` 的 `ApiResponse`。
 * `code` 是数字且与 HTTP 状态码同源（Servlet 侧 `GlobalExceptionHandler#build`、网关侧 `ApiJsonResponses#statusFor`
 * 走同一规则），成功码为 200；失败时 `data` 因 `@JsonInclude(NON_NULL)` 被省略。
 */
export interface ApiEnvelope<T> {
  code: number
  message: string
  data?: T
}
