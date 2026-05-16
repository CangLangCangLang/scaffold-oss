import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface AuditLogRecord {
  id?: number
  traceId?: string
  module?: string
  action?: string
  resourceType?: string
  resourceId?: string
  actor?: string
  actorId?: number
  actorDept?: string
  clientIp?: string
  requestUri?: string
  beforeValue?: string
  afterValue?: string
  diff?: string
  status?: number
  errorMessage?: string
  comment?: string
  costMs?: number
  createdAt?: string
}

export interface AuditLogQuery {
  pageNum?: number
  pageSize?: number
  module?: string
  action?: string
  resourceType?: string
  resourceId?: string
  actor?: string
  status?: number
  fromTime?: string
  toTime?: string
}

export const listAuditLog = (query: AuditLogQuery = {}) =>
  request.get<PageResult<AuditLogRecord>, PageResult<AuditLogRecord>>('/system/audit/log/list', {
    params: query
  })

export const getAuditLog = (id: number) =>
  request.get<ApiResult<AuditLogRecord>, ApiResult<AuditLogRecord>>(`/system/audit/log/${id}`)

export const deleteAuditLogOlder = (retainDays: number) =>
  request.delete<ApiResult<{ affected: number }>, ApiResult<{ affected: number }>>(
    '/system/audit/log/older',
    { params: { retainDays } }
  )

/**
 * RFC 6902 JSON Patch 单条操作。
 */
export interface JsonPatchOp {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test'
  path: string
  value?: unknown
  from?: string
}

/**
 * 解析后端存的 diff（字符串）→ patch 数组；失败返回空数组。
 */
export function parseDiff(diff: string | undefined | null): JsonPatchOp[] {
  if (!diff) return []
  try {
    const parsed = JSON.parse(diff)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}
