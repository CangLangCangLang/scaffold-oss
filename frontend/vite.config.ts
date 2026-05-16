import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { visualizer } from 'rollup-plugin-visualizer'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiPrefix = env.VITE_APP_BASE_API || '/dev-api'
  const backendOrigin = env.VITE_BACKEND_ORIGIN || 'http://localhost:9080'
  const port = Number(env.VITE_DEV_SERVER_PORT || 9081)
  const enableVisualizer = env.VITE_BUILD_REPORT === 'true'

  return {
    base: env.VITE_APP_PUBLIC_PATH || '/',
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    plugins: [
      vue(),
      {
        name: 'html-env-replace',
        transformIndexHtml(html) {
          return html.replace(/%VITE_APP_TITLE%/g, env.VITE_APP_TITLE || 'Fullstack Scaffold')
        }
      },
      AutoImport({
        imports: ['vue', 'vue-router', 'pinia'],
        resolvers: [ElementPlusResolver()],
        dts: 'src/types/auto-imports.d.ts',
        eslintrc: { enabled: true, filepath: './.eslintrc-auto-import.json' }
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/types/components.d.ts'
      }),
      enableVisualizer
        ? visualizer({ filename: 'dist/stats.html', gzipSize: true, brotliSize: true })
        : null
    ].filter(Boolean) as ReturnType<typeof vue>[],
    server: {
      host: '0.0.0.0',
      port,
      strictPort: false,
      proxy: {
        [apiPrefix]: {
          target: backendOrigin,
          changeOrigin: true,
          rewrite: (path) => path.replace(new RegExp(`^${apiPrefix}`), '')
        },
        '/v3/api-docs': {
          target: backendOrigin,
          changeOrigin: true
        },
        '/ws': {
          target: backendOrigin,
          changeOrigin: true,
          ws: true
        },
        // OAuth2 / OIDC SSO 入口与回调（由 Spring Security 处理后再 302 回前端）
        '/oauth2': { target: backendOrigin, changeOrigin: true },
        '/login/oauth2': { target: backendOrigin, changeOrigin: true }
      }
    },
    build: {
      sourcemap: false,
      target: 'es2020',
      cssCodeSplit: true,
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules')) {
              if (id.includes('echarts') || id.includes('vue-echarts') || id.includes('zrender')) {
                return 'vendor-charts'
              }
              if (id.includes('element-plus') || id.includes('@element-plus/icons-vue')) {
                return 'vendor-element'
              }
              if (id.includes('vue-i18n') || id.includes('@intlify')) {
                return 'vendor-i18n'
              }
              if (id.match(/[\\/]vue[\\/]/) || id.includes('vue-router') || id.includes('pinia')) {
                return 'vendor-vue'
              }
              // 拆 form-create：runtime 进主 bundle 周边的 vendor-form-create，
              // designer + 拖拽 + monaco 之类的重模块单独 vendor-form-create-designer，
              // 仅 FormDesigner 路由打开时才下载。
              if (
                id.includes('@form-create/designer')
                || id.includes('vuedraggable')
                || id.includes('codemirror')
              ) {
                return 'vendor-form-create-designer'
              }
              if (id.includes('@form-create/')) {
                return 'vendor-form-create-runtime'
              }
              if (id.includes('bpmn-js') || id.includes('diagram-js') || id.includes('didi') || id.includes('saxen')) {
                return 'vendor-bpmn'
              }
              // wangEditor 富文本（CMS 文章编辑器使用）：拆独立 chunk，
              // 仅 ArticleEdit 路由懒加载时才下载，避免主 bundle 被拖到 1MB 量级。
              if (id.includes('@wangeditor') || id.includes('@uppy') || id.includes('@types/dom-mediacapture-record')) {
                return 'vendor-wangeditor'
              }
              return 'vendor'
            }
            return undefined
          }
        }
      }
    }
  }
})
