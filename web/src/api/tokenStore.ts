import type { TokenPair } from '../types/auth'

/**
 * Token 存放层（安全权衡见 `web/README.md`「Token 持久化」段）：
 *
 * - accessToken 只驻内存，不落任何持久化存储；刷新页面即丢，由 refreshToken 静默轮转重建
 * - refreshToken 落 localStorage，以支撑刷新与多标签页保持会话（后端不发 httpOnly Cookie，前端不改 Java）
 */
const REFRESH_TOKEN_KEY = 'zen.admin.refresh-token'

let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

/** 轮转后必须整体替换：旧 refresh 当场进黑名单，留着旧值下一次必然 401 */
export function setTokens(pair: TokenPair): void {
  accessToken = pair.accessToken
  localStorage.setItem(REFRESH_TOKEN_KEY, pair.refreshToken)
}

export function clearTokens(): void {
  accessToken = null
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}
