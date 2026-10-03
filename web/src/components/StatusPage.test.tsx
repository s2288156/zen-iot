import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import type { RouteObject } from 'react-router'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { ThemeProvider } from '../theme'
import { ForbiddenPage } from './Forbidden'
import { NotFoundPage } from './NotFound'

// 403/404 都用了 useNavigate，必须挂在 Router 上下文里渲染
const routes: RouteObject[] = [
  { path: '/', element: <div>首页</div> },
  { path: '/forbidden', element: <ForbiddenPage /> },
  { path: '/not-found', element: <NotFoundPage /> },
]

function renderAt(path: string) {
  render(
    <ThemeProvider>
      <RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />
    </ThemeProvider>,
  )
}

afterEach(() => {
  cleanup()
})

describe('状态页', () => {
  it('403 页给出无权口径与回首页动作', () => {
    renderAt('/forbidden')

    expect(screen.getByText('无访问权限')).toBeDefined()
    expect(screen.getByText('返回首页')).toBeDefined()
  })

  it('404 页给出返回上一页与回首页两个动作', () => {
    renderAt('/not-found')

    expect(screen.getByText('页面不存在')).toBeDefined()
    expect(screen.getByText('返回上一页')).toBeDefined()
    expect(screen.getByText('返回首页')).toBeDefined()
  })
})
