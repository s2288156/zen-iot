import type { QueryClient } from '@tanstack/react-query'

/** 分页查询的参数维度：`PageQuery` 四参数 + 该接口的业务过滤条件 */
export type PageKeyParams = Record<string, string | number | boolean | undefined>

/**
 * 分页查询的 key 规范形态：`[domain, 'page', params]`。
 * 由 `PageTable` 内部生成，页面不手写 key；`params` 是普通对象，TanStack 按字典序哈希，故键序不影响命中。
 */
export function pageKey(domain: string, params: PageKeyParams): [string, 'page', PageKeyParams] {
  return [domain, 'page', params]
}

/**
 * 写操作（增删改）成功后按域前缀失效：`[domain]` 命中该域下所有分页与筛选组合。
 * 前缀匹配是 TanStack 的默认语义，因此失效整域比逐个枚举已发过的 key 更可靠。
 */
export function invalidateDomain(client: QueryClient, domain: string): Promise<void> {
  return client.invalidateQueries({ queryKey: [domain] })
}
