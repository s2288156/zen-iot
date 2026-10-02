import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/errors'
import { ThemeProvider } from '../theme'
import { ErrorBoundary } from './ErrorBoundary'

let shouldThrow = false
let thrown: unknown = null

function Boom() {
  if (shouldThrow) throw thrown
  return <div>正常内容</div>
}

beforeEach(() => {
  shouldThrow = false
  thrown = null
  // 错误边界会主动把堆栈打到控制台，测试里压掉以免刷屏
  vi.spyOn(console, 'error').mockImplementation(() => undefined)
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

function renderBounded() {
  return render(
    <ThemeProvider>
      <ErrorBoundary>
        <Boom />
      </ErrorBoundary>
    </ThemeProvider>,
  )
}

describe('ErrorBoundary', () => {
  it('子树抛错时渲染错误状态页而非白屏', () => {
    shouldThrow = true
    thrown = new Error('渲染期异常')

    renderBounded()

    expect(screen.getByText('页面出错了')).toBeDefined()
    expect(screen.getByText('渲染期异常')).toBeDefined()
    expect(screen.queryByText('正常内容')).toBeNull()
  })

  it('ApiError 把语义码一并露出来，便于用户报障', () => {
    shouldThrow = true
    thrown = new ApiError(409, '角色仍被用户引用')

    renderBounded()

    expect(screen.getByText('409 · 角色仍被用户引用')).toBeDefined()
  })

  it('重试重置边界，子树恢复后可重新渲染', () => {
    shouldThrow = true
    thrown = new Error('渲染期异常')

    renderBounded()
    expect(screen.getByText('页面出错了')).toBeDefined()

    shouldThrow = false
    fireEvent.click(screen.getByRole('button', { name: /重\s*试/ }))

    expect(screen.getByText('正常内容')).toBeDefined()
    expect(screen.queryByText('页面出错了')).toBeNull()
  })
})
