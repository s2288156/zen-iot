import { RouterProvider } from 'react-router'
import { ErrorBoundary } from './components/ErrorBoundary'
import { router } from './routes'

// 两层错误处理的第一层兜底：RouterProvider 之下的 ErrorBoundary 只接组件 render 抛出的异常；
// lazy 装载失败与 loader/action 抛错是 promise rejection，由路由 errorElement 负责（见 routes/index.tsx）。
function App() {
  return (
    <ErrorBoundary>
      <RouterProvider router={router} />
    </ErrorBoundary>
  )
}

export default App
