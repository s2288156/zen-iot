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

/** 排序方向：后端 `PageQuery#orderDirection` 只接受这两个字面值，缺省 `asc` */
export type OrderDirection = 'asc' | 'desc'

/**
 * 分页请求（`common-core` `PageQuery`）：`pageSize` 默认 10、上限 200（超出即参数校验失败）。
 * 参数名是 `orderBy` / `orderDirection`，可排序字段由各接口的白名单决定。
 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  orderBy?: string
  orderDirection?: OrderDirection
}

/**
 * 分页响应（`common-core` `PageResult`）：`total` / `pages` 后端为 `long`，经 JSON 落地即 `number`。
 * `pages` 是向上取整的总页数，前端分页控件按 `total` + `pageSize` 自算，不依赖它。
 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}
