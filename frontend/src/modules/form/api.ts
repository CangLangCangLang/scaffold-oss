import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

/**
 * 表单模板状态（与后端 {@code FormTemplateService.STATUS_*} 一一对应）。
 */
export type FormTemplateStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface FormTemplate {
  id: number
  formKey: string
  name: string
  category?: string
  schemaJson: string
  version: number
  status: FormTemplateStatus
  description?: string
  publishedAt?: string
  delFlag?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
  remark?: string
}

export interface FormTemplateSaveRequest {
  id?: number
  formKey?: string
  name: string
  category?: string
  schemaJson: string
  description?: string
}

export interface FormTemplateQuery {
  keyword?: string
  category?: string
  status?: FormTemplateStatus
  pageNum?: number
  pageSize?: number
}

export interface FormSubmission {
  id: number
  templateId: number
  templateKey: string
  templateVersion: number
  submitter: string
  submitterName?: string
  status: 'SUBMITTED'
  data: string
  createTime?: string
  updateTime?: string
}

export interface FormSubmissionRequest {
  templateId: number
  data: string
}

export interface FormSubmissionQuery {
  templateId?: number
  templateKey?: string
  submitter?: string
  beginTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  rows: T[]
  total: number
  code: number
  msg: string
}

/* ===== template ===== */

export const listTemplates = (params: FormTemplateQuery) =>
  request.get<PageResult<FormTemplate>, PageResult<FormTemplate>>('/form/template', { params })

export const getTemplate = (id: number) =>
  request.get<ApiResult<FormTemplate>, ApiResult<FormTemplate>>(`/form/template/${id}`)

export const getActiveTemplate = (formKey: string) =>
  request.get<ApiResult<FormTemplate | null>, ApiResult<FormTemplate | null>>(
    '/form/template/active',
    { params: { formKey } }
  )

export const addTemplate = (body: FormTemplateSaveRequest) =>
  request.post<ApiResult<FormTemplate>, ApiResult<FormTemplate>>('/form/template', body)

export const editTemplate = (body: FormTemplateSaveRequest) =>
  request.put<ApiResult<FormTemplate>, ApiResult<FormTemplate>>('/form/template', body)

export const publishTemplate = (id: number) =>
  request.post<ApiResult<FormTemplate>, ApiResult<FormTemplate>>(`/form/template/${id}/publish`)

export const archiveTemplate = (id: number) =>
  request.post<ApiResult<FormTemplate>, ApiResult<FormTemplate>>(`/form/template/${id}/archive`)

export const removeTemplate = (id: number) =>
  request.delete<ApiResult<unknown>, ApiResult<unknown>>(`/form/template/${id}`)

/* ===== submission ===== */

export const submitForm = (body: FormSubmissionRequest) =>
  request.post<ApiResult<FormSubmission>, ApiResult<FormSubmission>>('/form/submission', body)

export const listSubmissions = (params: FormSubmissionQuery) =>
  request.get<PageResult<FormSubmission>, PageResult<FormSubmission>>('/form/submission', {
    params
  })

export const getSubmission = (id: number) =>
  request.get<ApiResult<FormSubmission>, ApiResult<FormSubmission>>(`/form/submission/${id}`)
