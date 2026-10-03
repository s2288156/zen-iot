import { Flex, Spin } from 'antd'
import { useEffect, type ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { ForbiddenPage } from '../components/Forbidden'
import { useAuthStore } from '../stores/auth'
import type { ModuleCode } from '../types/auth'

function SessionPending() {
  return (
    <Flex justify="center" align="center" style={{ minHeight: '50vh' }}>
      <Spin />
    </Flex>
  )
}

/** 第一层守卫：会话未建立一律跳 /login；bootstrap 期间不渲染受保护内容 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const status = useAuthStore((state) => state.status)
  const bootstrap = useAuthStore((state) => state.bootstrap)
  const location = useLocation()

  useEffect(() => {
    if (status === 'idle') void bootstrap()
  }, [status, bootstrap])

  if (status === 'authenticated') return <>{children}</>
  if (status === 'anonymous') {
    // from 供登录成功后回跳原目标（Task 5.1 消费）
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  }
  return <SessionPending />
}

/** 第二层守卫：按 ModuleCode 并集拦截，无权限渲染 403 而非重定向 */
export function RequireModule({ code, children }: { code: ModuleCode; children: ReactNode }) {
  const allowed = useAuthStore((state) => state.modules.includes(code))
  return allowed ? <>{children}</> : <ForbiddenPage />
}

/** 落地页：无 admin 模块的账号退到个人中心，避免首屏即 403 */
export function DefaultHome() {
  const hasAdmin = useAuthStore((state) => state.modules.includes('admin'))
  return <Navigate to={hasAdmin ? '/users' : '/profile'} replace />
}
