import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

export interface DeptRecord {
  deptId?: number
  parentId?: number
  ancestors?: string
  deptName?: string
  orderNum?: number
  leader?: string
  phone?: string
  email?: string
  status?: string
  children?: DeptRecord[]
}

export const listDept = (query: { deptName?: string; status?: string } = {}) =>
  request.get<ApiResult<DeptRecord[]>, ApiResult<DeptRecord[]>>('/system/dept/list', { params: query })

export const listDeptExcludeChild = (deptId: number) =>
  request.get<ApiResult<DeptRecord[]>, ApiResult<DeptRecord[]>>(`/system/dept/list/exclude/${deptId}`)

export const getDept = (deptId: number) =>
  request.get<ApiResult<DeptRecord>, ApiResult<DeptRecord>>(`/system/dept/${deptId}`)

export const addDept = (data: DeptRecord) =>
  request.post<ApiResult, ApiResult>('/system/dept', data)

export const updateDept = (data: DeptRecord) =>
  request.put<ApiResult, ApiResult>('/system/dept', data)

export const delDept = (deptId: number) =>
  request.delete<ApiResult, ApiResult>(`/system/dept/${deptId}`)
