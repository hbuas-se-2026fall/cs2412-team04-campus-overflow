import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';

// 开发期把 /api 代理到后端，避免跨域，也让会话 Cookie 与 CSRF 双提交在同源下正常工作
export default defineConfig({
  plugins: [react()],
  resolve: {
    // 路径别名 @/ → src/（Lab 5 Step 2）
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
