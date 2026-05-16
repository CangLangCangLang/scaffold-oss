import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

/**
 * 报表中心前端 API（M-8）。
 *
 * 与后端 controllers 一一对应：
 *   /report/template/* /report/run /report/run/export /report/run/log
 *   /report/dashboard/* /report/datasource/*
 */

export interface SysReportTemplate {
  id?: number
  code: string
  name: string
  category?: string
  datasourceId?: number
  sqlText: string
  paramSchema?: string
  rowLimit?: number
  timeoutMs?: number
  permKey?: string
  status?: '0' | '1'
  remark?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

export interface SysReportDataSource {
  id: number
  code: string
  name: string
  type?: string
  jdbcUrl: string
  driverClass?: string
  username?: string
  passwordMask?: string
  status?: '0' | '1'
  remark?: string
  createBy?: string
  createTime?: string
}

export interface SysReportDashboard {
  id?: number
  code: string
  name: string
  category?: string
  layoutJson?: string
  permKey?: string
  status?: '0' | '1'
  remark?: string
  createBy?: string
  createTime?: string
}

export interface SysReportDashboardCard {
  id?: number
  dashboardId?: number
  templateId: number
  title: string
  chartType: 'table' | 'line' | 'bar' | 'pie' | 'number'
  configJson?: string
  paramJson?: string
  posX?: number
  posY?: number
  posW?: number
  posH?: number
  orderNum?: number
}

export interface SysReportRunLog {
  id: number
  templateId?: number
  templateCode?: string
  datasourceId?: number
  sqlPreview?: string
  paramJson?: string
  rowCount?: number
  costMs?: number
  status?: '0' | '1' | '2'
  errorMsg?: string
  createBy?: string
  createTime?: string
}

export interface RunRequest {
  templateId?: number
  sql?: string
  datasourceId?: number
  params?: Record<string, unknown>
  rowLimit?: number
  timeoutMs?: number
}

export interface RunResult {
  columns: string[]
  columnTypes: string[]
  rows: unknown[][]
  rowCount: number
  truncated: boolean
  costMs: number
  sqlPreview: string
  boundValues?: unknown[]
  runLogId?: number
}

export interface DataSourceUpsertRequest {
  id?: number
  code?: string
  name?: string
  type?: string
  jdbcUrl?: string
  driverClass?: string
  username?: string
  password?: string | null // null = 不动；空串 = 清空；非空 = 加密落库
  status?: '0' | '1'
  remark?: string
}

export interface DashboardSaveRequest {
  dashboard: SysReportDashboard
  cards: SysReportDashboardCard[]
}

export interface PageResult<T> {
  rows: T[]
  total: number
  code: number
  msg: string
}

/** 参数声明：模板 paramSchema 解析后的形态 */
export interface ParamDecl {
  name: string
  type?: 'string' | 'number' | 'date' | 'datetime' | 'boolean'
  label?: string
  required?: boolean
  default?: unknown
  options?: { label: string; value: string | number | boolean }[]
}

/* ========== template ========== */

export const listTemplates = (params: Record<string, unknown>) =>
  request.get<PageResult<SysReportTemplate>, PageResult<SysReportTemplate>>('/report/template', { params })

export const getTemplate = (id: number) =>
  request.get<ApiResult<SysReportTemplate>, ApiResult<SysReportTemplate>>(`/report/template/${id}`)

export const addTemplate = (body: SysReportTemplate) =>
  request.post<ApiResult<number>, ApiResult<number>>('/report/template', body)

export const updateTemplate = (body: SysReportTemplate) =>
  request.put<ApiResult<number>, ApiResult<number>>('/report/template', body)

export const removeTemplate = (id: number) =>
  request.delete<ApiResult<unknown>, ApiResult<unknown>>(`/report/template/${id}`)

export const validateTemplate = (body: SysReportTemplate) =>
  request.post<ApiResult<unknown>, ApiResult<unknown>>('/report/template/validate', body)

/* ========== run / export ========== */

export const runReport = (body: RunRequest) =>
  request.post<ApiResult<RunResult>, ApiResult<RunResult>>('/report/run', body)

export const exportReportUrl = '/report/run/export'

export const listRunLogs = (params: Record<string, unknown>) =>
  request.get<PageResult<SysReportRunLog>, PageResult<SysReportRunLog>>('/report/run/log', { params })

export const purgeLogsNow = (days: number) =>
  request.post<ApiResult<unknown>, ApiResult<unknown>>('/report/run/log/purge-now', null, { params: { days } })

/* ========== dashboard ========== */

export const listDashboards = (params: Record<string, unknown>) =>
  request.get<PageResult<SysReportDashboard>, PageResult<SysReportDashboard>>('/report/dashboard', { params })

export const getDashboard = (id: number) =>
  request.get<ApiResult<{ dashboard: SysReportDashboard; cards: SysReportDashboardCard[] }>,
    ApiResult<{ dashboard: SysReportDashboard; cards: SysReportDashboardCard[] }>>(`/report/dashboard/${id}`)

export const addDashboard = (body: DashboardSaveRequest) =>
  request.post<ApiResult<number>, ApiResult<number>>('/report/dashboard', body)

export const updateDashboard = (body: DashboardSaveRequest) =>
  request.put<ApiResult<number>, ApiResult<number>>('/report/dashboard', body)

export const removeDashboard = (id: number) =>
  request.delete<ApiResult<unknown>, ApiResult<unknown>>(`/report/dashboard/${id}`)

/* ========== datasource ========== */

export const listDataSources = () =>
  request.get<ApiResult<SysReportDataSource[]>, ApiResult<SysReportDataSource[]>>('/report/datasource')

export const getDataSource = (id: number) =>
  request.get<ApiResult<SysReportDataSource>, ApiResult<SysReportDataSource>>(`/report/datasource/${id}`)

export const addDataSource = (body: DataSourceUpsertRequest) =>
  request.post<ApiResult<number>, ApiResult<number>>('/report/datasource', body)

export const updateDataSource = (body: DataSourceUpsertRequest) =>
  request.put<ApiResult<number>, ApiResult<number>>('/report/datasource', body)

export const removeDataSource = (id: number) =>
  request.delete<ApiResult<unknown>, ApiResult<unknown>>(`/report/datasource/${id}`)

export const testDataSource = (body: DataSourceUpsertRequest) =>
  request.post<ApiResult<unknown>, ApiResult<unknown>>('/report/datasource/test', body)
