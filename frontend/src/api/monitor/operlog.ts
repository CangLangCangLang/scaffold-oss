import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface OperLogRecord {
  operId?: number
  title?: string
  businessType?: number
  method?: string
  requestMethod?: string
  operatorType?: number
  operName?: string
  deptName?: string
  operUrl?: string
  operIp?: string
  operLocation?: string
  operParam?: string
  jsonResult?: string
  status?: number
  errorMsg?: string
  operTime?: string
  costTime?: number
}

export const listOperlog = (query: { pageNum?: number; pageSize?: number; title?: string; operName?: string; businessType?: number; status?: number; beginTime?: string; endTime?: string } = {}) =>
  request.get<PageResult<OperLogRecord>, PageResult<OperLogRecord>>('/monitor/operlog/list', { params: query })

export const delOperlog = (operId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/monitor/operlog/${operId}`)

export const cleanOperlog = () =>
  request.delete<ApiResult, ApiResult>('/monitor/operlog/clean')
