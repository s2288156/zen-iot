/**
 * `ModuleCode` 枚举的字符串值（`common-security` `ModuleCode#getCode()`）：库里存的就是这四个小写码。
 * wcs / rcs 暂无后端服务，但仍属合法枚举值。
 */
export const MODULE_CODES = ['admin', 'wcs', 'rcs', 'ecs'] as const

export type ModuleCode = (typeof MODULE_CODES)[number]

/**
 * 一次登录签发的双 Token（`common-security` `TokenPair`）。
 * 后端**不返回** `expiresIn`，前端无法提前续期，只能在 access 失效收到 401 时轮转。
 */
export interface TokenPair {
  accessToken: string
  refreshToken: string
}

/** `GET /auth/me` 的 `UserProfileView`；`roleIds` 是角色 ID（升序），不含 modules */
export interface UserProfileView {
  id: number
  username: string
  nickname: string | null
  email: string | null
  phone: string | null
  avatar: string | null
  /** 1 启用 / 0 禁用 */
  status: number
  roleIds: number[]
}

/** `GET /roles/{id}` 的 `RoleView`；modules 为字典序的模块码集合 */
export interface RoleView {
  id: number
  roleCode: string
  roleName: string
  description: string | null
  modules: ModuleCode[]
  creator: string | null
  createTime: string | null
  updater: string | null
  updateTime: string | null
}
