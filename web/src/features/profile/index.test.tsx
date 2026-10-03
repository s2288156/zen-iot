import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../stores/auth'
import { ThemeProvider } from '../../theme'
import type { UserProfileView } from '../../types/auth'
import { Component } from './index'

interface FakeResponse {
  status: number
  ok: boolean
  text: () => Promise<string>
}

/** 客户端只消费 status / ok / text()，与 `api/http.test.ts` 同构的最小假响应 */
function respond(status: number, body: unknown): FakeResponse {
  return { status, ok: status >= 200 && status < 300, text: () => Promise.resolve(JSON.stringify(body)) }
}

const fetchMock = vi.fn()

const profile: UserProfileView = {
  id: 1,
  username: 'admin',
  nickname: '超级管理员',
  email: 'admin@zen-iot.local',
  phone: null,
  avatar: null,
  status: 1,
  roleIds: [1],
}

function renderPage(): void {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <ThemeProvider>
        <Component />
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

function fillAndSubmit(oldPassword: string, newPassword: string, confirm: string): void {
  fireEvent.change(screen.getByLabelText('当前口令'), { target: { value: oldPassword } })
  fireEvent.change(screen.getByLabelText('新口令'), { target: { value: newPassword } })
  fireEvent.change(screen.getByLabelText('确认新口令'), { target: { value: confirm } })
  fireEvent.click(screen.getByRole('button', { name: '修改口令' }))
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  useAuthStore.setState({ status: 'authenticated', user: profile, modules: ['admin', 'wcs'] })
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('个人中心的账号信息', () => {
  it('直接取 auth store，本页不再发请求', () => {
    renderPage()

    expect(screen.getByText('超级管理员')).toBeDefined()
    expect(screen.getByText('admin@zen-iot.local')).toBeDefined()
    // 模块码渲染成中文显示名
    expect(screen.getByText('管理后台')).toBeDefined()
    expect(screen.getByText('仓库控制')).toBeDefined()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('未登录态不炸，空值一律给占位符', () => {
    useAuthStore.setState({ status: 'anonymous', user: null, modules: [] })
    renderPage()

    expect(screen.getByText('无')).toBeDefined()
    expect(screen.getAllByText('—').length).toBeGreaterThan(0)
  })
})

describe('自助改密的错误分支', () => {
  it('两次口令不一致时在前端拦下，不发请求', async () => {
    renderPage()
    fillAndSubmit('old-pass', 'new-pass-1', 'new-pass-2')

    // 校验规则返回 Promise，错误文案要等异步校验落盘后才出现
    expect(await screen.findByText('两次输入的口令不一致')).toBeDefined()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('新口令短于 8 位时在前端拦下，不发请求', async () => {
    renderPage()
    fillAndSubmit('old-pass', 'short', 'short')

    expect(await screen.findByText('新口令长度需在 8 ~ 72 个字符之间')).toBeDefined()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('旧口令不正确时把后端 message 直显在表单上方，而不是弹 toast', async () => {
    fetchMock.mockResolvedValue(respond(400, { code: 400, message: '旧口令不正确' }))
    renderPage()
    fillAndSubmit('wrong-old', 'new-pass-1', 'new-pass-1')

    const alert = await screen.findByText('旧口令不正确')
    // `silentError: true` 的口径：错误就地渲染在 Alert 里，同一条错误不再弹全局 toast
    expect(alert.closest('.ant-alert')).not.toBeNull()
    expect(screen.getAllByText('旧口令不正确')).toHaveLength(1)
  })
})

describe('自助改密成功', () => {
  it('清空表单并说明其他会话已下线', async () => {
    fetchMock.mockResolvedValue(respond(200, { code: 200, message: '成功' }))
    renderPage()
    fillAndSubmit('old-pass', 'new-pass-1', 'new-pass-1')

    // `AuthService#changePassword` 成功后吊销本人除当前会话外的全部会话，故文案必须点明
    expect(await screen.findByText('口令已修改，本账号的其他会话已下线')).toBeDefined()
    expect(screen.getByLabelText('当前口令')).toHaveProperty('value', '')
    expect(screen.getByLabelText('新口令')).toHaveProperty('value', '')
  })
})
