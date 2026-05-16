import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

/**
 * 文件中心前端 API（M-6）。
 *
 * 与后端 controllers 一一对应：/file/file/* /file/folder/* /file/share/* /file/download/{id}。
 */

export interface SysFile {
  id: number
  bucket?: string
  folderId?: number | null
  name: string
  originalName: string
  ext?: string
  mime?: string
  sizeBytes: number
  storagePath: string
  category?: string
  tags?: string
  refCount?: number
  delFlag?: '0' | '2'
  deleteTime?: string
  createBy?: string
  createByName?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
  remark?: string
}

export interface SysFileFolder {
  id: number
  owner: string
  parentId: number
  name: string
  path: string
  delFlag?: '0' | '2'
  createTime?: string
  updateTime?: string
}

export interface SysFileShare {
  id: number
  fileId: number
  token: string
  expireAt?: string
  oneTime?: '0' | '1'
  visits?: number
  status?: '0' | '1' | '2'
  createBy?: string
  createTime?: string
}

export interface SysFileRef {
  id: number
  fileId: number
  refModule: string
  refType: string
  refId: string
  createBy?: string
  createTime?: string
}

export interface FileQuery {
  name?: string
  createBy?: string
  bucket?: string
  ext?: string
  mime?: string
  category?: string
  folderId?: number
  beginTime?: string
  endTime?: string
  minBytes?: number
  maxBytes?: number
  delFlag?: '0' | '2'
  pageNum?: number
  pageSize?: number
}

export interface FileEditRequest {
  id: number
  name?: string
  folderId?: number
  category?: string
  tags?: string
  remark?: string
}

export interface FolderRequest {
  id?: number
  parentId?: number
  name: string
}

export interface ShareCreateRequest {
  fileId: number
  expireDays?: number
  oneTime?: '0' | '1'
  password?: string
}

export interface PageResult<T> {
  rows: T[]
  total: number
  code: number
  msg: string
}

/* ========== file ========== */

export const listFiles = (params: FileQuery) =>
  request.get<PageResult<SysFile>, PageResult<SysFile>>('/file/file', { params })

export const getFile = (id: number) =>
  request.get<ApiResult<SysFile>, ApiResult<SysFile>>(`/file/file/${id}`)

export const editFile = (body: FileEditRequest) =>
  request.put<ApiResult<unknown>, ApiResult<unknown>>('/file/file', body)

export const removeFile = (id: number) =>
  request.delete<ApiResult<unknown>, ApiResult<unknown>>(`/file/file/${id}`)

export const batchRemoveFiles = (ids: number[]) =>
  request.delete<ApiResult<unknown>, ApiResult<unknown>>('/file/file/batch', { data: ids })

export const purgeFile = (id: number) =>
  request.delete<ApiResult<unknown>, ApiResult<unknown>>(`/file/file/purge/${id}`)

export const purgeNow = (retainDays?: number) =>
  request.post<ApiResult<unknown>, ApiResult<unknown>>('/file/file/purge-now', null, {
    params: retainDays !== undefined ? { retainDays } : {}
  })

export const listFileRefs = (id: number) =>
  request.get<ApiResult<SysFileRef[]>, ApiResult<SysFileRef[]>>(`/file/file/${id}/refs`)

/* ========== folder ========== */

export const listFolders = (owner?: string) =>
  request.get<ApiResult<SysFileFolder[]>, ApiResult<SysFileFolder[]>>('/file/folder', {
    params: owner ? { owner } : {}
  })

export const addFolder = (body: FolderRequest) =>
  request.post<ApiResult<SysFileFolder>, ApiResult<SysFileFolder>>('/file/folder', body)

export const renameFolder = (body: FolderRequest) =>
  request.put<ApiResult<unknown>, ApiResult<unknown>>('/file/folder', body)

export const removeFolder = (id: number) =>
  request.delete<ApiResult<unknown>, ApiResult<unknown>>(`/file/folder/${id}`)

/* ========== share ========== */

export const listMyShares = () =>
  request.get<ApiResult<SysFileShare[]>, ApiResult<SysFileShare[]>>('/file/share')

export const createShare = (body: ShareCreateRequest) =>
  request.post<ApiResult<SysFileShare>, ApiResult<SysFileShare>>('/file/share', body)

export const disableShare = (id: number) =>
  request.put<ApiResult<unknown>, ApiResult<unknown>>(`/file/share/${id}/disable`)

export const removeShare = (id: number) =>
  request.delete<ApiResult<unknown>, ApiResult<unknown>>(`/file/share/${id}`)

/* ========== download / share access ==========
   下载是浏览器导航 / fetch blob 场景，不走 axios 拦截。这里仅给前端拼 URL；
   token 自动通过 cookie / Authorization header 由 axios baseURL 同源注入。 */

export const downloadUrl = (id: number): string => `/file/download/${id}`

export const shareAccessUrl = (token: string, password?: string): string => {
  const base = `/file/share/access/${token}`
  return password ? `${base}?password=${encodeURIComponent(password)}` : base
}
