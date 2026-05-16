import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'

export interface JobRecord {
  jobId?: number
  jobName?: string
  jobGroup?: string
  invokeTarget?: string
  cronExpression?: string
  misfirePolicy?: string
  concurrent?: string
  status?: string
  remark?: string
  createTime?: string
  nextValidTime?: string
}

export const listJob = (query: { pageNum?: number; pageSize?: number; jobName?: string; jobGroup?: string; status?: string } = {}) =>
  request.get<PageResult<JobRecord>, PageResult<JobRecord>>('/monitor/job/list', { params: query })

export const getJob = (jobId: number) =>
  request.get<ApiResult<JobRecord>, ApiResult<JobRecord>>(`/monitor/job/${jobId}`)

export const addJob = (data: JobRecord) =>
  request.post<ApiResult, ApiResult>('/monitor/job', data)

export const updateJob = (data: JobRecord) =>
  request.put<ApiResult, ApiResult>('/monitor/job', data)

export const delJob = (jobId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/monitor/job/${jobId}`)

export const changeJobStatus = (jobId: number, status: string) =>
  request.put<ApiResult, ApiResult>('/monitor/job/changeStatus', { jobId, status })

export const runJob = (jobId: number, jobGroup: string) =>
  request.put<ApiResult, ApiResult>('/monitor/job/run', { jobId, jobGroup })

export interface JobLogRecord {
  jobLogId?: number
  jobName?: string
  jobGroup?: string
  invokeTarget?: string
  jobMessage?: string
  status?: string
  exceptionInfo?: string
  createTime?: string
}

export const listJobLog = (query: { pageNum?: number; pageSize?: number; jobName?: string; jobGroup?: string; status?: string } = {}) =>
  request.get<PageResult<JobLogRecord>, PageResult<JobLogRecord>>('/monitor/jobLog/list', { params: query })

export const delJobLog = (jobLogId: number | number[]) =>
  request.delete<ApiResult, ApiResult>(`/monitor/jobLog/${jobLogId}`)

export const cleanJobLog = () =>
  request.delete<ApiResult, ApiResult>('/monitor/jobLog/clean')
