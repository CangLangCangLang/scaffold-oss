import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface InboxEntry {
  id: number
  messageId: string
  scope: 'USER' | 'TOPIC'
  target: string
  type: string
  payload: unknown
  /** 0=未读 1=已读 2=已过期 */
  status: number
  createdAt: string
  readAt?: string
  expireAt?: string
}

export interface InboxQuery {
  pageNum?: number
  pageSize?: number
  /** 状态过滤（0/1/2）数组；为空时后端默认 [0,1] */
  statuses?: number[]
  typeKeyword?: string
  fromTime?: string
  toTime?: string
}

export function listUnread(limit = 50) {
  return request.get<ApiResult<InboxEntry[]>, ApiResult<InboxEntry[]>>('/system/inbox/unread', {
    params: { limit }
  })
}

export function countUnread() {
  return request.get<ApiResult<{ count: number }>, ApiResult<{ count: number }>>('/system/inbox/unread-count')
}

export function ackInbox(id: number) {
  return request.post<ApiResult, ApiResult>(`/system/inbox/${id}/ack`)
}

export function ackAllInbox() {
  return request.post<ApiResult<{ count: number }>, ApiResult<{ count: number }>>('/system/inbox/ack-all')
}

/** 全页面：分页查询 */
export function pageInbox(query: InboxQuery = {}) {
  return request.get<PageResult<InboxEntry>, PageResult<InboxEntry>>('/system/inbox/page', {
    params: query
  })
}

/** 批量已读 */
export function ackBatchInbox(ids: number[]) {
  return request.post<ApiResult<{ count: number }>, ApiResult<{ count: number }>>(
    '/system/inbox/ack-batch',
    ids
  )
}

/** 批量删除 */
export function deleteBatchInbox(ids: number[]) {
  return request.delete<ApiResult<{ count: number }>, ApiResult<{ count: number }>>(
    '/system/inbox/batch',
    { data: ids }
  )
}

/** 单条删除 */
export function deleteOneInbox(id: number) {
  return request.delete<ApiResult, ApiResult>(`/system/inbox/${id}`)
}
