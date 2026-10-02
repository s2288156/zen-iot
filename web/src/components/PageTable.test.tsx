import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { TableColumnsType } from 'antd'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ThemeProvider } from '../theme'
import type { PageTableProps } from './PageTable'
import { PageTable } from './PageTable'

interface Row {
  id: number
  username: string
  createTime: string
}

const columns: TableColumnsType<Row> = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '创建时间', key: 'createTime', sorter: true },
]

interface FakeResponse {
  status: number
  ok: boolean
  text: () => Promise<string>
}

const urls: string[] = []

/** 可控响应：`gate` 未放行前请求吊着，用于观察 loading 窗口 */
function mockFetch(next: () => Promise<{ code: number; message: string; data?: unknown }>) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown) => {
      urls.push(String(input))
      const body = await next()
      const response: FakeResponse = { status: 200, ok: true, text: () => Promise.resolve(JSON.stringify(body)) }
      return response
    }),
  )
}

function page(list: Row[], total = list.length) {
  return { list, total, pageNum: 1, pageSize: 10, pages: Math.ceil(total / 10) }
}

const rows: Row[] = [{ id: 1, username: 'admin', createTime: '2026-10-01' }]

/** antd 给两字按钮插入空格（`查 询`），因此只能按可访问名做正则匹配 */
function button(name: RegExp): HTMLElement {
  return screen.getByRole('button', { name })
}

function renderTable(props: Partial<PageTableProps<Row>> = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const ui: PageTableProps<Row> = {
    domain: 'users',
    path: '/users/page',
    columns,
    rowKey: 'id',
    ...props,
  }
  render(
    <ThemeProvider>
      <QueryClientProvider client={client}>
        <PageTable<Row> {...ui} />
      </QueryClientProvider>
    </ThemeProvider>,
  )
  return client
}

/** loading 结束即数据已落到渲染（Spin 卸载与行渲染在同一次提交） */
async function settled(): Promise<void> {
  await waitFor(() => expect(document.querySelector('.ant-spin-spinning')).toBeNull())
}

beforeEach(() => {
  urls.length = 0
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('PageTable 装载', () => {
  it('挂载即以 PageQuery 口径发起分页请求，并渲染行与总数', async () => {
    mockFetch(async () => ({ code: 200, message: '成功', data: page(rows, 42) }))

    renderTable()

    expect(await screen.findByText('admin')).toBeDefined()
    expect(screen.getByText('共 42 条')).toBeDefined()
    expect(urls[0]).toBe('/api/admin/users/page?pageNum=1&pageSize=10')
  })

  it('响应未到达期间是 loading 态，到达后转成数据', async () => {
    let release!: () => void
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    mockFetch(async () => {
      await gate
      return { code: 200, message: '成功', data: page(rows) }
    })

    renderTable()
    expect(document.querySelector('.ant-spin-spinning')).not.toBeNull()

    release()
    await settled()

    expect(screen.getByText('admin')).toBeDefined()
    expect(document.querySelector('.ant-table-empty')).toBeNull()
  })

  it('空结果走表格内置空态，不留一片空白', async () => {
    mockFetch(async () => ({ code: 200, message: '成功', data: page([], 0) }))

    renderTable()
    await settled()

    expect(document.querySelector('.ant-table-empty')).not.toBeNull()
    expect(document.querySelector('.ant-empty-description')?.textContent).toBe('暂无数据')
  })
})

describe('PageTable 查询表单', () => {
  it('提交条件后按参数名重新请求，不追加空维度', async () => {
    mockFetch(async () => ({ code: 200, message: '成功', data: page(rows) }))

    renderTable({ filters: [{ name: 'username', label: '用户名', type: 'text' }] })
    await settled()

    fireEvent.change(screen.getByLabelText('用户名'), { target: { value: 'admin' } })
    fireEvent.click(button(/查\s*询/))

    await waitFor(() => expect(urls).toHaveLength(2))
    expect(urls[1]).toContain('username=admin')
    expect(urls[1]).not.toMatch(/[?&][^&=]+=(&|$)/)
  })

  it('重置清空条件并回到第 1 页', async () => {
    mockFetch(async () => ({ code: 200, message: '成功', data: page(rows) }))

    renderTable({ filters: [{ name: 'roleName', label: '角色名', type: 'text' }] })
    await settled()

    fireEvent.change(screen.getByLabelText('角色名'), { target: { value: 'ops' } })
    fireEvent.click(button(/查\s*询/))
    await waitFor(() => expect(urls).toHaveLength(2))
    expect(urls[1]).toContain('roleName=ops')

    fireEvent.click(button(/重\s*置/))

    await waitFor(() => expect(urls).toHaveLength(3))
    expect(urls[2]).toBe('/api/admin/users/page?pageNum=1&pageSize=10')
  })
})

describe('PageTable 排序与失败态', () => {
  it('点表头按后端白名单字段发起排序，方向用 asc/desc', async () => {
    mockFetch(async () => ({ code: 200, message: '成功', data: page(rows) }))

    renderTable()
    await settled()

    fireEvent.click(screen.getByText('创建时间'))

    await waitFor(() => expect(urls).toHaveLength(2))
    expect(urls[1]).toContain('orderBy=createTime')
    expect(urls[1]).toContain('orderDirection=asc')
    expect(urls[1]).not.toContain('sortField')
  })

  it('请求失败渲染错误提示而非空表，重试再发一次请求', async () => {
    mockFetch(async () => ({ code: 409, message: '角色不存在' }))

    renderTable()

    expect(await screen.findByText('列表加载失败')).toBeDefined()
    expect(screen.getByText('角色不存在')).toBeDefined()
    expect(document.querySelector('.ant-table')).toBeNull()

    fireEvent.click(button(/重\s*试/))

    await waitFor(() => expect(urls).toHaveLength(2))
  })
})
