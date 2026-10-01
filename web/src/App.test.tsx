import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Component as LoginPage } from './features/auth/LoginPage'

// 冒烟用例：验证测试基建（vitest + testing-library + jsdom）可用；路由级断言随 Task 3 守卫测试展开。
describe('应用骨架冒烟', () => {
  it('渲染登录页门面标识', () => {
    render(<LoginPage />)
    expect(screen.getByText('zen-iot 管理控制台')).toBeDefined()
  })
})
