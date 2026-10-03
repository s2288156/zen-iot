import type { RoleCreateRequest, RoleUpdateRequest } from '../types/admin'
import type { ModuleCode, RoleView } from '../types/auth'
import { request } from './http'

/** 除 `GET /roles/{id}` 外都带 `@RequireModule(ModuleCode.ADMIN)` */

/** `GET /roles/{id}`：登录即可读（无 `@RequireModule`），故可用于任意用户的权限 bootstrap */
export function getRole(id: number): Promise<RoleView> {
  return request<RoleView>({ path: `/roles/${id}` })
}

/** `POST /roles`：roleCode 全库唯一，撞车 409「角色编码已存在: {code}」 */
export function createRole(body: RoleCreateRequest): Promise<RoleView> {
  return request<RoleView>({ path: '/roles', method: 'POST', body })
}

/** `PUT /roles/{id}`：只改 roleName/description，**不含 modules**；description 传 `''` 会存成空串 */
export function updateRole(id: number, body: RoleUpdateRequest): Promise<RoleView> {
  return request<RoleView>({ path: `/roles/${id}`, method: 'PUT', body })
}

/**
 * `PUT /roles/{id}/modules`：全量覆盖，`[]` 即收回全部模块。
 * 模块码**大小写敏感**，非枚举值 → 400「非法模块编码: {code}」。
 * 角色的 modules 只在登录时被读进 Token 快照，改完后已登录用户须重新登录才生效。
 * 请求体是**裸数组**（`@RequestBody Set<String>`），包一层 `{ modules: [...] }` 会被 Jackson 拒成 400。
 */
export function assignRoleModules(id: number, modules: ModuleCode[]): Promise<RoleView> {
  return request<RoleView>({ path: `/roles/${id}/modules`, method: 'PUT', body: modules })
}

/** `DELETE /roles/{id}`：仍被用户引用时 409「角色仍被用户引用,无法删除」 */
export function deleteRole(id: number): Promise<void> {
  return request<void>({ path: `/roles/${id}`, method: 'DELETE' })
}
