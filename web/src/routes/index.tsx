import { createBrowserRouter, Navigate } from 'react-router'
import { Layout } from './Layout'

// 路由骨架：全部业务页以 lazy 装载（模块须具名导出 Component）。
// RequireAuth / RequireModule 守卫随 Task 3 接入，本轮先不设防。
export const router = createBrowserRouter([
  {
    path: '/login',
    lazy: () => import('../features/auth/LoginPage'),
  },
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Navigate to="/users" replace /> },
      { path: 'users', lazy: () => import('../features/users') },
      { path: 'roles', lazy: () => import('../features/roles') },
      { path: 'sessions', lazy: () => import('../features/sessions') },
      { path: 'profile', lazy: () => import('../features/profile') },
      // 基调自检页（Task 2.3）：仅 dev 挂载，不进生产路由
      ...(import.meta.env.DEV ? [{ path: 'theme-self-check', lazy: () => import('../theme/ThemeSelfCheck') }] : []),
    ],
  },
])
