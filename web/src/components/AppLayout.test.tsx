import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { RouteObject } from 'react-router'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { ApiError } from '../api/errors'
import { useAuthStore, type AuthStatus } from '../stores/auth'
import type { ModuleCode, UserProfileView } from '../types/auth'
import { ThemeProvider } from '../theme'
import { AppLayout } from './AppLayout'

const OUTLET = '内容区'

const user: UserProfileView = {
  id: 1,
  username: 'admin',
  nickname: '超级管理员',
  email: null,
  phone: null,
  avatar: null,
  status: 1,
  roleIds: [1],
}

const routes: RouteObject[] = [
  { path: '/login', element: <div>登录页</div> },
  { path: '/profile', element: <div>个人中心页</div> },
  { path: '/users', element: <div>用户管理页</div> },
  {
    path: '/shell',
    element: <AppLayout />,
    children: [
      { index: true, element: <div>{OUTLET}</div> },
      // 全屏开关只在路由 handle 上生效：测试夹具，生产路由刻意不给任何页面挂它
      { path: 'canvas', handle: { fullScreen: true }, element: <div>全屏画布</div> },
    ],
  },
]

function renderAt(path = '/shell') {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <ThemeProvider>
      <RouterProvider router={router} />
    </ThemeProvider>,
  )
  return router
}

function signIn(modules: ModuleCode[], logout = vi.fn(async () => undefined)) {
  useAuthStore.setState({ status: 'authenticated' satisfies AuthStatus, modules, user, logout })
  return logout
}

beforeEach(() => {
  useAuthStore.setState({ status: 'idle', modules: [], user: null, logout: vi.fn(async () => undefined) })
})

afterEach(() => {
  cleanup()
})

describe('AppLayout 菜单过滤', () => {
  it('授予 admin 时列出 admin 域菜单与个人中心', () => {
    signIn(['admin'])

    renderAt()

    expect(screen.getByText('用户管理')).toBeDefined()
    expect(screen.getByText('角色管理')).toBeDefined()
    expect(screen.getByText('会话管理')).toBeDefined()
    expect(screen.getByText('个人中心')).toBeDefined()
    expect(screen.getByText(OUTLET)).toBeDefined()
  })

  it('未授予 admin 时只剩个人中心，避免点进去一路 403', () => {
    signIn(['ecs'])

    renderAt()

    expect(screen.queryByText('用户管理')).toBeNull()
    expect(screen.queryByText('会话管理')).toBeNull()
    expect(screen.getByText('个人中心')).toBeDefined()
  })

  it('顶栏展示昵称，无昵称时退到用户名', () => {
    signIn(['admin'])

    renderAt()

    expect(screen.getByText('超级管理员')).toBeDefined()
  })
})

describe('AppLayout 折叠与全屏', () => {
  it('顶栏按钮收起侧栏为图标条，再次点击展开', () => {
    signIn(['admin'])

    renderAt()

    expect(document.querySelector('.ant-layout-sider-collapsed')).toBeNull()

    fireEvent.click(screen.getByLabelText('收起侧栏'))
    expect(document.querySelector('.ant-layout-sider-collapsed')).toBeDefined()

    fireEvent.click(screen.getByLabelText('展开侧栏'))
    expect(document.querySelector('.ant-layout-sider-collapsed')).toBeNull()
  })

  it('handle.fullScreen 为真时不渲染侧栏与折叠按钮，顶栏用户区仍在', () => {
    signIn(['admin'])

    renderAt('/shell/canvas')

    expect(document.querySelector('.ant-layout-sider')).toBeNull()
    expect(screen.getByLabelText('用户菜单')).toBeDefined()
    expect(screen.getByText('全屏画布')).toBeDefined()
  })
})

describe('AppLayout 登出', () => {
  // 弹层挂在 body 上的门户里，且「个人中心」与侧栏菜单同名，必须限定在弹层内查
  async function openUserMenu(): Promise<ReturnType<typeof within>> {
    fireEvent.click(screen.getByLabelText('用户菜单'))
    await waitFor(() => expect(document.querySelector('.ant-dropdown')).not.toBeNull())
    return within(document.querySelector<HTMLElement>('.ant-dropdown') as HTMLElement)
  }

  it('登出成功清掉会话并回 /login', async () => {
    const logout = signIn(['admin'])

    const router = renderAt()
    const menu = await openUserMenu()
    fireEvent.click(menu.getByText('退出登录'))

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(logout).toHaveBeenCalledTimes(1)
    expect(screen.queryByText(/撤销未成功/)).toBeNull()
  })

  it('服务端撤销失败时明示本地已登出该事实，并照常回 /login', async () => {
    signIn(
      ['admin'],
      vi.fn(async () => Promise.reject(new ApiError(401, '登录已失效'))),
    )

    const router = renderAt()
    const menu = await openUserMenu()
    fireEvent.click(menu.getByText('退出登录'))

    const tip = await screen.findByText(/本地已退出登录，但服务端会话撤销未成功/)
    expect(tip.textContent).toContain('401')
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
  })

  it('个人中心入口走导航，不清会话', async () => {
    const logout = signIn(['admin'])

    const router = renderAt()
    const menu = await openUserMenu()
    fireEvent.click(menu.getByText('个人中心'))

    await waitFor(() => expect(router.state.location.pathname).toBe('/profile'))
    expect(logout).not.toHaveBeenCalled()
  })
})
