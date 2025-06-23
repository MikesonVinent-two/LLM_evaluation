import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 根据当前工作目录中的 `mode` 加载 .env 文件
  // 设置第三个参数为 '' 来加载所有环境变量，而不管是否有 `VITE_` 前缀。
  const env = loadEnv(mode, process.cwd(), '')

  return {
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '/src/types/websocket.ts': fileURLToPath(new URL('./src/types/websocketTypes.ts', import.meta.url))
    },
  },
    // 在这里定义一些与环境相关的值
    define: {
      'import.meta.env.VITE_APP_TITLE': JSON.stringify(env.VITE_APP_TITLE || '前端项目'),
      'import.meta.env.VITE_APP_ENV': JSON.stringify(env.VITE_APP_ENV || mode),
      'import.meta.env.VITE_APP_API_BASE_URL': JSON.stringify(env.VITE_APP_API_BASE_URL || 'http://localhost:8080'),
      'import.meta.env.VITE_APP_API_TIMEOUT': env.VITE_APP_API_TIMEOUT || 10000,
      'import.meta.env.VITE_APP_LLM_CHAT_TIMEOUT': env.VITE_APP_LLM_CHAT_TIMEOUT || 60000,
      // 为 sockjs-client 添加 global polyfill
      global: {}
    },
    optimizeDeps: {
      include: ['@stomp/stompjs', 'sockjs-client']
    }
  }
})
