import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { RouteObject } from 'react-router'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { DefaultHome, RequireAuth, RequireModule } from './guards'
import { useAuthStore, type AuthStatus } from '../stores/auth'
import type { ModuleCode, UserProfileView } from '../types/auth'

const HOME = '受保护内容'

const routes: RouteObject[] = [
  { path: '/login', element: <div>登录页</div> },
  { path: '/profile', element: <div>个人中心</div> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <div>{HOME}</div>
      </RequireAuth>
    ),
  },
  // RequireModule 与 DefaultHome 单独验证，故不再套一层 RequireAuth
  {
    path: '/users',
    element: (
      <RequireModule code="admin">
        <div>用户管理</div>
      </RequireModule>
    ),
  },
  { path: '/entry', element: <DefaultHome /> },
]

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
  return router
}

function setState(status: AuthStatus, modules: ModuleCode[] = [], user: UserProfileView | null = null) {
  useAuthStore.setState({ status, modules, user })
}

/** 手动控制装配何时完成，用来观察「bootstrap 进行中」这段不放行的窗口 */
function deferred() {
  let resolve!: () => void
  const promise = new Promise<void>((r) => {
    resolve = r
  })
  return { promise, resolve }
}

const profile: UserProfileView = {
  id: 1,
  username: 'wu',
  nickname: null,
  email: null,
  phone: null,
  avatar: null,
  status: 1,
  roleIds: [1],
}

beforeEach(() => {
  useAuthStore.setState({ status: 'idle', user: null, modules: [], bootstrap: vi.fn(async () => undefined) })
})

afterEach(() => {
  cleanup()
})

describe('RequireAuth', () => {
  it('idle 时触发一次 bootstrap，装配期间不放行，完成才放行内容', async () => {
    const gate = deferred()
    const bootstrap = vi.fn(async () => {
      await gate.promise
      setState('authenticated', ['admin'], profile)
    })
    useAuthStore.setState({ bootstrap })

    renderAt('/')

    expect(bootstrap).toHaveBeenCalledTimes(1)
    expect(screen.queryByText(HOME)).toBeNull()

    await act(async () => {
      gate.resolve()
      await Promise.resolve()
    })

    expect(screen.getByText(HOME)).toBeDefined()
  })

  it('bootstrap 进行中不渲染受保护内容', () => {
    setState('bootstrapping')

    renderAt('/')

    expect(screen.queryByText(HOME)).toBeNull()
    expect(useAuthStore.getState().bootstrap).not.toHaveBeenCalled()
  })

  it('未登录跳 /login，并把原路径与 query 存进 state.from 供登录后回跳', () => {
    setState('anonymous')

    const router = renderAt('/?tab=roles&page=2')

    expect(screen.getByText('登录页')).toBeDefined()
    expect(router.state.location.pathname).toBe('/login')
    expect(router.state.location.state).toMatchObject({ from: '/?tab=roles&page=2' })
  })

  it('已登录直接放行，不重复 bootstrap', () => {
    setState('authenticated', ['admin'], profile)

    renderAt('/')

    expect(screen.getByText(HOME)).toBeDefined()
    expect(useAuthStore.getState().bootstrap).not.toHaveBeenCalled()
  })

  it('会话在页内失效（轮转失败清会话）时立即回落 /login', async () => {
    setState('authenticated', ['admin'], profile)
    renderAt('/')

    setState('anonymous')

    await waitFor(() => expect(screen.getByText('登录页')).toBeDefined())
  })
})

describe('RequireModule', () => {
  it('授予该模块时放行页面', () => {
    setState('authenticated', ['admin', 'ecs'], profile)

    const router = renderAt('/users')

    expect(screen.getByText('用户管理')).toBeDefined()
    expect(router.state.location.pathname).toBe('/users')
  })

  it('未授予时渲染 403 状态页而非重定向', () => {
    setState('authenticated', ['ecs'], profile)

    const router = renderAt('/users')

    expect(screen.getByText('无访问权限')).toBeDefined()
    expect(screen.queryByText('用户管理')).toBeNull()
    expect(router.state.location.pathname).toBe('/users')
  })
})

describe('DefaultHome', () => {
  it('具备 admin 模块时落用户管理', () => {
    setState('authenticated', ['admin'], profile)

    const router = renderAt('/entry')

    expect(router.state.location.pathname).toBe('/users')
  })

  it('无 admin 模块时退到个人中心，避免首屏即 403', () => {
    setState('authenticated', ['ecs'], profile)

    const router = renderAt('/entry')

    expect(router.state.location.pathname).toBe('/profile')
  })
})
