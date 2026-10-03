import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { Table } from 'antd'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ThemeProvider } from '../../theme'
import type { SessionView } from '../../types/admin'
import type { SessionColumnsDeps } from './sessionColumns'
import { buildSessionColumns } from './sessionColumns'

function deps(overrides: Partial<SessionColumnsDeps> = {}): SessionColumnsDeps {
  return { currentUserId: 1, onKickout: vi.fn(), ...overrides }
}

function session(overrides: Partial<SessionView> = {}): SessionView {
  return {
    sessionId: '9f1c0a7e-2b6d-4c11-9a5e-0d3f7b2c8a41',
    userId: 2,
    username: 'ops',
    ip: '10.0.0.8',
    userAgent: 'Mozilla/5.0 (X11; Linux x86_64)',
    // 不带时区偏移，格式化结果不随运行环境时区漂移（UTC 纳秒形态的解析在 `utils/datetime` 一侧覆盖）
    issueTime: '2026-10-01T10:24:00',
    expireTime: '2026-10-08T10:24:00',
    ...overrides,
  }
}

function renderRow(record: SessionView, dependencies: SessionColumnsDeps): void {
  render(
    <ThemeProvider>
      <Table<SessionView>
        columns={buildSessionColumns(dependencies)}
        dataSource={[record]}
        rowKey="sessionId"
        pagination={false}
      />
    </ThemeProvider>,
  )
}

/** `Typography.Link disabled` 只加类名、不加 `aria-disabled`，故按类名判定 */
function kickoutDisabled(): boolean {
  return screen.getByText('强制下线').closest('a')?.classList.contains('ant-typography-disabled') === true
}

afterEach(cleanup)

describe('在线会话列定义', () => {
  it('任何一列都不提供排序控件', () => {
    // `SessionService#page` 恒按 Redis ZSET 的过期时刻升序取页，完全忽略 orderBy/orderDirection
    const columns = buildSessionColumns(deps())
    expect(columns.some((column) => 'sorter' in column && column.sorter === true)).toBe(false)
  })

  it('IP 与 User-Agent 缺失时用占位符', () => {
    renderRow(session({ ip: null, userAgent: null }), deps())
    expect(screen.getAllByText('—')).toHaveLength(2)
  })

  it('登录与过期时间渲染成 YYYY-MM-DD HH:mm:ss', () => {
    renderRow(session(), deps())
    expect(screen.getByText('2026-10-01 10:24:00')).toBeDefined()
    expect(screen.getByText('2026-10-08 10:24:00')).toBeDefined()
  })
})

describe('强制下线的自我保护', () => {
  it('本人名下的会话被禁用，点了也不回调', () => {
    const dependencies = deps({ currentUserId: 2 })
    renderRow(session({ userId: 2 }), dependencies)

    expect(kickoutDisabled()).toBe(true)
    fireEvent.click(screen.getByText('强制下线'))
    expect(dependencies.onKickout).not.toHaveBeenCalled()
  })

  it('会话尚未就绪时不判定为本人，避免误禁他人行', () => {
    renderRow(session({ userId: 2 }), deps({ currentUserId: null }))
    expect(kickoutDisabled()).toBe(false)
  })

  it('他人会话经二次确认后以 sessionId 回调', async () => {
    const dependencies = deps({ currentUserId: 1 })
    renderRow(session({ userId: 2, username: 'ops' }), dependencies)

    expect(kickoutDisabled()).toBe(false)
    fireEvent.click(screen.getByText('强制下线'))
    expect(await screen.findByText('确认强制下线 ops？')).toBeDefined()

    fireEvent.click(screen.getByRole('button', { name: /下\s*线/ }))
    await waitFor(() => expect(dependencies.onKickout).toHaveBeenCalledTimes(1))
    // `DELETE /sessions/{sessionId}` 的路径参数是 UUID，不是数值 userId
    expect(dependencies.onKickout).toHaveBeenCalledWith(
      expect.objectContaining({ sessionId: '9f1c0a7e-2b6d-4c11-9a5e-0d3f7b2c8a41' }),
    )
  })
})
