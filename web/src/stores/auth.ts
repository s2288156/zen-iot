import { create } from 'zustand'
import { getMe, login as apiLogin, logout as apiLogout } from '../api/auth'
import { setSessionExpiredHandler } from '../api/http'
import { getRole } from '../api/roles'
import { clearTokens, getRefreshToken, setTokens } from '../api/tokenStore'
import type { ModuleCode, UserProfileView } from '../types/auth'

/** `idle` 尚未 bootstrap；`bootstrapping` 进行中；`authenticated` 已有会话；`anonymous` 未登录 */
export type AuthStatus = 'idle' | 'bootstrapping' | 'authenticated' | 'anonymous'

interface AuthState {
  status: AuthStatus
  user: UserProfileView | null
  modules: ModuleCode[]
  bootstrap: () => Promise<void>
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

/** 未登录态的固定切片：显式标注，避免 `as const` 把 `modules` 收成 readonly 而塞不进 store */
const anonymous: Pick<AuthState, 'status' | 'user' | 'modules'> = { status: 'anonymous', user: null, modules: [] }

/**
 * 权限数据源只用既有接口拼：`/auth/me` 给 `roleIds`，逐个 `/roles/{id}` 取 `modules` 求并集。
 * `Promise.all` 足够——角色逻辑删除会被「仍被用户引用」的 409 拦住，故 `roleIds` 里的角色必然可查。
 */
async function loadSession(): Promise<{ user: UserProfileView; modules: ModuleCode[] }> {
  const user = await getMe()
  const roles = await Promise.all(user.roleIds.map((id) => getRole(id)))
  return { user, modules: [...new Set(roles.flatMap((role) => role.modules))] }
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  status: 'idle',
  user: null,
  modules: [],

  bootstrap: async () => {
    // StrictMode 会把 effect 连跑两次：先同步占位，保证 bootstrap 幂等
    if (get().status !== 'idle') return
    set({ status: 'bootstrapping' })
    try {
      set({ ...(await loadSession()), status: 'authenticated' })
    } catch {
      // 建不起会话就是未登录（无 Token、轮转失败、网络故障皆如此），交给 RequireAuth 跳 /login
      set(anonymous)
    }
  },

  login: async (username, password) => {
    setTokens(await apiLogin(username, password))
    try {
      set({ ...(await loadSession()), status: 'authenticated' })
    } catch (error) {
      set(anonymous)
      throw error
    }
  },

  logout: async () => {
    const refreshToken = getRefreshToken()
    try {
      if (refreshToken !== null) await apiLogout(refreshToken)
    } finally {
      // 撤销失败也要断本地会话，否则 Token 仍被注入，用户以为已登出
      clearTokens()
      set(anonymous)
    }
  },
}))

// 轮转失败时 http 层只清 Token，会话状态在这里回落；跳 /login 由 RequireAuth 响应式完成，
// 不做命令式导航，以免 api 层反向依赖 router（未被守卫订阅的场景本就不存在受保护页面）。
setSessionExpiredHandler(() => useAuthStore.setState(anonymous))
