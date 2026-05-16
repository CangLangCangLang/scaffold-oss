import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

export interface CacheInfo {
  info?: Record<string, string>
  dbSize?: number
  commandStats?: Array<{ name: string; value: string }>
}

export interface CacheNameItem {
  cacheName: string
  cacheKey?: string
  cacheValue?: string
  remark?: string
}

export const getCacheInfo = () =>
  request.get<ApiResult<CacheInfo>, ApiResult<CacheInfo>>('/monitor/cache')

export const listCacheName = () =>
  request.get<ApiResult<CacheNameItem[]>, ApiResult<CacheNameItem[]>>('/monitor/cache/getNames')

export const listCacheKey = (cacheName: string) =>
  request.get<ApiResult<string[]>, ApiResult<string[]>>(`/monitor/cache/getKeys/${cacheName}`)

export const getCacheValue = (cacheName: string, cacheKey: string) =>
  request.get<ApiResult<CacheNameItem>, ApiResult<CacheNameItem>>(`/monitor/cache/getValue/${cacheName}/${cacheKey}`)

export const clearCacheName = (cacheName: string) =>
  request.delete<ApiResult, ApiResult>(`/monitor/cache/clearCacheName/${cacheName}`)

export const clearCacheKey = (cacheKey: string) =>
  request.delete<ApiResult, ApiResult>(`/monitor/cache/clearCacheKey/${cacheKey}`)

export const clearCacheAll = () =>
  request.delete<ApiResult, ApiResult>('/monitor/cache/clearCacheAll')
