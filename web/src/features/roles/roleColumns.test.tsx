import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import type { TableColumnsType } from 'antd'
import { Table } from 'antd'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ThemeProvider } from '../../theme'
import type { RoleView } from '../../types/auth'
import type { RoleColumnsDeps } from './roleColumns'
import { buildRoleColumns } from './roleColumns'

function deps(overrides: Partial<RoleColumnsDeps> = {}): RoleColumnsDeps {
  return { onEdit: vi.fn(), onAssignModules: vi.fn(), onDelete: vi.fn(), ...overrides }
}

function role(overrides: Partial<RoleView> = {}): RoleView {
  return {
    id: 2,
    roleCode: 'warehouse_admin',
    roleName: '仓库管理员',
    description: '负责出入库',
    modules: ['admin'],
    creator: 'system',
    // 不带时区偏移，与 `LocalDateTime` 序列化的真实形态一致，格式化结果不随运行环境时区漂移
    createTime: '2026-09-24T05:28:11',
    updater: null,
    updateTime: null,
    ...overrides,
  }
}

function renderRow(record: RoleView, dependencies: RoleColumnsDeps = deps()): void {
  render(
    <ThemeProvider>
      <Table<RoleView> columns={buildRoleColumns(dependencies)} dataSource={[record]} rowKey="id" pagination={false} />
    </ThemeProvider>,
  )
}

function sortableKeys(columns: TableColumnsType<RoleView>): string[] {
  return columns.flatMap((column) => ('sorter' in column && column.sorter === true ? [String(column.key)] : []))
}

/**
 * 操作入口要按 `<a>` 定位：「模块」同时是列标题，「删除」同时是 Popconfirm 的确认按钮文案，
 * 而确认框关闭后 DOM 仍留在文档里（antd 只加 `display:none`），按纯文本查会命中多个节点。
 */
function actionLink(name: string): HTMLElement {
  const link = screen.getAllByText(name).find((node) => node.tagName === 'A')
  if (link === undefined) throw new Error(`找不到操作入口「${name}」`)
  return link
}

afterEach(cleanup)

describe('角色列表列定义', () => {
  it('只有创建时间可排序，且 key 落在后端排序白名单里', () => {
    // `RoleService#SORTABLE_FIELDS` 之外的值会让 `GET /roles/page` 返回 400「不支持的排序字段」
    expect(sortableKeys(buildRoleColumns(deps()))).toEqual(['createTime'])
  })

  it('模块列渲染中文显示名而不是模块码', () => {
    renderRow(role({ modules: ['admin', 'wcs', 'rcs', 'ecs'] }))
    expect(screen.getByText('管理后台')).toBeDefined()
    expect(screen.getByText('仓库控制')).toBeDefined()
    expect(screen.getByText('机器人调度')).toBeDefined()
    expect(screen.getByText('设备控制')).toBeDefined()
  })

  it('未授权任何模块时给「未授权」提示，而不是空单元格', () => {
    renderRow(role({ modules: [] }))
    expect(screen.getByText('未授权')).toBeDefined()
  })

  it('描述为空时用占位符', () => {
    renderRow(role({ description: null }))
    expect(screen.getByText('—')).toBeDefined()
  })
})

describe('角色列表操作入口', () => {
  it('点「编辑」「模块」把整行记录回调出去', () => {
    const dependencies = deps()
    const record = role()
    renderRow(record, dependencies)

    fireEvent.click(actionLink('编辑'))
    fireEvent.click(actionLink('模块'))

    expect(dependencies.onEdit).toHaveBeenCalledWith(record)
    // 模块授权是独立入口：`PUT /roles/{id}` 不含 modules，故必须走 `PUT /roles/{id}/modules`
    expect(dependencies.onAssignModules).toHaveBeenCalledWith(record)
  })

  it('删除经二次确认后才回调，取消则不回调', () => {
    const dependencies = deps()
    renderRow(role(), dependencies)

    fireEvent.click(actionLink('删除'))
    expect(screen.getByText('确认删除 仓库管理员？')).toBeDefined()

    fireEvent.click(screen.getByRole('button', { name: /取\s*消/ }))
    expect(dependencies.onDelete).not.toHaveBeenCalled()

    fireEvent.click(actionLink('删除'))
    fireEvent.click(screen.getByRole('button', { name: /删\s*除/ }))
    expect(dependencies.onDelete).toHaveBeenCalledTimes(1)
  })
})
