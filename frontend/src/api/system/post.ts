import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface PostRecord {
  postId?: number
  postCode?: string
  postName?: string
  postSort?: number
  status?: string
  remark?: string
  createTime?: string
}

export const listPost = (query: { pageNum?: number; pageSize?: number; postCode?: string; postName?: string; status?: string } = {}) =>
  request.get<PageResult<PostRecord>, PageResult<PostRecord>>('/system/post/list', { params: query })

export const getPost = (postId: number) =>
  request.get<ApiResult<PostRecord>, ApiResult<PostRecord>>(`/system/post/${postId}`)

export const addPost = (data: PostRecord) =>
  request.post<ApiResult, ApiResult>('/system/post', data)

export const updatePost = (data: PostRecord) =>
  request.put<ApiResult, ApiResult>('/system/post', data)

export const delPost = (postId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/system/post/${postId}`)
