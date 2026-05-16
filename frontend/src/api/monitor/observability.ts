import request from '@/utils/request'

export interface SlowRequest {
  id: number
  requestUri: string
  method: string
  status: number
  costMs: number
  traceId?: string
  username?: string
  clientIp?: string
  reason: 'SLOW' | 'SERVER_ERROR' | 'CLIENT_ERROR'
  exceptionMsg?: string
  alerted: '0' | '1'
  createTime: string
}

export interface SlowRequestListResp {
  rows: SlowRequest[]
  total: number
  pending: number
}

export interface SlowRequestQuery {
  reason?: string
  requestUri?: string
  beginTime?: string
  endTime?: string
}

export function listSlowRequests(query: SlowRequestQuery) {
  return request({
    url: '/monitor/slow-request',
    method: 'get',
    params: query
  })
}

export function purgeSlowRequests(days: number) {
  return request({
    url: '/monitor/slow-request/purge',
    method: 'post',
    params: { days }
  })
}

export function deleteSlowRequest(id: number) {
  return request({
    url: `/monitor/slow-request/${id}`,
    method: 'delete'
  })
}

export function scanSlowAlertNow() {
  return request({
    url: '/monitor/slow-request/scan-now',
    method: 'post'
  })
}

export function listBusinessMetrics() {
  return request({
    url: '/monitor/slow-request/business-metrics',
    method: 'get'
  })
}

export function fetchActuator(path: string) {
  return request({
    url: `/actuator/${path}`,
    method: 'get'
  })
}
