/**
 * `ModuleCode` 枚举的字符串值（`common-security` `ModuleCode#getCode()`）：库里存的就是这四个小写码。
 * wcs / rcs 暂无后端服务，但仍属合法枚举值。
 */
export const MODULE_CODES = ['admin', 'wcs', 'rcs', 'ecs'] as const

export type ModuleCode = (typeof MODULE_CODES)[number]

/**
 * 模块码的中文显示名。后端 `ModuleCode` 只有 `getCode()`，**没有任何中文名**，也无 `/modules` 字典接口，
 * 所以这份映射只能由前端自维；语义取自根 README 的模块职责描述。
 * 后端新增模块码时，`Record<ModuleCode, string>` 会让这里在 typecheck 阶段直接报错，而不是静默漏显示。
 */
export const MODULE_LABELS: Record<ModuleCode, string> = {
  admin: '管理后台',
  wcs: '仓库控制',
  rcs: '机器人调度',
  ecs: '设备控制',
}

/** `Checkbox.Group` / `Select` 的模块候选项；顺序固定按 `MODULE_CODES`，与后端枚举声明序一致 */
export const MODULE_OPTIONS = MODULE_CODES.map((code) => ({ label: MODULE_LABELS[code], value: code }))

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
