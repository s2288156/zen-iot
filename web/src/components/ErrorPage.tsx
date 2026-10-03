import { Button } from 'antd'
import { isRouteErrorResponse } from 'react-router'
import { ApiError } from '../api/errors'
import { StatusPage } from './StatusPage'

export interface ErrorPageProps {
  /** 捕获到的原始异常：`ApiError` / 路由 `ErrorResponse` / 任意 `Error` 都走同一套文案归一 */
  error: unknown
  /** 重试动作：错误边界传「重置边界」，路由层传「刷新页面」，`PageTable` 传「重新发请求」 */
  onRetry: () => void
  /** 回首页：只有处于 Router 上下文里的调用方（`RouteErrorView`）才给，错误边界给了会踩 `useNavigate` */
  onBackHome?: () => void
}

/** 面向用户的错误摘要：后端语义码值得露出，未知异常只给一句口径，不把堆栈甩到页面上 */
function describe(error: unknown): string {
  if (error instanceof ApiError) return `${error.code} · ${error.message}`
  if (isRouteErrorResponse(error)) return `${error.status} ${error.statusText}`
  if (error instanceof Error) return error.message
  return '发生未预期的异常，请重试或联系管理员。'
}

/** 404 由路由兜底时也是一次「错误」，但状态页该给叉号以外的语义，故按码值分派 */
function statusFor(error: unknown): '404' | 'error' {
  return isRouteErrorResponse(error) && error.status === 404 ? '404' : 'error'
}

/**
 * 运行时错误状态页：两层错误处理（路由 `errorElement` + React 错误边界）共用的同一份呈现（口径 C）。
 * 本体只依赖 props，不读 Router 上下文——`ErrorBoundary` 位于 `RouterProvider` 之外，那里没有导航能力。
 */
export function ErrorPage({ error, onRetry, onBackHome }: ErrorPageProps) {
  const status = statusFor(error)

  return (
    <StatusPage
      status={status}
      title={status === '404' ? '页面不存在' : '页面出错了'}
      subTitle={describe(error)}
      extra={
        <>
          <Button type="primary" onClick={onRetry}>
            重试
          </Button>
          {onBackHome === undefined ? null : <Button onClick={onBackHome}>返回首页</Button>}
        </>
      }
    />
  )
}
