import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface DictTypeRecord {
  dictId?: number
  dictName?: string
  dictType?: string
  status?: string
  remark?: string
  createTime?: string
}

export interface DictDataRecord {
  dictCode?: number
  dictSort?: number
  dictLabel?: string
  dictValue?: string
  dictType?: string
  cssClass?: string
  listClass?: string
  isDefault?: string
  status?: string
  remark?: string
  createTime?: string
}

export const listType = (query: { pageNum?: number; pageSize?: number; dictName?: string; dictType?: string; status?: string } = {}) =>
  request.get<PageResult<DictTypeRecord>, PageResult<DictTypeRecord>>('/system/dict/type/list', { params: query })

export const getType = (dictId: number) =>
  request.get<ApiResult<DictTypeRecord>, ApiResult<DictTypeRecord>>(`/system/dict/type/${dictId}`)

export const addType = (data: DictTypeRecord) =>
  request.post<ApiResult, ApiResult>('/system/dict/type', data)

export const updateType = (data: DictTypeRecord) =>
  request.put<ApiResult, ApiResult>('/system/dict/type', data)

export const delType = (dictId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/system/dict/type/${dictId}`)

export const refreshDictCache = () =>
  request.delete<ApiResult, ApiResult>('/system/dict/type/refreshCache')

export const optionselect = () =>
  request.get<ApiResult<DictTypeRecord[]>, ApiResult<DictTypeRecord[]>>('/system/dict/type/optionselect')

export const listData = (query: { pageNum?: number; pageSize?: number; dictType?: string; dictLabel?: string; status?: string } = {}) =>
  request.get<PageResult<DictDataRecord>, PageResult<DictDataRecord>>('/system/dict/data/list', { params: query })

export const getData = (dictCode: number) =>
  request.get<ApiResult<DictDataRecord>, ApiResult<DictDataRecord>>(`/system/dict/data/${dictCode}`)

export const addData = (data: DictDataRecord) =>
  request.post<ApiResult, ApiResult>('/system/dict/data', data)

export const updateData = (data: DictDataRecord) =>
  request.put<ApiResult, ApiResult>('/system/dict/data', data)

export const delData = (dictCode: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/system/dict/data/${dictCode}`)
