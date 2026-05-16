import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface RoleRecord {
  roleId?: number
  roleName?: string
  roleKey?: string
  roleSort?: number
  status?: string
  dataScope?: string
  menuCheckStrictly?: boolean
  deptCheckStrictly?: boolean
  remark?: string
  menuIds?: number[]
  deptIds?: number[]
  createTime?: string
}

export interface RoleQuery {
  pageNum?: number
  pageSize?: number
  roleName?: string
  roleKey?: string
  status?: string
}

export const listRole = (query: RoleQuery) =>
  request.get<PageResult<RoleRecord>, PageResult<RoleRecord>>('/system/role/list', { params: query })

export const getRole = (roleId: number) =>
  request.get<ApiResult<RoleRecord>, ApiResult<RoleRecord>>(`/system/role/${roleId}`)

export const addRole = (data: RoleRecord) =>
  request.post<ApiResult, ApiResult>('/system/role', data)

export const updateRole = (data: RoleRecord) =>
  request.put<ApiResult, ApiResult>('/system/role', data)

export const delRole = (roleId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/system/role/${roleId}`)

export const changeRoleStatus = (roleId: number, status: string) =>
  request.put<ApiResult, ApiResult>('/system/role/changeStatus', { roleId, status })

export const dataScope = (data: RoleRecord) =>
  request.put<ApiResult, ApiResult>('/system/role/dataScope', data)

export interface DeptTreeNode {
  id: number
  label: string
  children?: DeptTreeNode[]
}

export interface DeptTreeSelectResult {
  checkedKeys: number[]
  depts: DeptTreeNode[]
}

/**
 * 后端返回字段：{ code, msg, checkedKeys, depts }（外层不是标准 data 包裹）。
 * 这里把它当成扁平 ApiResult & DeptTreeSelectResult 处理，避免改后端契约。
 */
export const deptTreeSelect = (roleId: number) =>
  request.get<ApiResult & DeptTreeSelectResult, ApiResult & DeptTreeSelectResult>(
    `/system/role/deptTree/${roleId}`
  )
