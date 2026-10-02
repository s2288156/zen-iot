import { QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { queryClient } from './api/queryClient'
import { ThemeProvider } from './theme'

// 顺序定案：ThemeProvider > QueryClientProvider > App。
// QueryClient 与主题无关（不下沉进 theme/），放 ThemeProvider 内层保证 hook 同时拿得到 AntdApp 上下文。
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)
