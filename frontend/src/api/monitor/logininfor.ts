import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface LoginInforRecord {
  infoId?: number
  userName?: string
  ipaddr?: string
  loginLocation?: string
  browser?: string
  os?: string
  status?: string
  msg?: string
  loginTime?: string
}

export const listLogininfor = (query: { pageNum?: number; pageSize?: number; ipaddr?: string; userName?: string; status?: string; beginTime?: string; endTime?: string } = {}) =>
  request.get<PageResult<LoginInforRecord>, PageResult<LoginInforRecord>>('/monitor/logininfor/list', { params: query })

export const delLogininfor = (infoId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/monitor/logininfor/${infoId}`)

export const unlockLogininfor = (userName: string) =>
  request.get<ApiResult, ApiResult>(`/monitor/logininfor/unlock/${userName}`)

export const cleanLogininfor = () =>
  request.delete<ApiResult, ApiResult>('/monitor/logininfor/clean')
