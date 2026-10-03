import { useMutation, useQueryClient } from '@tanstack/react-query'
import { App } from 'antd'
import { errorText } from '../api/errors'
import { invalidateDomain } from '../api/queryKeys'

export interface ActionOptions<TVariables> {
  /** 成功后失效的查询域前缀（`invalidateDomain` 的 `[domain]`）；没有列表可刷时传 `null` */
  domain: string | null
  /** 成功提示；需要按入参定制文案（如启停要看目标状态）时传函数 */
  success: string | ((variables: TVariables) => string)
  /**
   * 失败时不弹全局 toast，把错误留给调用方就地渲染。
   * 表单类操作（创建/编辑/改密）在弹窗的 Alert 上直显后端 message，同一条错误既弹 toast 又占表单位是噪音。
   * 默认 `false`：行内操作（启停/删除/下线）没有就地的展示位，必须靠 toast。
   */
  silentError?: boolean
}

/**
 * 管理台写操作的统一封装：`useMutation` + 成功提示 + 整域失效 + 失败 toast。
 *
 * 十来个写接口（用户 6 / 角色 4 / 会话 1 / 自助改密 1）的成功与失败处理完全同构，
 * 差别只在文案与失效域，故收敛到一处；页面只声明「动了哪个域、成功说什么」。
 * `message` 取自 `App.useApp()`，因此提示挂在 `ThemeProvider` 的 `AntdApp` 上下文里，能吃到主题与 locale。
 */
export function useAction<TData, TVariables>(
  mutate: (variables: TVariables) => Promise<TData>,
  options: ActionOptions<TVariables>,
) {
  const { message } = App.useApp()
  const client = useQueryClient()

  return useMutation({
    mutationFn: mutate,
    onSuccess: (_data, variables) => {
      if (options.domain !== null) void invalidateDomain(client, options.domain)
      message.success(typeof options.success === 'string' ? options.success : options.success(variables))
    },
    onError: (error) => {
      if (options.silentError !== true) message.error(errorText(error))
    },
  })
}
