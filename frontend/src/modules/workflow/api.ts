import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

export interface ProcessDefinitionView {
  id: string
  key: string
  name?: string
  version: number
  description?: string
  resourceName?: string
  deploymentId?: string
  suspended?: boolean
  deploymentTime?: string
}

export interface ProcessInstanceView {
  id: string
  processDefinitionId?: string
  processDefinitionKey?: string
  processDefinitionName?: string
  businessKey?: string
  startUserId?: string
  startTime?: string
  endTime?: string
  activityId?: string
  ended?: boolean
  suspended?: boolean
}

export interface TaskView {
  id: string
  name?: string
  description?: string
  assignee?: string
  owner?: string
  processInstanceId?: string
  processDefinitionId?: string
  processDefinitionKey?: string
  processDefinitionName?: string
  businessKey?: string
  taskDefinitionKey?: string
  createTime?: string
  claimTime?: string
  endTime?: string
  dueDate?: string
  priority?: number
  suspended?: boolean
  /**
   * 当前任务被前加签阻塞的子任务 id 列表（只在待办列表里有值）；
   * 非空说明用户必须等加签人完成才可提交，前端按 disable + tag 渲染。
   */
  blockedByTaskIds?: string[]
}

export interface StartProcessRequest {
  processDefinitionKey: string
  businessKey?: string
  name?: string
  variables?: Record<string, unknown>
}

export interface CompleteTaskRequest {
  comment?: string
  /** 系统级变量（覆盖 formData 中同名 key） */
  variables?: Record<string, unknown>
  /** 动态表单产出的字段集合 */
  formData?: Record<string, unknown>
}

export type TimelineEntryCode =
  | 'process.start'
  | 'process.end'
  | 'activity.start'
  | 'activity.end'
  | 'task.complete'
  | 'task.cc'
  | 'task.addsign.after'
  | 'task.addsign.before'
  | 'task.sendback'
  | 'task.comment'

export interface TimelineEntry {
  /** 后端 enum name，例如 "TASK_CC"，前端建议用 code 字段 */
  type?: string
  /** 字符串形式 type，例如 "task.cc" */
  code: TimelineEntryCode
  occurredAt: string
  actor?: string
  /** 关联活动节点 ID（任务 / 流程节点）*/
  activityId?: string
  taskId?: string
  /** 描述文本，前端可直接展示 */
  message: string
  /** 额外结构化字段，按 type 不同携带 */
  extra?: Record<string, unknown>
}

export interface CcRequest {
  receiverUserIds: string[]
  comment?: string
  occurredAt?: string
}

export interface AddSignRequest {
  assignee: string
  comment?: string
}

/** 前加签：在当前任务之前插入一个审批人；原任务被阻塞直到加签人完成。 */
export interface AddSignBeforeRequest {
  assignee: string
  comment?: string
}

export interface SendBackRequest {
  targetActivityId?: string
  comment: string
}

export interface ProcessRuntimeStateView {
  processInstanceId: string
  processDefinitionId?: string
  activeActivityIds?: string[]
  completedActivityIds?: string[]
  rejectedActivityIds?: string[]
  ended?: boolean
  startTime?: string
  endTime?: string
}

export const listProcessDefinitions = (keyword?: string) =>
  request.get<ApiResult<ProcessDefinitionView[]>, ApiResult<ProcessDefinitionView[]>>(
    '/workflow/process/definitions',
    { params: keyword ? { keyword } : undefined }
  )

export const getBpmnXml = (processDefinitionId: string) =>
  request.get<ApiResult<{ xml: string }>, ApiResult<{ xml: string }>>(
    `/workflow/process/definitions/${encodeURIComponent(processDefinitionId)}/xml`
  )

/**
 * 列出某 processDefinitionKey 的所有历史版本（按 version 倒序），给"版本对比"下拉用。
 * 注意：与 `listProcessDefinitions` 不同 —— 后者只返每个 key 的最新激活版，这里全量含 suspended。
 */
export const listVersionsByKey = (key: string) =>
  request.get<ApiResult<ProcessDefinitionView[]>, ApiResult<ProcessDefinitionView[]>>(
    `/workflow/process/definitions/by-key/${encodeURIComponent(key)}/versions`
  )

export const deployBpmn = (file: File, name?: string) => {
  const form = new FormData()
  form.append('file', file)
  if (name) form.append('name', name)
  return request.post<ApiResult<{ id: string; deploymentTime: string }>, ApiResult<{ id: string; deploymentTime: string }>>(
    '/workflow/process/deployments',
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
}

export const deleteDeployment = (deploymentId: string, cascade = true) =>
  request.delete<ApiResult, ApiResult>(
    `/workflow/process/deployments/${encodeURIComponent(deploymentId)}`,
    { params: { cascade } }
  )

export const startProcess = (req: StartProcessRequest) =>
  request.post<ApiResult<ProcessInstanceView>, ApiResult<ProcessInstanceView>>(
    '/workflow/process/instances',
    req
  )

export const listMyInstances = () =>
  request.get<ApiResult<ProcessInstanceView[]>, ApiResult<ProcessInstanceView[]>>(
    '/workflow/process/instances/mine'
  )

export interface InstanceSearchParams {
  processDefinitionKey?: string
  businessKey?: string
  startUserId?: string
  /** running / finished / all */
  status?: 'running' | 'finished' | 'all'
  pageNum?: number
  pageSize?: number
}

export interface InstancePage {
  rows: ProcessInstanceView[]
  total: number
  code?: number
  msg?: string
}

/**
 * 流程实例分页（admin 视角）。后端会强制非 admin 用 startedBy=current 过滤，前端不必自己处理。
 */
export const searchInstances = (params: InstanceSearchParams) =>
  request.get<InstancePage, InstancePage>('/workflow/process/instances', { params })

export const cancelInstance = (processInstanceId: string, reason?: string) =>
  request.delete<ApiResult, ApiResult>(
    `/workflow/process/instances/${encodeURIComponent(processInstanceId)}`,
    { params: reason ? { reason } : undefined }
  )

export const getInstanceState = (processInstanceId: string) =>
  request.get<ApiResult<ProcessRuntimeStateView>, ApiResult<ProcessRuntimeStateView>>(
    `/workflow/process/instances/${encodeURIComponent(processInstanceId)}/state`
  )

export const getInstanceXml = (processInstanceId: string) =>
  request.get<
    ApiResult<{ xml: string; state: ProcessRuntimeStateView }>,
    ApiResult<{ xml: string; state: ProcessRuntimeStateView }>
  >(`/workflow/process/instances/${encodeURIComponent(processInstanceId)}/xml`)

export const listTodoTasks = (keyword?: string) =>
  request.get<ApiResult<TaskView[]>, ApiResult<TaskView[]>>(
    '/workflow/task/todo',
    { params: keyword ? { keyword } : undefined }
  )

export const listDoneTasks = () =>
  request.get<ApiResult<TaskView[]>, ApiResult<TaskView[]>>('/workflow/task/done')

export const completeTask = (taskId: string, req?: CompleteTaskRequest) =>
  request.post<ApiResult, ApiResult>(
    `/workflow/task/${encodeURIComponent(taskId)}/complete`,
    req ?? {}
  )

export const claimTask = (taskId: string) =>
  request.post<ApiResult, ApiResult>(`/workflow/task/${encodeURIComponent(taskId)}/claim`)

export const unclaimTask = (taskId: string) =>
  request.post<ApiResult, ApiResult>(`/workflow/task/${encodeURIComponent(taskId)}/unclaim`)

export const delegateTask = (taskId: string, targetUserId: string) =>
  request.post<ApiResult, ApiResult>(
    `/workflow/task/${encodeURIComponent(taskId)}/delegate`,
    null,
    { params: { targetUserId } }
  )

export const ccTask = (taskId: string, req: CcRequest) =>
  request.post<ApiResult, ApiResult>(
    `/workflow/task/${encodeURIComponent(taskId)}/cc`,
    req
  )

export const addSignTask = (taskId: string, req: AddSignRequest) =>
  request.post<ApiResult, ApiResult>(
    `/workflow/task/${encodeURIComponent(taskId)}/add-sign`,
    req
  )

export const addSignBeforeTask = (taskId: string, req: AddSignBeforeRequest) =>
  request.post<ApiResult, ApiResult>(
    `/workflow/task/${encodeURIComponent(taskId)}/add-sign-before`,
    req
  )

/** 撤销前加签：仅本子任务的发起人 / admin 可撤；后端会校验 operatorUserId 是否一致。 */
export const cancelAddSignBeforeTask = (childTaskId: string) =>
  request.delete<ApiResult, ApiResult>(
    `/workflow/task/${encodeURIComponent(childTaskId)}/add-sign-before`
  )

export const getInstanceTimeline = (processInstanceId: string) =>
  request.get<ApiResult<TimelineEntry[]>, ApiResult<TimelineEntry[]>>(
    `/workflow/process/instances/${encodeURIComponent(processInstanceId)}/timeline`
  )

export const sendBackTask = (taskId: string, req: SendBackRequest) =>
  request.post<ApiResult, ApiResult>(
    `/workflow/task/${encodeURIComponent(taskId)}/send-back`,
    req
  )

// ---------------- 动态表单 schema ----------------

export interface WfFormSchema {
  id?: number
  processDefinitionKey: string
  activityId?: string
  name?: string
  version?: number
  schemaJson: string
  enabled?: boolean
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

export const ACTIVITY_START_FORM = '__START__'

export const saveFormSchema = (req: WfFormSchema) =>
  request.post<ApiResult<WfFormSchema>, ApiResult<WfFormSchema>>(
    '/workflow/form/schemas',
    req
  )

export const getActiveFormSchema = (processDefinitionKey: string, activityId?: string) =>
  request.get<ApiResult<WfFormSchema | null>, ApiResult<WfFormSchema | null>>(
    '/workflow/form/schemas/active',
    { params: { processDefinitionKey, activityId: activityId ?? ACTIVITY_START_FORM } }
  )

export const listFormSchemasByDef = (processDefinitionKey: string) =>
  request.get<ApiResult<WfFormSchema[]>, ApiResult<WfFormSchema[]>>(
    '/workflow/form/schemas',
    { params: { processDefinitionKey } }
  )

export const getFormSchemaById = (id: number) =>
  request.get<ApiResult<WfFormSchema>, ApiResult<WfFormSchema>>(`/workflow/form/schemas/${id}`)

export const deleteFormSchema = (id: number) =>
  request.delete<ApiResult, ApiResult>(`/workflow/form/schemas/${id}`)
