import type { ModuleCode } from './auth'

/**
 * `admin-service` 管理域视图与请求体，字段名逐字对齐 `/api/admin/v3/api-docs`。
 * 认证域的 `TokenPair` / `UserProfileView` / `RoleView` 在 `types/auth.ts`。
 */

/** `GET /users/page`、`GET /users/{id}` 的 `UserView` */
export interface UserView {
  id: number
  username: string
  nickname: string | null
  email: string | null
  phone: string | null
  avatar: string | null
  /** 1 启用 / 0 禁用 */
  status: number
  roleIds: number[]
  creator: string | null
  createTime: string | null
  updater: string | null
  updateTime: string | null
}

/**
 * `GET /sessions` 的 `SessionView`。
 * `sessionId` 是会话登记的独立 UUID（`SessionService#recordLogin` 生成），与 access/refresh 的 JWT `jti`
 * 无对应关系，后端也刻意不下发 `jti`，故前端**无法**凭它认出「本人当前会话」。
 */
export interface SessionView {
  sessionId: string
  userId: number
  username: string
  ip: string | null
  userAgent: string | null
  issueTime: string | null
  expireTime: string | null
}

/** `POST /users` 的 `UserCreateRequest`：`password` 8~72 是后端 `@Size` 口径 */
export interface UserCreateRequest {
  username: string
  password: string
  nickname: string | null
  email: string | null
  phone: string | null
  avatar: string | null
}

/**
 * `PUT /users/{id}` 的 `UserUpdateRequest`：**全量覆盖**，且不含 `username`（创建后不可改）。
 * 四个字段后端都只约束长度/格式而不约束非空，传 `''` 会原样存成空串而不是 NULL，
 * 因此类型上强制 `string | null`，由调用方把表单空值归一成 `null`。
 */
export interface UserUpdateRequest {
  nickname: string | null
  email: string | null
  phone: string | null
  avatar: string | null
}

/** `PATCH /users/{id}/status` 的 `UserStatusRequest`：1 启用 / 0 禁用 */
export interface UserStatusRequest {
  status: number
}

/** `PUT /users/{id}/password` 的 `UserResetPasswordRequest` */
export interface UserResetPasswordRequest {
  password: string
}

/** `POST /roles` 的 `RoleCreateRequest`：`modules` 缺省即空集（后端紧凑构造器归一） */
export interface RoleCreateRequest {
  roleCode: string
  roleName: string
  description: string | null
  modules: ModuleCode[]
}

/** `PUT /roles/{id}` 的 `RoleUpdateRequest`：不含 `roleCode`（不可改）也不含 `modules`（走独立接口） */
export interface RoleUpdateRequest {
  roleName: string
  description: string | null
}
