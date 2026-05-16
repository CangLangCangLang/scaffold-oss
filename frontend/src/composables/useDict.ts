import { ref, type Ref } from 'vue'
import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

export interface DictDataItem {
  dictLabel: string
  dictValue: string
  cssClass?: string
  listClass?: string
  raw?: unknown
}

const dictCache = new Map<string, DictDataItem[]>()

async function fetchDict(dictType: string): Promise<DictDataItem[]> {
  const res = await request.get<
    ApiResult<DictDataItem[]>,
    ApiResult<DictDataItem[]>
  >(`/system/dict/data/type/${dictType}`)
  const list = (res.data || []) as DictDataItem[]
  return list.map((item) => ({
    dictLabel: item.dictLabel,
    dictValue: String(item.dictValue),
    cssClass: item.cssClass,
    listClass: item.listClass,
    raw: item
  }))
}

/**
 * 与后端字典数据对齐的组合式 hook，多次调用同一字典类型会复用缓存。
 */
export function useDict(...dictTypes: string[]): Record<string, Ref<DictDataItem[]>> {
  const result: Record<string, Ref<DictDataItem[]>> = {}
  for (const type of dictTypes) {
    const items = ref<DictDataItem[]>(dictCache.get(type) ?? [])
    if (!dictCache.has(type)) {
      fetchDict(type)
        .then((list) => {
          dictCache.set(type, list)
          items.value = list
        })
        .catch(() => {
          dictCache.set(type, [])
        })
    }
    result[type] = items
  }
  return result
}

export function clearDictCache(dictType?: string): void {
  if (dictType) dictCache.delete(dictType)
  else dictCache.clear()
}
