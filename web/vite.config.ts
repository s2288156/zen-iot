import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// dev 期 /api 经网关（28080）联调：CORS 由网关集中配置，前端不另配
export default defineConfig({
  plugins: [react()],
  server: {
    // 端口显式钉死：Vite 默认会在 5173 被占时静默换端，`./gradlew devUp` 的端口探测会因此失效
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:28080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
  },
})
