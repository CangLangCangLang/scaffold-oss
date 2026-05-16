import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface GenTableRecord {
  tableId?: number
  tableName?: string
  tableComment?: string
  className?: string
  packageName?: string
  moduleName?: string
  businessName?: string
  functionName?: string
  functionAuthor?: string
  tplCategory?: string
  genType?: string
  genPath?: string
  remark?: string
  createTime?: string
}

export const listTable = (query: { pageNum?: number; pageSize?: number; tableName?: string; tableComment?: string } = {}) =>
  request.get<PageResult<GenTableRecord>, PageResult<GenTableRecord>>('/tool/gen/list', { params: query })

export const listDbTable = (query: { pageNum?: number; pageSize?: number; tableName?: string; tableComment?: string } = {}) =>
  request.get<PageResult<GenTableRecord>, PageResult<GenTableRecord>>('/tool/gen/db/list', { params: query })

export const getGenTable = (tableId: number) =>
  request.get<ApiResult, ApiResult>(`/tool/gen/${tableId}`)

export const updateGenTable = (data: GenTableRecord) =>
  request.put<ApiResult, ApiResult>('/tool/gen', data)

export const importTable = (params: { tables: string }) =>
  request.post<ApiResult, ApiResult>('/tool/gen/importTable', null, { params })

export const previewTable = (tableId: number) =>
  request.get<ApiResult<Record<string, string>>, ApiResult<Record<string, string>>>(`/tool/gen/preview/${tableId}`)

export const delTable = (tableId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/tool/gen/${tableId}`)

export const genCode = (tableName: string) =>
  request.get<ApiResult, ApiResult>(`/tool/gen/genCode/${tableName}`)

export const synchDb = (tableName: string) =>
  request.get<ApiResult, ApiResult>(`/tool/gen/synchDb/${tableName}`)
