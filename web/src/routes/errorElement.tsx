import { useNavigate, useRouteError } from 'react-router'
import { ErrorPage } from '../components/ErrorPage'

/**
 * 路由层错误呈现（两层错误处理的第一层，口径 C）：接 `lazy` 模块装载失败、`loader`/`action` 抛错与未匹配路由。
 *
 * 这里的「重试」只能是整页刷新：路由错误往往来自装载失败的模块，组件树已经不可信，
 * 单纯重渲染不会重新发起 `import()`。回首页动作由本组件注入，`ErrorPage` 本体不碰 Router。
 */
export function RouteErrorView() {
  const error = useRouteError()
  const navigate = useNavigate()

  return (
    <ErrorPage
      error={error}
      onRetry={() => window.location.reload()}
      onBackHome={() => void navigate('/', { replace: true })}
    />
  )
}
