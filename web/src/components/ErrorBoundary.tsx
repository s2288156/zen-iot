import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { ErrorPage } from './ErrorPage'

interface Props {
  children: ReactNode
}

interface State {
  caught: boolean
  error: unknown
}

/**
 * 渲染期兜底（两层错误处理的第二层）：只接子树 `render` / 生命周期抛出的异常。
 * `lazy` 装载失败、`loader`/`action` 抛错都是 promise rejection，React 边界抓不到，归路由 `errorElement`。
 * 位置在 `RouterProvider` 之外，故其内的 `ErrorPage` 不能依赖 Router 上下文。
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { caught: false, error: null }

  static getDerivedStateFromError(error: unknown): State {
    return { caught: true, error }
  }

  componentDidCatch(error: unknown, info: ErrorInfo): void {
    // 页面上只给口径，堆栈进控制台：没有日志通道可去，控制台是唯一可追的落点
    console.error('[ErrorBoundary] 渲染期异常', error, info.componentStack)
  }

  private readonly reset = (): void => {
    this.setState({ caught: false, error: null })
  }

  render(): ReactNode {
    const { caught, error } = this.state
    if (caught) {
      return <ErrorPage error={error} onRetry={this.reset} />
    }
    return this.props.children
  }
}
