import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { TableColumnsType } from 'antd'
import { Table } from 'antd'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ThemeProvider } from '../../theme'
import type { UserView } from '../../types/admin'
import type { UserColumnsDeps } from './userColumns'
import { buildUserColumns } from './userColumns'

function deps(overrides: Partial<UserColumnsDeps> = {}): UserColumnsDeps {
  return {
    currentUserId: 1,
    roleName: (id) => `角色${id}`,
    onEdit: vi.fn(),
    onResetPassword: vi.fn(),
    onAssignRoles: vi.fn(),
    onChangeStatus: vi.fn(),
    onDelete: vi.fn(),
    ...overrides,
  }
}

function user(overrides: Partial<UserView> = {}): UserView {
  return {
    id: 1,
    username: 'admin',
    nickname: '管理员',
    email: 'admin@zen-iot.local',
    phone: null,
    avatar: null,
    status: 1,
    roleIds: [1],
    creator: 'system',
    // 不带时区偏移，与 `UserView.createTime` 的真实形态一致（LocalDateTime 序列化）
    createTime: '2026-09-24T05:28:11',
    updater: null,
    updateTime: null,
    ...overrides,
  }
}

function renderRow(row: UserView, dependencies: UserColumnsDeps): void {
  render(
    <ThemeProvider>
      <Table<UserView>
        columns={buildUserColumns(dependencies)}
        dataSource={[row]}
        rowKey="id"
        pagination={false}
        scroll={{ x: 1280 }}
      />
    </ThemeProvider>,
  )
}

/** `Typography.Link disabled` 只加类名、不加 `aria-disabled`，故按类名判定 */
function isDisabled(text: string): boolean {
  return screen.getByText(text).closest('a')?.classList.contains('ant-typography-disabled') === true
}

function sortableKeys(columns: TableColumnsType<UserView>): string[] {
  return columns.flatMap((column) => ('sorter' in column && column.sorter === true ? [String(column.key)] : []))
}

afterEach(cleanup)

describe('用户列表列定义', () => {
  it('只有创建时间可排序，且 key 落在后端排序白名单里', () => {
    expect(sortableKeys(buildUserColumns(deps()))).toEqual(['createTime'])
  })

  it('角色列用传入的映射渲染显示名，未分配时给占位符', () => {
    renderRow(user({ roleIds: [1, 2] }), deps({ roleName: (id) => `角色${id}` }))
    expect(screen.getByText('角色1')).toBeDefined()
    expect(screen.getByText('角色2')).toBeDefined()
    cleanup()

    renderRow(user({ roleIds: [] }), deps())
    expect(screen.getByText('—')).toBeDefined()
  })

  it('创建时间渲染成 YYYY-MM-DD HH:mm:ss', () => {
    renderRow(user(), deps())
    expect(screen.getByText('2026-09-24 05:28:11')).toBeDefined()
  })
})

describe('用户列表自我保护', () => {
  it('当前登录用户那一行的「禁用」「删除」被禁用，点了也不回调', () => {
    const dependencies = deps({ currentUserId: 1 })
    renderRow(user({ id: 1 }), dependencies)

    expect(isDisabled('禁用')).toBe(true)
    expect(isDisabled('删除')).toBe(true)

    fireEvent.click(screen.getByText('删除'))
    fireEvent.click(screen.getByText('禁用'))
    expect(dependencies.onDelete).not.toHaveBeenCalled()
    expect(dependencies.onChangeStatus).not.toHaveBeenCalled()
  })

  it('他人行的「禁用」「删除」可操作', () => {
    const dependencies = deps({ currentUserId: 1 })
    renderRow(user({ id: 2 }), dependencies)

    expect(isDisabled('禁用')).toBe(false)
    expect(isDisabled('删除')).toBe(false)
  })

  it('会话尚未就绪时不做自我保护判定', () => {
    renderRow(user({ id: 1 }), deps({ currentUserId: null }))
    expect(isDisabled('删除')).toBe(false)
  })

  it('禁用他人经二次确认后以目标状态 0 回调', async () => {
    const dependencies = deps({ currentUserId: 1 })
    renderRow(user({ id: 2, username: 'ops' }), dependencies)

    fireEvent.click(screen.getByText('禁用'))
    expect(await screen.findByText('确认禁用 ops？')).toBeDefined()

    fireEvent.click(screen.getByRole('button', { name: /禁\s*用/ }))
    await waitFor(() => expect(dependencies.onChangeStatus).toHaveBeenCalledTimes(1))
    expect(dependencies.onChangeStatus).toHaveBeenCalledWith(expect.objectContaining({ id: 2 }), 0)
  })
})
