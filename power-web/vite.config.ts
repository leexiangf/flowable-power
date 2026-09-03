import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: true,
    port: 5173,
    strictPort: false,
    proxy: {
      '/auth': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/system': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/workflow': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
