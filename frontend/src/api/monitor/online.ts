import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface OnlineUserRecord {
  tokenId?: string
  userName?: string
  ipaddr?: string
  loginLocation?: string
  browser?: string
  os?: string
  loginTime?: number
}

export const listOnline = (query: { pageNum?: number; pageSize?: number; ipaddr?: string; userName?: string } = {}) =>
  request.get<PageResult<OnlineUserRecord>, PageResult<OnlineUserRecord>>('/monitor/online/list', { params: query })

export const forceLogout = (tokenId: string) =>
  request.delete<ApiResult, ApiResult>(`/monitor/online/${tokenId}`)
