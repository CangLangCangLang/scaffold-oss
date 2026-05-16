import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'nprogress/nprogress.css'
import '@/styles/index.scss'

import App from './App.vue'
import router from './router'
import i18n from './locales'
import { setupPermissionDirectives } from './directives/permission'
import { useUserStore } from './stores/user'
import { useThemeStore } from './stores/theme'
import { loadFrontendModules } from './modules/loader'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(i18n)

setupPermissionDirectives(app)

useThemeStore() // ensure dark/light html class applied on boot

const userStore = useUserStore()
userStore.bootstrapToken()

// 自动加载 src/modules/* 与 src/enterprise/* 下所有可插拔模块（每个模块通过默认导出注册路由 / locales / install）。
// 删除模块整目录即下线，无需修改主程序。先挂载再加载也可，但路由 / locales 应在挂载前生效，
// 所以这里同步等待。
loadFrontendModules({ app, router, i18n })
  .catch((err) => console.error('[modules] 加载失败', err))
  .finally(() => app.mount('#app'))
