import { Result } from 'antd'
import type { ResultProps } from 'antd'
import { layout, spacing } from '../theme/tokens'

/**
 * 状态页壳的口径：色板一律交给 antd token（跟随主题），此处只放 `Result` 自身没有的盒模型常量。
 * 刻意不设背景：满视口时透出 `Layout.bodyBg`，与内容区同源，不会在深色/浅色切换时失配。
 */
const shellStyle = {
  minHeight: '100vh',
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  margin: `0 ${spacing.lg}px`,
  borderRadius: layout.borderRadius,
} as const

export interface StatusPageProps extends ResultProps {
  /** 为真时占满视口垂直居中（脱离布局的独立页面）；嵌在 `AppLayout` 内容区时传 `false` */
  fill?: boolean
}

/**
 * 公共状态页外壳：403 / 404 / 错误页共用的唯一一份 `Result` 包装（口径 C「别各写一份」）。
 * `status` 沿用 antd 的 `'403' | '404' | '500' | 'info' | ...` 联合，`'error'` 即红色叉号，用于运行时异常。
 */
export function StatusPage({ fill = true, ...result }: StatusPageProps) {
  return (
    <div style={fill ? shellStyle : undefined}>
      <Result {...result} />
    </div>
  )
}
