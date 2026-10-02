import type { PageQuery, PageResult } from '../types/api'
import { request } from './http'

/** 业务过滤条件：值域与 `request` 的 query 一致，`undefined` 由 `http.ts#buildUrl` 剔除 */
export type PageFilters = Record<string, string | number | boolean | undefined>

/** 列表接口入参 = `PageQuery` 四参数 + 该接口自己的过滤条件 */
export type PageRequest = PageQuery & PageFilters

/**
 * 服务端分页统一入口（GET）。
 * 空串按「不过滤」剔除：查询表单清空后留下 `username=` 会让 URL 与 queryKey 平白多一个维度，
 * 而后端模糊查询本就按 `hasText` 判定，传空等价于不传。`0`（如 status=0 禁用）必须保留。
 */
export async function fetchPage<T>(path: string, params: PageRequest): Promise<PageResult<T>> {
  const query: PageFilters = {}
  for (const [key, value] of Object.entries(params)) {
    if (value !== '') query[key] = value
  }
  return request<PageResult<T>>({ path, query })
}
