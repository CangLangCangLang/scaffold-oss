import type { Component } from 'vue'
import { reactive, toRaw } from 'vue'

/**
 * 顶部栏组件槽（widget）注册中心。
 * <p>
 * 设计目的：让业务模块（如 inbox）能在 install 钩子里向 TopBar 注入自己的图标 / 弹窗，
 * 而 TopBar 本身不依赖任何模块，从而满足"删模块即下线"的约束。
 *
 * 用法：
 * <pre>
 *   // 在模块的 install(ctx) 中
 *   registerTopBarWidget({ key: 'inbox.bell', component: NotificationBell, order: 10 })
 * </pre>
 */
export interface TopBarWidget {
  /** 全局唯一 key，重复注册会覆盖前者 */
  key: string
  /** 要渲染的组件 */
  component: Component
  /** 排序权重，越小越靠左；缺省 100 */
  order?: number
}

const widgets = reactive<Map<string, TopBarWidget>>(new Map())

export function registerTopBarWidget(widget: TopBarWidget): void {
  widgets.set(widget.key, widget)
}

export function unregisterTopBarWidget(key: string): void {
  widgets.delete(key)
}

/** 给 TopBar 用：返回按 order 排序的快照 */
export function listTopBarWidgets(): TopBarWidget[] {
  return Array.from(widgets.values())
    .map((w) => ({ ...w, component: toRaw(w.component) as Component }))
    .sort((a, b) => (a.order ?? 100) - (b.order ?? 100))
}
