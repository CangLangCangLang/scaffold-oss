import type { App } from 'vue'
import type { Router, RouteRecordRaw } from 'vue-router'
import type { I18n } from 'vue-i18n'

/**
 * 业务模块对外契约。
 * <p>
 * 在 {@code frontend/src/modules/<name>/index.ts} 或
 * {@code frontend/src/enterprise/<name>/index.ts} 默认导出本对象即可：
 * <pre>
 * export default {
 *   name: 'inbox',
 *   routes: [...],
 *   locales: { 'zh-CN': zhCN, 'en-US': enUS },
 *   install(app, ctx) { ... }
 * } satisfies ScaffoldFrontendModule
 * </pre>
 * 删除整个目录即可下线模块。
 */
export interface ScaffoldFrontendModule {
  /** 模块短名（必填，唯一），用于日志与重复检测 */
  name: string
  /** 可选静态路由：会被插入到根布局的 children 中 */
  routes?: RouteRecordRaw[]
  /** 可选 i18n 包：按 locale code 合并到 vue-i18n */
  locales?: Record<string, Record<string, unknown>>
  /** 可选 install 钩子：拿到 app / router / i18n 做自定义初始化（注册指令、全局组件等） */
  install?: (ctx: ModuleInstallContext) => void | Promise<void>
}

export interface ModuleInstallContext {
  app: App
  router: Router
  i18n: I18n
}

const loaded: ScaffoldFrontendModule[] = []

/**
 * 自动加载 src/modules/<name>/index.ts 和 src/enterprise/<name>/index.ts。
 * <p>
 * 由 main.ts 在 createApp 之后调用一次。删除模块目录即下线，零中央配置。
 */
export async function loadFrontendModules(ctx: ModuleInstallContext): Promise<ScaffoldFrontendModule[]> {
  const modules: Record<string, { default: ScaffoldFrontendModule }> = {
    ...import.meta.glob<{ default: ScaffoldFrontendModule }>('../modules/*/index.ts', { eager: true }),
    ...import.meta.glob<{ default: ScaffoldFrontendModule }>('../enterprise/*/index.ts', { eager: true })
  }
  const seen = new Set<string>()
  for (const path of Object.keys(modules).sort()) {
    const exported = modules[path]?.default
    if (!exported || !exported.name) {
      console.warn(`[modules] ${path} 缺少默认导出或 name 字段，已跳过`)
      continue
    }
    if (seen.has(exported.name)) {
      console.warn(`[modules] 模块名重复：${exported.name}（${path}），已跳过`)
      continue
    }
    seen.add(exported.name)
    if (exported.locales) {
      for (const [locale, messages] of Object.entries(exported.locales)) {
        ctx.i18n.global.mergeLocaleMessage(locale, messages as Record<string, unknown>)
      }
    }
    if (exported.routes && exported.routes.length) {
      for (const route of exported.routes) {
        // 模块路由默认插到根布局（'/'）下，name 必须保证全局唯一
        const root = ctx.router.getRoutes().find((r) => r.path === '/')
        if (!root || !root.name) {
          ctx.router.addRoute(route)
        } else {
          ctx.router.addRoute(root.name, route)
        }
      }
    }
    if (typeof exported.install === 'function') {
      await exported.install(ctx)
    }
    loaded.push(exported)
  }
  if (loaded.length) {
    console.info(
      `[modules] 已加载 ${loaded.length} 个前端模块: ${loaded.map((m) => m.name).join(', ')}`
    )
  }
  // 异步探测后端实际加载的模块清单（失败静默不阻塞 UI）。
  // 用动态 import 避开 main.ts 加载顺序问题（pinia 在 createApp 之后才装）。
  void import('@/stores/modules').then(({ useModulesStore }) => {
    void useModulesStore().probe()
  })
  return loaded
}

/** 用于测试 / 调试：返回当前已加载模块的快照 */
export function getLoadedModules(): readonly ScaffoldFrontendModule[] {
  return loaded
}
