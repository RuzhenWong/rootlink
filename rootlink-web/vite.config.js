import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())

  return {
    plugins: [vue()],

    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },

    // 开发服务器配置
    server: {
      port: 3000,
      open: true,   // 启动后自动打开浏览器
      proxy: {
        // 开发时将 /api 请求代理到后端，解决跨域
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },

    // 生产打包配置
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      // 打包后的文件是否生成 sourcemap（生产环境建议关闭）
      sourcemap: false,
      rollupOptions: {
        output: {
          // 按模块拆分打包，减小单文件体积
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            elementPlus: ['element-plus', '@element-plus/icons-vue'],
          },
        },
      },
    },
  }
})
