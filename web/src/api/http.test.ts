import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { TokenPair } from '../types/auth'
import { login } from './auth'
import { ApiError, NetworkError } from './errors'
import { request, setSessionExpiredHandler } from './http'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './tokenStore'

interface FakeResponse {
  status: number
  ok: boolean
  text: () => Promise<string>
}

interface FetchCall {
  url: string
  headers: Record<string, string>
}

/** 客户端只消费 status / ok / text()，用最小假响应避免依赖运行环境的 Response 实现 */
function ok(data: unknown): FakeResponse {
  return respond(200, { code: 200, message: '成功', data })
}

function fail(code: number, message: string): FakeResponse {
  return respond(code, { code, message })
}

function respond(status: number, body: unknown): FakeResponse {
  return { status, ok: status >= 200 && status < 300, text: () => Promise.resolve(JSON.stringify(body)) }
}

function raw(status: number, text: string): FakeResponse {
  return { status, ok: status >= 200 && status < 300, text: () => Promise.resolve(text) }
}

const calls: FetchCall[] = []

function mockFetch(handler: (url: string) => Promise<FakeResponse>) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown, init?: unknown) => {
      const url = String(input)
      const headers = ((init as { headers?: Record<string, string> } | undefined)?.headers ?? {}) as Record<
        string,
        string
      >
      calls.push({ url, headers })
      return handler(url)
    }),
  )
}

function bearerOf(index: number): string | undefined {
  return calls[index]?.headers.Authorization
}

beforeEach(() => {
  calls.length = 0
  clearTokens()
  localStorage.clear()
  setSessionExpiredHandler(() => undefined)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ApiResponse 解包与错误口径', () => {
  it('成功码 200 解包出 data，并带上网关前缀', async () => {
    mockFetch(async () => ok({ id: 7, username: 'wu' }))

    await expect(request<{ id: number; username: string }>({ path: '/auth/me' })).resolves.toEqual({
      id: 7,
      username: 'wu',
    })
    expect(calls[0].url).toBe('/api/admin/auth/me')
  })

  it('Void 接口省略 data 时解包为 undefined', async () => {
    mockFetch(async () => respond(200, { code: 200, message: '成功' }))

    await expect(
      request<void>({ path: '/auth/logout', method: 'POST', body: { refreshToken: 'R1' } }),
    ).resolves.toBeUndefined()
  })

  it('非成功码抛归一化 ApiError，保留 code 与 message', async () => {
    mockFetch(async () => fail(409, '角色仍被用户引用,无法删除'))

    await expect(request({ path: '/roles/1', method: 'DELETE' })).rejects.toMatchObject({
      name: 'ApiError',
      code: 409,
      message: '角色仍被用户引用,无法删除',
    })
  })

  it('代理裸 5xx 无 ApiResponse 体时按 HTTP 状态码归一化', async () => {
    mockFetch(async () => raw(502, '<html>Bad Gateway</html>'))

    const error = await request({ path: '/auth/me' }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe(502)
  })

  it('HTTP 200 但响应体不是信封时不得当成成功', async () => {
    mockFetch(async () => raw(200, '<html>login page</html>'))

    const error = await request({ path: '/auth/me' }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe(500)
  })

  it('query 参与拼接且跳过 undefined', async () => {
    mockFetch(async () => ok([]))

    await request<unknown[]>({ path: '/roles/page', query: { pageNum: 1, orderBy: undefined, roleName: 'op' } })
    expect(calls[0].url).toBe('/api/admin/roles/page?pageNum=1&roleName=op')
  })

  it('超时会话归一化为 NetworkError，与断网区分口径', async () => {
    mockFetch(async () => {
      throw new DOMException('timeout', 'TimeoutError')
    })
    const timeout = await request({ path: '/auth/me' }).catch((e: unknown) => e)
    expect(timeout).toBeInstanceOf(NetworkError)
    expect((timeout as NetworkError).timedOut).toBe(true)

    calls.length = 0
    mockFetch(async () => {
      throw new TypeError('Failed to fetch')
    })
    const offline = await request({ path: '/auth/me' }).catch((e: unknown) => e)
    expect(offline).toBeInstanceOf(NetworkError)
    expect((offline as NetworkError).timedOut).toBe(false)
  })
})

describe('Bearer 注入', () => {
  it('内存中有 accessToken 才注入 Authorization', async () => {
    mockFetch(async () => ok(null))

    await request({ path: '/roles/1' })
    expect(bearerOf(0)).toBeUndefined()

    setTokens({ accessToken: 'A1', refreshToken: 'R1' })
    await request({ path: '/roles/1' })
    expect(bearerOf(1)).toBe('Bearer A1')
  })
})

describe('401 → refresh 轮转单飞', () => {
  it('并发 401 只轮转一次，重放携带轮转后的新 access', async () => {
    setTokens({ accessToken: 'A1', refreshToken: 'R1' })
    let refreshCount = 0

    mockFetch(async (url) => {
      if (url.includes('/auth/refresh')) {
        refreshCount += 1
        await new Promise((resolve) => setTimeout(resolve, 10))
        const pair: TokenPair = { accessToken: 'A2', refreshToken: 'R2' }
        return ok(pair)
      }
      const current = calls[calls.length - 1].headers.Authorization
      if (current !== 'Bearer A2') return fail(401, '未认证或登录已失效')
      return ok(url.includes('/roles/1') ? ['admin', 'ecs'] : { id: 1 })
    })

    const [me, role] = await Promise.all([
      request<{ id: number }>({ path: '/auth/me' }),
      request<string[]>({ path: '/roles/1' }),
    ])

    expect(me).toEqual({ id: 1 })
    expect(role).toEqual(['admin', 'ecs'])
    expect(refreshCount).toBe(1)
    expect(getAccessToken()).toBe('A2')
    expect(getRefreshToken()).toBe('R2')

    const business = calls.filter((call) => !call.url.includes('/auth/refresh'))
    expect(business).toHaveLength(4)
    expect(business.slice(2).map((call) => call.headers.Authorization)).toEqual(['Bearer A2', 'Bearer A2'])
  })

  it('轮转失败清理会话并通知，原请求以 401 抛出', async () => {
    setTokens({ accessToken: 'A1', refreshToken: 'R1' })
    const expired = vi.fn()
    setSessionExpiredHandler(expired)

    mockFetch(async () => fail(401, '登录已失效，请重新登录'))

    await expect(request({ path: '/auth/me' })).rejects.toMatchObject({ code: 401, message: '登录已失效，请重新登录' })
    expect(expired).toHaveBeenCalledTimes(1)
    expect(getAccessToken()).toBeNull()
    expect(getRefreshToken()).toBeNull()
  })

  it('本地无 refresh Token 时不发起轮转请求', async () => {
    mockFetch(async () => fail(401, '未携带访问令牌'))

    await expect(request({ path: '/auth/me' })).rejects.toBeInstanceOf(ApiError)
    expect(calls.map((call) => call.url)).toEqual(['/api/admin/auth/me'])
  })

  it('重放仍 401 时不再二次轮转（防死循环）', async () => {
    setTokens({ accessToken: 'A1', refreshToken: 'R1' })
    let refreshCount = 0

    mockFetch(async (url) => {
      if (url.includes('/auth/refresh')) {
        refreshCount += 1
        return ok({ accessToken: 'A2', refreshToken: 'R2' } satisfies TokenPair)
      }
      return fail(401, '未认证或登录已失效')
    })

    await expect(request({ path: '/auth/me' })).rejects.toMatchObject({ code: 401 })
    expect(refreshCount).toBe(1)
  })

  it('登录接口的 401 是凭证错误，不触发轮转且 message 原样透传', async () => {
    setTokens({ accessToken: 'A1', refreshToken: 'R1' })
    mockFetch(async () => fail(401, '用户名或密码错误'))

    await expect(login('wu', 'bad')).rejects.toMatchObject({ code: 401, message: '用户名或密码错误' })
    expect(calls.map((call) => call.url)).toEqual(['/api/admin/auth/login'])
  })
})
