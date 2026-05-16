import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface NoticeRecord {
  noticeId?: number
  noticeTitle?: string
  noticeType?: string
  noticeContent?: string
  status?: string
  remark?: string
  createTime?: string
}

export const listNotice = (query: { pageNum?: number; pageSize?: number; noticeTitle?: string; noticeType?: string; createBy?: string } = {}) =>
  request.get<PageResult<NoticeRecord>, PageResult<NoticeRecord>>('/system/notice/list', { params: query })

export const getNotice = (noticeId: number) =>
  request.get<ApiResult<NoticeRecord>, ApiResult<NoticeRecord>>(`/system/notice/${noticeId}`)

export const addNotice = (data: NoticeRecord) =>
  request.post<ApiResult, ApiResult>('/system/notice', data)

export const updateNotice = (data: NoticeRecord) =>
  request.put<ApiResult, ApiResult>('/system/notice', data)

export const delNotice = (noticeId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/system/notice/${noticeId}`)
