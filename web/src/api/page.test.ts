import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchPage } from './page'

interface FakeResponse {
  status: number
  ok: boolean
  text: () => Promise<string>
}

const urls: string[] = []

function mockFetch(page: unknown): void {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown) => {
      urls.push(String(input))
      const body = { code: 200, message: '成功', data: page }
      const response: FakeResponse = { status: 200, ok: true, text: () => Promise.resolve(JSON.stringify(body)) }
      return response
    }),
  )
}

beforeEach(() => {
  urls.length = 0
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('fetchPage', () => {
  it('解包 PageResult 并保留后端字段名', async () => {
    mockFetch({ list: [{ id: 1 }], total: 42, pageNum: 2, pageSize: 10, pages: 5 })

    const page = await fetchPage<{ id: number }>('/users/page', { pageNum: 2, pageSize: 10 })

    expect(page).toEqual({ list: [{ id: 1 }], total: 42, pageNum: 2, pageSize: 10, pages: 5 })
    expect(urls[0]).toBe('/api/admin/users/page?pageNum=2&pageSize=10')
  })

  it('排序参数用后端口径 orderBy/orderDirection，不是 sortField/sortOrder', async () => {
    mockFetch({ list: [], total: 0, pageNum: 1, pageSize: 10, pages: 0 })

    await fetchPage('/roles/page', { pageNum: 1, pageSize: 10, orderBy: 'createTime', orderDirection: 'desc' })

    expect(urls[0]).toContain('orderBy=createTime')
    expect(urls[0]).toContain('orderDirection=desc')
    expect(urls[0]).not.toContain('sortField')
  })

  it('空串按不过滤剔除，但 status=0 这类合法假值必须留下', async () => {
    mockFetch({ list: [], total: 0, pageNum: 1, pageSize: 10, pages: 0 })

    await fetchPage('/users/page', { pageNum: 1, pageSize: 10, username: '', status: 0 })

    expect(urls[0]).toBe('/api/admin/users/page?pageNum=1&pageSize=10&status=0')
  })

  it('会话列表接口路径没有 /page 后缀，也不接受过滤条件', async () => {
    mockFetch({ list: [], total: 0, pageNum: 1, pageSize: 10, pages: 0 })

    await fetchPage('/sessions', { pageNum: 1, pageSize: 10 })

    expect(urls[0]).toBe('/api/admin/sessions?pageNum=1&pageSize=10')
  })
})
