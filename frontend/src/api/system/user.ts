import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface UserRecord {
  userId?: number
  deptId?: number
  userName?: string
  nickName?: string
  email?: string
  phonenumber?: string
  sex?: string
  status?: string
  remark?: string
  password?: string
  postIds?: number[]
  roleIds?: number[]
  dept?: { deptName?: string } | null
  createTime?: string
  loginDate?: string
  [key: string]: unknown
}

export interface UserQuery {
  pageNum?: number
  pageSize?: number
  userName?: string
  phonenumber?: string
  status?: string
  deptId?: number
  beginTime?: string
  endTime?: string
}

export const listUser = (query: UserQuery) =>
  request.get<PageResult<UserRecord>, PageResult<UserRecord>>('/system/user/list', { params: query })

export const getUser = (userId?: number | string) =>
  request.get<ApiResult, ApiResult>(`/system/user/${userId ?? ''}`)

export const addUser = (data: UserRecord) =>
  request.post<ApiResult, ApiResult>('/system/user', data)

export const updateUser = (data: UserRecord) =>
  request.put<ApiResult, ApiResult>('/system/user', data)

export const delUser = (userId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/system/user/${userId}`)

export const resetUserPwd = (userId: number, password: string) =>
  request.put<ApiResult, ApiResult>('/system/user/resetPwd', { userId, password })

export const changeUserStatus = (userId: number, status: string) =>
  request.put<ApiResult, ApiResult>('/system/user/changeStatus', { userId, status })

export const deptTreeSelect = () =>
  request.get<ApiResult, ApiResult>('/system/user/deptTree')

export const getAuthRole = (userId: number) =>
  request.get<ApiResult, ApiResult>(`/system/user/authRole/${userId}`)

export const updateAuthRole = (params: { userId: number; roleIds: string }) =>
  request.put<ApiResult, ApiResult>('/system/user/authRole', null, { params })
