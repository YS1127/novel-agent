import { resolve } from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, resolve(process.cwd(), '..'), '')
  const backendPort = Number(env.SERVER_PORT || 8080)
  const frontendPort = Number(env.FRONTEND_PORT || 5173)

  return {
    plugins: [vue()],
    server: {
      port: frontendPort,
      proxy: {
        '/api': {
          target: `http://localhost:${backendPort}`,
          changeOrigin: true,
        },
      },
    },
  }
})
