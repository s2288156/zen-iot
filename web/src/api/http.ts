import type { ApiEnvelope } from '../types/api'
import type { TokenPair } from '../types/auth'
import { ApiError, NetworkError } from './errors'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './tokenStore'

/** 业务接口一律带网关前缀 `/api/{service}`；dev 由 Vite proxy 转发到 `localhost:28080` */
const API_BASE = '/api/admin'

/** `GlobalErrorCode.SUCCESS`；失败码与 HTTP 状态码同源，因此 body 的 code 总是权威判定依据 */
const SUCCESS_CODE = 200

const DEFAULT_TIMEOUT_MS = 10_000

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export interface RequestOptions {
  path: string
  method?: HttpMethod
  body?: unknown
  query?: Record<string, string | number | boolean | undefined>
  /**
   * 该请求本身就是认证动作（`/auth/login`、`/auth/refresh`）。
   * 凭证错误同样返回 HTTP 401，若不排除就会被判成「会话过期」去轮转：
   * 既拿不到「用户名或密码错误」的 message，`/auth/refresh` 还会自我递归。
   */
  authAction?: boolean
  timeoutMs?: number
}

let sessionExpiredHandler: () => void = () => undefined

/** 会话确认失效时的通知入口；由 auth store 注册，避免 api 层反向依赖 store / router */
export function setSessionExpiredHandler(handler: () => void): void {
  sessionExpiredHandler = handler
}

/** 轮转单飞：旧 refresh 一经使用立即失效，并发 401 必须共享同一份在途轮转 */
let rotation: Promise<void> | null = null

export async function request<T>(options: RequestOptions): Promise<T> {
  const first = await send(options)
  if (first.status !== 401 || options.authAction === true) return unwrap<T>(first)

  try {
    await rotateOnce()
  } catch {
    // 轮转失败（含 refresh 自身 401）：会话已清理，原样抛出首答的 401
    return unwrap(first)
  }
  return unwrap<T>(await send(options))
}

function rotateOnce(): Promise<void> {
  rotation ??= doRotate().finally(() => {
    rotation = null
  })
  return rotation
}

async function doRotate(): Promise<void> {
  const refreshToken = getRefreshToken()
  if (refreshToken === null) {
    expireSession()
    throw new ApiError(401, '未认证或登录已失效')
  }
  try {
    const pair = await unwrap<TokenPair>(
      await send({
        path: '/auth/refresh',
        method: 'POST',
        body: { refreshToken },
        authAction: true,
      }),
    )
    setTokens(pair)
  } catch (error) {
    expireSession()
    throw error
  }
}

function expireSession(): void {
  clearTokens()
  sessionExpiredHandler()
}

async function send(options: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const accessToken = getAccessToken()
  if (accessToken !== null) headers.Authorization = `Bearer ${accessToken}`
  const hasBody = options.body !== undefined
  if (hasBody) headers['Content-Type'] = 'application/json'

  try {
    return await fetch(buildUrl(options.path, options.query), {
      method: options.method ?? 'GET',
      headers,
      body: hasBody ? JSON.stringify(options.body) : undefined,
      signal: AbortSignal.timeout(options.timeoutMs ?? DEFAULT_TIMEOUT_MS),
    })
  } catch (cause) {
    const timedOut = cause instanceof DOMException && cause.name === 'TimeoutError'
    throw new NetworkError(timedOut ? '请求超时，请稍后重试' : '无法连接服务，请检查网络或后端是否就绪', timedOut)
  }
}

function buildUrl(path: string, query?: Record<string, string | number | boolean | undefined>): string {
  const url = `${API_BASE}${path}`
  if (query === undefined) return url
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined) search.set(key, String(value))
  }
  const qs = search.toString()
  return qs === '' ? url : `${url}?${qs}`
}

async function unwrap<T>(response: Response): Promise<T> {
  const envelope = await readEnvelope(response)
  if (envelope.code !== SUCCESS_CODE) throw new ApiError(envelope.code, envelope.message)
  return envelope.data as T
}

async function readEnvelope(response: Response): Promise<ApiEnvelope<unknown>> {
  const text = await response.text()
  if (text !== '') {
    try {
      const envelope = asEnvelope(JSON.parse(text) as unknown)
      if (envelope !== null) return envelope
    } catch {
      // 非 JSON：落到下面的状态码归一化
    }
  }
  // 代理/网关的裸 5xx 没有 ApiResponse 体，只能按 HTTP 状态码归一化
  if (response.ok) throw new ApiError(500, '服务响应格式异常')
  throw new ApiError(response.status, `服务响应异常（HTTP ${response.status}）`)
}

function asEnvelope(value: unknown): ApiEnvelope<unknown> | null {
  if (typeof value !== 'object' || value === null) return null
  const candidate = value as Partial<ApiEnvelope<unknown>>
  if (typeof candidate.code !== 'number' || typeof candidate.message !== 'string') return null
  return { code: candidate.code, message: candidate.message, data: candidate.data }
}
