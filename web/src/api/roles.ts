import type { RoleView } from '../types/auth'
import { request } from './http'

/** `GET /roles/{id}`：登录即可读（无 `@RequireModule`），故可用于任意用户的权限 bootstrap */
export function getRole(id: number): Promise<RoleView> {
  return request<RoleView>({ path: `/roles/${id}` })
}
