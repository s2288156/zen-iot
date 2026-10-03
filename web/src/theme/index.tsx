import { App as AntdApp, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import type { ReactNode } from 'react'
import { antdTheme } from './tokens'

// 暗色模式本轮不提供（无切换控件）；未来接入仅需一行：
// import { theme } from 'antd'
// theme = { ...antdTheme, algorithm: theme.darkAlgorithm }

/** 全局视觉入口：Design Tokens 经 ConfigProvider 注入，AntdApp 提供 message/modal 上下文 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  return (
    <ConfigProvider locale={zhCN} theme={antdTheme}>
      <AntdApp>{children}</AntdApp>
    </ConfigProvider>
  )
}
