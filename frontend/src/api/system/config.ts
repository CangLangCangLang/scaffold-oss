import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface ConfigRecord {
  configId?: number
  configName?: string
  configKey?: string
  configValue?: string
  configType?: string
  remark?: string
  createTime?: string
}

export const listConfig = (query: { pageNum?: number; pageSize?: number; configName?: string; configKey?: string; configType?: string } = {}) =>
  request.get<PageResult<ConfigRecord>, PageResult<ConfigRecord>>('/system/config/list', { params: query })

export const getConfig = (configId: number) =>
  request.get<ApiResult<ConfigRecord>, ApiResult<ConfigRecord>>(`/system/config/${configId}`)

export const getConfigKey = (configKey: string) =>
  request.get<ApiResult<string>, ApiResult<string>>(`/system/config/configKey/${configKey}`)

export const addConfig = (data: ConfigRecord) =>
  request.post<ApiResult, ApiResult>('/system/config', data)

export const updateConfig = (data: ConfigRecord) =>
  request.put<ApiResult, ApiResult>('/system/config', data)

export const delConfig = (configId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/system/config/${configId}`)

export const refreshConfigCache = () =>
  request.delete<ApiResult, ApiResult>('/system/config/refreshCache')
