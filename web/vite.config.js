import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite 配置：开发期将 /api 代理到后端 svc。
// 后端端口由 SVC_PORT 环境变量覆盖（install 脚本启动时设置），默认 8080。
const SVC_PORT = process.env.SVC_PORT || '8080';
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: `http://localhost:${SVC_PORT}`,
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'node',
    globals: true,
  },
});
