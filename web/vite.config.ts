import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// dev 期 /api 经网关（28080）联调：CORS 由网关集中配置，前端不另配
export default defineConfig({
  plugins: [react()],
  server: {
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
