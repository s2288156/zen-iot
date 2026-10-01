import type { TokenPair, UserProfileView } from '../types/auth'
import { request } from './http'

/** `POST /auth/login`：后端签发的 TokenPair 由调用方（auth store）落盘 */
export function login(username: string, password: string): Promise<TokenPair> {
  return request<TokenPair>({ path: '/auth/login', method: 'POST', body: { username, password }, authAction: true })
}

/** `POST /auth/logout`：需同时携带 access（请求上下文撤销）与 refresh（入参撤销），缺一不可 */
export function logout(refreshToken: string): Promise<void> {
  return request<void>({ path: '/auth/logout', method: 'POST', body: { refreshToken } })
}

export function getMe(): Promise<UserProfileView> {
  return request<UserProfileView>({ path: '/auth/me' })
}

/** `POST /auth/change-password`：新口令后端约束 8~72 位；旧口令不符返回 400 */
export function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  return request<void>({ path: '/auth/change-password', method: 'POST', body: { oldPassword, newPassword } })
}
