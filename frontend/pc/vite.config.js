import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import compression from 'vite-plugin-compression'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // element-plus 按需自动引入（组件 + ElMessage 等 API）
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue'],
      dts: false
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false
    }),
    // 构建时生成 .gz / .br 预压缩文件（静态服务自动响应，体积再降约 65%）
    compression({ algorithm: 'gzip', ext: '.gz', threshold: 10240 }),
    compression({ algorithm: 'brotliCompress', ext: '.br', threshold: 10240 })
  ],
  resolve: {
    alias: {
      "@": path.resolve("./src"), // 相对路径别名配置，使用 @ 代替 src
      "@xigua/core": path.resolve("../packages/xigua-core/src")
    }
  },

  server: {
    host: '0.0.0.0', // 这个用于启动
    port: '5100', // 指定启动端口
    fs: {
      // 共享核心位于项目同级 packages 目录，开发服务器需显式放行
      allow: [path.resolve('.'), path.resolve('..')]
    }
  },

  // 生产包本地预览时也走同源代理：Cloudflare Tunnel 只需暴露 5100 一个端口
  preview: {
    host: '0.0.0.0',
    port: 5100,
    strictPort: true,
    // Cloudflare Tunnel / 自定义域名会带非 localhost 的 Host 头
    allowedHosts: true,
    proxy: {
      '/client': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
      '/center': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
      '/ai': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
      '/admin': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
      '/search': { target: 'http://127.0.0.1:8090', changeOrigin: true, ws: true },
      '/media': {
        target: 'http://127.0.0.1:9000',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/media/, '')
      }
    }
  },

  build: {
    // 大依赖拆成独立 chunk：可并行下载 + 长期缓存，显著降低首屏解析量
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-element': ['element-plus'],
          'vendor-axios': ['axios'],
          'vendor-utils': ['js-cookie', 'ua-parser-js']
        }
      }
    }
  }
})
