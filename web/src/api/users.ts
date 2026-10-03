import type {
  UserCreateRequest,
  UserResetPasswordRequest,
  UserStatusRequest,
  UserUpdateRequest,
  UserView,
} from '../types/admin'
import { request } from './http'

/** 全部接口都带 `@RequireModule(ModuleCode.ADMIN)`，无 admin 模块的用户会得到 403「无访问权限」 */

/** `POST /users`：username 全库唯一，撞车 409 且 message 带已存在的用户名 */
export function createUser(body: UserCreateRequest): Promise<UserView> {
  return request<UserView>({ path: '/users', method: 'POST', body })
}

/** `GET /users/{id}`：不存在或已逻辑删除 → 404「用户不存在: {id}」 */
export function getUser(id: number): Promise<UserView> {
  return request<UserView>({ path: `/users/${id}` })
}

/** `PUT /users/{id}`：**全量覆盖**，请求体里没带或带 `null` 的字段都会被写成 NULL */
export function updateUser(id: number, body: UserUpdateRequest): Promise<UserView> {
  return request<UserView>({ path: `/users/${id}`, method: 'PUT', body })
}

/**
 * `PATCH /users/{id}/status`：1 启用 / 0 禁用。
 * 后端**没有自停保护**，禁用自己会留下活 Token 却再也登不进来，故调用方须自行挡住当前用户那一行。
 */
export function changeUserStatus(id: number, body: UserStatusRequest): Promise<UserView> {
  return request<UserView>({ path: `/users/${id}/status`, method: 'PATCH', body })
}

/**
 * `PUT /users/{id}/password`：管理员重置他人口令，成功后 `UserService#resetPassword` 会
 * `revokeAllForUser`，即**该用户的全部会话（含 refresh Token）立即下线**，须重新登录。
 * 响应是 `ApiResponse<Void>`，信封里没有 `data` 键。
 */
export function resetUserPassword(id: number, body: UserResetPasswordRequest): Promise<void> {
  return request<void>({ path: `/users/${id}/password`, method: 'PUT', body })
}

/**
 * `PUT /users/{id}/roles`：全量覆盖，`[]` 即清空；含不存在的角色 ID → 400「角色不存在: {id}」。
 * 请求体是**裸数组**（`@RequestBody Set<Long>`），包一层 `{ roleIds: [...] }` 会被 Jackson 拒成 400。
 */
export function assignUserRoles(id: number, roleIds: number[]): Promise<UserView> {
  return request<UserView>({ path: `/users/${id}/roles`, method: 'PUT', body: roleIds })
}

/** `DELETE /users/{id}`：逻辑删除，同样**没有自删保护**；响应是 `ApiResponse<Void>` */
export function deleteUser(id: number): Promise<void> {
  return request<void>({ path: `/users/${id}`, method: 'DELETE' })
}
