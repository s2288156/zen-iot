import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Alert, Button, Form, Input, Select, Space, Table } from 'antd'
import type { TableColumnsType, TableProps } from 'antd'
import { useState } from 'react'
import type { ReactNode } from 'react'
import type { PageFilters, PageRequest } from '../api'
import { fetchPage, MAX_PAGE_SIZE, pageKey } from '../api'
import type { OrderDirection } from '../types/api'
import { spacing } from '../theme/tokens'

/** 后端 `PageQuery` 的默认页容量 */
const DEFAULT_PAGE_SIZE = 10

/** 页容量候选：最大档取 `MAX_PAGE_SIZE`（后端 `PageQuery` 的硬约束），不得给出更大的选项 */
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100, MAX_PAGE_SIZE]

/** 查询表单的一个条件项；`name` 直接作为 query 参数名，故必须与后端字段名逐字一致 */
export interface PageTableFilter {
  name: string
  label: string
  type: 'text' | 'select'
  /** `type === 'select'` 必填；`text` 走后端模糊匹配 */
  options?: { label: string; value: string | number }[]
  placeholder?: string
}

export interface PageTableProps<T> {
  /** 缓存域：参与 queryKey 首段，写操作后按此前缀失效整域 */
  domain: string
  /** 列表接口路径，如 `/users/page`（不含 `/api/admin` 前缀，由 `http.ts#API_BASE` 补） */
  path: string
  /**
   * 需要排序的列设 `sorter: true`，并把 `key` 写成后端排序白名单里的字段名（如 `createTime`）。
   * 白名单外的字段会被后端拒绝，封装不做前端兜底排序。
   */
  columns: TableColumnsType<T>
  rowKey: string | ((record: T) => string)
  filters?: PageTableFilter[]
  /** 表格右上角的操作区（如「新建」按钮） */
  toolbar?: ReactNode
  /** 透传给 antd `Table`：列多的管理页需要 `{{ x: … }}` 横向滚动，否则窄屏会把操作列挤出可视区 */
  scroll?: TableProps<T>['scroll']
}

/**
 * 服务端分页表格封装（Task 4.2）：分页 + 排序 + 查询表单 + loading/error/empty 三态。
 *
 * 参数与缓存完全由封装托管，页面只声明 `domain` / `path` / `columns` / `filters`，
 * queryKey 一律由 `pageKey(domain, params)` 生成（约定见 `web/README.md`）。
 * 排序/换页容量回到第 1 页，避免「第 8 页换个排序方式」这类后端根本不该出现的请求。
 */
export function PageTable<T extends object>({
  domain,
  path,
  columns,
  rowKey,
  filters,
  toolbar,
  scroll,
}: PageTableProps<T>) {
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE)
  const [orderBy, setOrderBy] = useState<string | undefined>(undefined)
  const [orderDirection, setOrderDirection] = useState<OrderDirection | undefined>(undefined)
  const [filterValues, setFilterValues] = useState<PageFilters>({})
  const [form] = Form.useForm<PageFilters>()

  const params: PageRequest = {
    ...filterValues,
    pageNum,
    pageSize,
    ...(orderBy === undefined ? {} : { orderBy, orderDirection }),
  }

  const query = useQuery({
    queryKey: pageKey(domain, params),
    queryFn: () => fetchPage<T>(path, params),
    placeholderData: keepPreviousData,
  })

  const handleTableChange: TableProps<T>['onChange'] = (pagination, _selected, sorter, extra) => {
    if (extra.action === 'paginate') {
      const nextSize = pagination.pageSize ?? pageSize
      setPageSize(nextSize)
      setPageNum(nextSize === pageSize ? (pagination.current ?? 1) : 1)
      return
    }
    if (extra.action !== 'sort') return

    const result = Array.isArray(sorter) ? sorter[0] : sorter
    const field = typeof result?.columnKey === 'string' && result.order !== undefined ? result.columnKey : undefined
    setOrderBy(field)
    setOrderDirection(field === undefined ? undefined : result.order === 'ascend' ? 'asc' : 'desc')
    setPageNum(1)
  }

  const submitFilters = (values: PageFilters) => {
    setFilterValues(values)
    setPageNum(1)
  }

  const resetFilters = () => {
    form.resetFields()
    setFilterValues({})
    setPageNum(1)
  }

  const result = query.data
  const error = query.error

  return (
    <Space orientation="vertical" size={spacing.md} style={{ width: '100%' }}>
      {filters === undefined || filters.length === 0 ? null : (
        <Form<PageFilters>
          form={form}
          layout="inline"
          name={`${domain}-filters`}
          onFinish={submitFilters}
          style={{ rowGap: spacing.sm, display: 'flex' }}
        >
          {filters.map((item) => (
            <Form.Item key={item.name} name={item.name} label={item.label}>
              {item.type === 'text' ? (
                <Input allowClear placeholder={item.placeholder ?? `请输入${item.label}`} />
              ) : (
                <Select allowClear options={item.options} placeholder={item.placeholder ?? '全部'} />
              )}
            </Form.Item>
          ))}
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                查询
              </Button>
              <Button onClick={resetFilters}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      )}

      {toolbar === undefined ? null : <div style={{ display: 'flex', justifyContent: 'flex-end' }}>{toolbar}</div>}

      {query.isError ? (
        <Alert
          type="error"
          showIcon
          title="列表加载失败"
          description={error instanceof Error ? error.message : '未知错误'}
          action={
            <Button size="small" onClick={() => void query.refetch()}>
              重试
            </Button>
          }
        />
      ) : (
        <Table<T>
          rowKey={rowKey}
          columns={columns}
          dataSource={result?.list ?? []}
          loading={query.isPending || query.isPlaceholderData}
          onChange={handleTableChange}
          scroll={scroll}
          pagination={{
            current: result?.pageNum ?? pageNum,
            pageSize,
            total: result?.total ?? 0,
            showSizeChanger: true,
            pageSizeOptions: PAGE_SIZE_OPTIONS,
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      )}
    </Space>
  )
}
