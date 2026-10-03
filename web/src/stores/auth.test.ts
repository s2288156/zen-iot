import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/errors'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '../api/tokenStore'
import type { ModuleCode, RoleView, TokenPair, UserProfileView } from '../types/auth'
import { useAuthStore } from './auth'

const api = vi.hoisted(() => ({
  getMe: vi.fn(),
  getRole: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  // 捕获 store 注册的会话过期通知，用于验证轮转失败后状态确实回落
  expiredHandlers: [] as (() => void)[],
}))

vi.mock('../api/auth', () => ({ getMe: api.getMe, login: api.login, logout: api.logout }))
vi.mock('../api/roles', () => ({ getRole: api.getRole }))
vi.mock('../api/http', () => ({
  setSessionExpiredHandler: (handler: () => void) => {
    api.expiredHandlers.push(handler)
  },
}))

const TOKENS: TokenPair = { accessToken: 'A1', refreshToken: 'R1' }

function profile(roleIds: number[]): UserProfileView {
  return { id: 1, username: 'wu', nickname: '吴', email: null, phone: null, avatar: null, status: 1, roleIds }
}

function role(id: number, modules: ModuleCode[]): RoleView {
  return {
    id,
    roleCode: `r${id}`,
    roleName: `角色${id}`,
    description: null,
    modules,
    creator: null,
    createTime: null,
    updater: null,
    updateTime: null,
  }
}

beforeEach(() => {
  vi.resetAllMocks()
  clearTokens()
  localStorage.clear()
  useAuthStore.setState({ status: 'idle', user: null, modules: [] })
})

describe('bootstrap：以 /auth/me 装配会话', () => {
  it('roleIds 逐个取角色后求 modules 并集并去重', async () => {
    api.getMe.mockResolvedValue(profile([1, 2]))
    api.getRole.mockImplementation(async (id: number) => role(id, id === 1 ? ['admin', 'ecs'] : ['ecs', 'wcs']))

    await useAuthStore.getState().bootstrap()

    const state = useAuthStore.getState()
    expect(state.status).toBe('authenticated')
    expect(state.user).toEqual(profile([1, 2]))
    expect(state.modules).toEqual(['admin', 'ecs', 'wcs'])
    expect(api.getRole.mock.calls.map((call) => call[0])).toEqual([1, 2])
  })

  it('无角色时 modules 为空且不发起角色请求', async () => {
    api.getMe.mockResolvedValue(profile([]))

    await useAuthStore.getState().bootstrap()

    expect(useAuthStore.getState().modules).toEqual([])
    expect(useAuthStore.getState().status).toBe('authenticated')
    expect(api.getRole).not.toHaveBeenCalled()
  })

  it('装配失败回落未登录（无 Token / 轮转失败 / 网络故障同一路径）', async () => {
    api.getMe.mockRejectedValue(new ApiError(401, '未认证或登录已失效'))

    await useAuthStore.getState().bootstrap()

    expect(useAuthStore.getState()).toMatchObject({ status: 'anonymous', user: null, modules: [] })
  })

  it('并发与 StrictMode 双跑只装配一次', async () => {
    api.getMe.mockResolvedValue(profile([]))

    await Promise.all([useAuthStore.getState().bootstrap(), useAuthStore.getState().bootstrap()])
    await useAuthStore.getState().bootstrap()

    expect(api.getMe).toHaveBeenCalledTimes(1)
  })
})

describe('login：签发并落盘 TokenPair', () => {
  it('登录成功写入 Token 并进入已认证', async () => {
    api.login.mockResolvedValue(TOKENS)
    api.getMe.mockResolvedValue(profile([1]))
    api.getRole.mockResolvedValue(role(1, ['admin']))

    await useAuthStore.getState().login('wu', 'P@ssw0rd!')

    expect(api.login).toHaveBeenCalledWith('wu', 'P@ssw0rd!')
    expect(getAccessToken()).toBe('A1')
    expect(getRefreshToken()).toBe('R1')
    expect(useAuthStore.getState()).toMatchObject({ status: 'authenticated', modules: ['admin'] })
  })

  it('凭证错误抛出且不留任何 Token', async () => {
    api.login.mockRejectedValue(new ApiError(401, '用户名或密码错误'))

    await expect(useAuthStore.getState().login('wu', 'bad')).rejects.toMatchObject({
      code: 401,
      message: '用户名或密码错误',
    })
    expect(getAccessToken()).toBeNull()
    expect(getRefreshToken()).toBeNull()
    expect(api.getMe).not.toHaveBeenCalled()
  })

  it('装配失败时回落未登录并抛出；Token 保留，刷新页面后由 bootstrap 静默轮转自愈', async () => {
    api.login.mockResolvedValue(TOKENS)
    api.getMe.mockRejectedValue(new ApiError(503, '服务暂不可用'))

    await expect(useAuthStore.getState().login('wu', 'P@ssw0rd!')).rejects.toBeInstanceOf(ApiError)

    expect(useAuthStore.getState().status).toBe('anonymous')
    expect(getRefreshToken()).toBe('R1')
  })
})

describe('logout：撤销并断本地会话', () => {
  it('以 refreshToken 调用撤销接口后清干净', async () => {
    setTokens(TOKENS)
    api.logout.mockResolvedValue(undefined)

    await useAuthStore.getState().logout()

    expect(api.logout).toHaveBeenCalledWith('R1')
    expect(getAccessToken()).toBeNull()
    expect(getRefreshToken()).toBeNull()
    expect(useAuthStore.getState()).toMatchObject({ status: 'anonymous', user: null, modules: [] })
  })

  it('撤销失败也断本地会话，并把服务端撤销未成功的事实抛给调用方', async () => {
    setTokens(TOKENS)
    api.logout.mockRejectedValue(new ApiError(503, '服务暂不可用'))

    await expect(useAuthStore.getState().logout()).rejects.toBeInstanceOf(ApiError)

    expect(getAccessToken()).toBeNull()
    expect(getRefreshToken()).toBeNull()
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('本地没有 refreshToken 时不打撤销接口', async () => {
    api.logout.mockResolvedValue(undefined)

    await useAuthStore.getState().logout()

    expect(api.logout).not.toHaveBeenCalled()
    expect(useAuthStore.getState().status).toBe('anonymous')
  })
})

describe('会话过期通知接线', () => {
  it('store 注册唯一通知入口，http 层轮转失败后状态回落未登录', async () => {
    expect(api.expiredHandlers).toHaveLength(1)
    const notify = api.expiredHandlers[0]
    if (notify === undefined) throw new Error('auth store 未注册会话过期通知')

    useAuthStore.setState({ status: 'authenticated', user: profile([1]), modules: ['admin'] })
    notify()

    expect(useAuthStore.getState()).toMatchObject({ status: 'anonymous', user: null, modules: [] })
  })
})
