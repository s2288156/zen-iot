import { request } from './http'

/** 全部接口都带 `@RequireModule(ModuleCode.ADMIN)` */

/**
 * `DELETE /sessions/{sessionId}`：撤销该会话的 access 与 refresh Token（写 Redis 黑名单）。
 *
 * 后端按**调用者的 userId** 拒绝，而不是按「是否当前这条会话」：下线自己名下任何会话都会
 * 400「不能强制下线自己的会话」，退出登录请走 `POST /auth/logout`。
 * 会话已过期或不存在 → 404「会话不存在或已下线: {sessionId}」。
 * 响应是 `ApiResponse<Void>`，信封里没有 `data` 键。
 */
export function kickoutSession(sessionId: string): Promise<void> {
  return request<void>({ path: `/sessions/${sessionId}`, method: 'DELETE' })
}
