import type { Component } from 'vue'

export type CellRenderType =
  | 'text'
  | 'tag'
  | 'date'
  | 'switch'
  | 'index'
  | 'custom'

export interface TableColumn<T = Record<string, unknown>> {
  prop?: string
  label?: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: boolean | 'left' | 'right'
  showOverflowTooltip?: boolean
  sortable?: boolean | 'custom'
  /** 选中列；type=selection */
  type?: 'selection' | 'index' | 'expand'
  /** 单元格展示模式 */
  render?: CellRenderType
  /** dict 翻译类型，render=tag 时使用 */
  dict?: string
  /** 自定义格式化（render=text 时） */
  formatter?: (row: T, value: unknown) => string
  /** 自定义渲染（render=custom 时配合作用域插槽） */
  slot?: string
  /** switch 展示时的回调 */
  onSwitchChange?: (row: T, value: string) => Promise<void> | void
}

export interface SearchField {
  prop: string
  label: string
  type?: 'input' | 'select' | 'date-range' | 'date'
  placeholder?: string
  options?: Array<{ value: string | number; label: string }>
  /** 自定义渲染 (TS 中只用于扩展能力) */
  component?: Component
}
