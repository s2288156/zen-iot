import { QueryClient } from '@tanstack/react-query'

/**
 * 全局唯一 QueryClient。与主题无关，故留在 api 层、由 `main.tsx` 挂在 `ThemeProvider` 内层。
 * `retry: false`：失败码是后端业务语义（401/403/409…），盲目重试既救不回也会放大请求；
 * 超时与断网已由 `http.ts` 归一化成面向用户的文案，同样不该自动再来一次。
 * `refetchOnWindowFocus: false`：管理台是低频读，切回窗口就重拉会冲掉用户正在填的筛选态。
 * `mutations.retry: false`：React Query 对 mutation 的默认值同样是 0，这里显式写死是因为写操作的失败码
 * （400 参数不合法 / 409 编码已存在 / 角色仍被引用）都是确定性的，重试只会重复报错并可能产生副作用。
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false, refetchOnWindowFocus: false },
    mutations: { retry: false },
  },
})
