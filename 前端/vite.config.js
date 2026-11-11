import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置
// 关键点：proxy —— 前端跑在 5173，Java 跑在 8081
// 浏览器请求 /api/xxx 时，Vite 把它转发到 8081，这样就没有跨域问题
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 编辑器保存文件时会先写临时文件再改名，Vite 的文件监听会撞上被锁的
    // 临时文件，抛 EBUSY 直接把整个 dev server 干掉。下面把这些排除掉。
    watch: {
      ignored: [
        '**/*.tmp',
        '**/*.tmpdir/**',
        '**/.~*',
        '**/.*.js.*.tmpdir/**'
      ]
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})