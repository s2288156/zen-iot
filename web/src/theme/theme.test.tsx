import { render, screen } from '@testing-library/react'
import { theme } from 'antd'
import { describe, expect, it } from 'vitest'
import { colorPrimary } from './tokens'
import { ThemeProvider } from './index'

function TokenProbe() {
  const { token } = theme.useToken()
  return <span data-testid="probe">{token.colorPrimary}</span>
}

// 主题冒烟：ThemeProvider 经 ConfigProvider 注入的 token 与 src/theme/tokens.ts 定义一致（风格单点生效）。
describe('视觉主题层', () => {
  it('ConfigProvider 注入科技蓝主色', () => {
    render(
      <ThemeProvider>
        <TokenProbe />
      </ThemeProvider>,
    )
    expect(screen.getByTestId('probe').textContent).toBe(colorPrimary)
  })
})
