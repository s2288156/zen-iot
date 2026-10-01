import { Outlet, createBrowserRouter } from 'react-router'
import { Layout } from './Layout'
import { DefaultHome, RequireAuth, RequireModule } from './guards'

// 两层守卫按 spec 落地：RequireAuth 挡未登录（跳 /login），RequireModule 挡无模块权限（渲染 403）。
// admin 域页面收进一个无路径 layout 路由统一包裹，lazy 机制不受影响；业务页仍以 lazy 装载（模块须具名导出 Component）。
const adminOnly = {
  element: (
    <RequireModule code="admin">
      <Outlet />
    </RequireModule>
  ),
  children: [
    { path: 'users', lazy: () => import('../features/users') },
    { path: 'roles', lazy: () => import('../features/roles') },
    { path: 'sessions', lazy: () => import('../features/sessions') },
  ],
}

export const router = createBrowserRouter([
  {
    path: '/login',
    lazy: () => import('../features/auth/LoginPage'),
  },
  {
    path: '/',
    element: (
      <RequireAuth>
        <Layout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <DefaultHome /> },
      adminOnly,
      // 个人中心对所有已登录账号开放：自助改密与登出不依赖模块授权
      { path: 'profile', lazy: () => import('../features/profile') },
      // 基调自检页（Task 2.3）：仅 dev 挂载，不进生产路由
      ...(import.meta.env.DEV ? [{ path: 'theme-self-check', lazy: () => import('../theme/ThemeSelfCheck') }] : []),
    ],
  },
])
