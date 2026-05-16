import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

export interface MenuRecord {
  menuId?: number
  parentId?: number
  menuName?: string
  menuType?: string
  orderNum?: number
  path?: string
  component?: string
  query?: string
  isFrame?: string
  isCache?: string
  visible?: string
  status?: string
  perms?: string
  icon?: string
  remark?: string
  children?: MenuRecord[]
}

export const listMenu = (query: { menuName?: string; status?: string } = {}) =>
  request.get<ApiResult<MenuRecord[]>, ApiResult<MenuRecord[]>>('/system/menu/list', { params: query })

export const getMenu = (menuId: number) =>
  request.get<ApiResult<MenuRecord>, ApiResult<MenuRecord>>(`/system/menu/${menuId}`)

export const treeselect = () =>
  request.get<ApiResult, ApiResult>('/system/menu/treeselect')

export const roleMenuTreeselect = (roleId: number) =>
  request.get<ApiResult, ApiResult>(`/system/menu/roleMenuTreeselect/${roleId}`)

export const addMenu = (data: MenuRecord) =>
  request.post<ApiResult, ApiResult>('/system/menu', data)

export const updateMenu = (data: MenuRecord) =>
  request.put<ApiResult, ApiResult>('/system/menu', data)

export const delMenu = (menuId: number) =>
  request.delete<ApiResult, ApiResult>(`/system/menu/${menuId}`)
