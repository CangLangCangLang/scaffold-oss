/**
 * `@wangeditor/editor-for-vue` 在 5.x package.json 的 exports 里把类型声明压在
 * `dist/src/index.d.ts`，TS（vue-tsc 在严格模式 + bundler resolution 下）解析不到，
 * 这里手动转发一下，让 ArticleEditor.vue 的 `defineAsyncComponent(() => import(...))`
 * 不再 implicitly any。<br>
 * 仅声明本项目用到的两个组件，避免拉一份完整的官方 d.ts。
 */
declare module '@wangeditor/editor-for-vue' {
  import type { DefineComponent } from 'vue'
  export const Editor: DefineComponent<Record<string, unknown>>
  export const Toolbar: DefineComponent<Record<string, unknown>>
}
