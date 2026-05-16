<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  listTodoTasks,
  completeTask,
  claimTask,
  delegateTask,
  ccTask,
  addSignTask,
  addSignBeforeTask,
  sendBackTask,
  getInstanceState,
  getActiveFormSchema,
  type TaskView
} from '../api'
import ProcessProgressDialog from '../components/ProcessProgressDialog.vue'

const { t } = useI18n()

const list = ref<TaskView[]>([])
const loading = ref(false)
const keyword = ref('')

const completeVisible = ref(false)
const completeTarget = ref<TaskView | null>(null)
const completeComment = ref('')
const completeVarsRaw = ref('{}')

// 任务级动态表单：openComplete 时按 (processDefinitionKey, taskDefinitionKey) 取
type FormCreateApi = { validate?: () => Promise<unknown>; formData?: () => Record<string, unknown> }
const completeFormRule = ref<unknown[] | null>(null)
const completeFormApi = ref<FormCreateApi | null>(null)
const completeFormModel = ref<Record<string, unknown>>({})
const completeFormSchemaName = ref('')

function onCompleteFormApi(api: FormCreateApi) {
  completeFormApi.value = api
}

const progressVisible = ref(false)
const progressTarget = ref<TaskView | null>(null)

// 抄送
const ccVisible = ref(false)
const ccTarget = ref<TaskView | null>(null)
const ccReceiversRaw = ref('')
const ccComment = ref('')

// 后加签
const addSignVisible = ref(false)
const addSignTarget = ref<TaskView | null>(null)
const addSignAssignee = ref('')
const addSignComment = ref('')

// 前加签
const addSignBeforeVisible = ref(false)
const addSignBeforeTarget = ref<TaskView | null>(null)
const addSignBeforeAssignee = ref('')
const addSignBeforeComment = ref('')

// 退回
const sendBackVisible = ref(false)
const sendBackTarget = ref<TaskView | null>(null)
const sendBackComment = ref('')
const sendBackTargetActivityId = ref('')
const sendBackTargetOptions = ref<string[]>([])
const sendBackLoading = ref(false)

function isBlocked(row: TaskView): boolean {
  return Array.isArray(row.blockedByTaskIds) && row.blockedByTaskIds.length > 0
}

function openProgress(row: TaskView) {
  progressTarget.value = row
  progressVisible.value = true
}

function openCc(row: TaskView) {
  ccTarget.value = row
  ccReceiversRaw.value = ''
  ccComment.value = ''
  ccVisible.value = true
}

async function handleCc() {
  if (!ccTarget.value) return
  const receivers = ccReceiversRaw.value
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  if (receivers.length === 0) {
    ElMessage.error(t('workflow.todo.ccReceiversRequired'))
    return
  }
  await ccTask(ccTarget.value.id, { receiverUserIds: receivers, comment: ccComment.value || undefined })
  ElMessage.success(t('workflow.todo.ccDoneCount', { n: receivers.length }))
  ccVisible.value = false
}

function openAddSign(row: TaskView) {
  addSignTarget.value = row
  addSignAssignee.value = ''
  addSignComment.value = ''
  addSignVisible.value = true
}

async function handleAddSign() {
  if (!addSignTarget.value) return
  if (!addSignAssignee.value) {
    ElMessage.error(t('workflow.todo.addSignAssigneeRequired'))
    return
  }
  await addSignTask(addSignTarget.value.id, {
    assignee: addSignAssignee.value,
    comment: addSignComment.value || undefined
  })
  ElMessage.success(t('workflow.todo.addSignDoneOk'))
  addSignVisible.value = false
  await fetchList()
}

function openAddSignBefore(row: TaskView) {
  addSignBeforeTarget.value = row
  addSignBeforeAssignee.value = ''
  addSignBeforeComment.value = ''
  addSignBeforeVisible.value = true
}

async function handleAddSignBefore() {
  if (!addSignBeforeTarget.value) return
  if (!addSignBeforeAssignee.value) {
    ElMessage.error(t('workflow.todo.addSignAssigneeRequired'))
    return
  }
  await addSignBeforeTask(addSignBeforeTarget.value.id, {
    assignee: addSignBeforeAssignee.value,
    comment: addSignBeforeComment.value || undefined
  })
  ElMessage.success(t('workflow.todo.addSignBeforeOk'))
  addSignBeforeVisible.value = false
  await fetchList()
}

async function openSendBack(row: TaskView) {
  sendBackTarget.value = row
  sendBackComment.value = ''
  sendBackTargetActivityId.value = ''
  sendBackTargetOptions.value = []
  sendBackVisible.value = true
  if (!row.processInstanceId) return
  sendBackLoading.value = true
  try {
    const res = await getInstanceState(row.processInstanceId)
    sendBackTargetOptions.value = res.data?.completedActivityIds ?? []
  } finally {
    sendBackLoading.value = false
  }
}

async function handleSendBack() {
  if (!sendBackTarget.value) return
  if (!sendBackComment.value) {
    ElMessage.error(t('workflow.todo.sendBackReasonRequired'))
    return
  }
  await sendBackTask(sendBackTarget.value.id, {
    targetActivityId: sendBackTargetActivityId.value || undefined,
    comment: sendBackComment.value
  })
  ElMessage.success(t('workflow.todo.sendBackOk'))
  sendBackVisible.value = false
  await fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listTodoTasks(keyword.value || undefined)
    list.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

async function openComplete(row: TaskView) {
  completeTarget.value = row
  completeComment.value = ''
  completeVarsRaw.value = '{}'
  completeFormRule.value = null
  completeFormModel.value = {}
  completeFormApi.value = null
  completeFormSchemaName.value = ''
  completeVisible.value = true

  const defKey = row.processDefinitionKey
  const actKey = row.taskDefinitionKey
  if (!defKey || !actKey) return
  try {
    const res = await getActiveFormSchema(defKey, actKey)
    const data = res.data
    if (data?.schemaJson) {
      try {
        const rule = JSON.parse(data.schemaJson)
        if (Array.isArray(rule) && rule.length > 0) {
          completeFormRule.value = rule
          completeFormSchemaName.value = `${data.name ?? actKey} v${data.version ?? '?'}`
        }
      } catch {
        // 非法 JSON：保持降级到原 JSON 输入
      }
    }
  } catch {
    // 加载 schema 失败不阻断完成任务
  }
}

async function handleComplete() {
  if (!completeTarget.value) return
  let formData: Record<string, unknown> | undefined
  let variables: Record<string, unknown> | undefined

  if (completeFormRule.value && completeFormRule.value.length > 0) {
    if (completeFormApi.value?.validate) {
      try {
        await completeFormApi.value.validate()
      } catch {
        ElMessage.error(t('workflow.process.formValidateFailed'))
        return
      }
    }
    formData = completeFormApi.value?.formData?.() ?? completeFormModel.value ?? {}
  } else {
    try {
      variables = completeVarsRaw.value.trim() ? JSON.parse(completeVarsRaw.value) : {}
    } catch {
      ElMessage.error(t('workflow.process.variablesInvalid'))
      return
    }
  }

  try {
    await completeTask(completeTarget.value.id, {
      comment: completeComment.value || undefined,
      variables,
      formData
    })
    ElMessage.success(t('workflow.todo.completedOk'))
    completeVisible.value = false
    await fetchList()
  } catch (e) {
    // 后端会用 ServiceException 把 “被前加签阻塞” 之类信息回传，request 拦截器会弹通用错误，
    // 这里再补一次 console 方便定位
    console.warn('completeTask failed', e)
  }
}

async function handleClaim(row: TaskView) {
  await claimTask(row.id)
  ElMessage.success(t('workflow.todo.claimedOk'))
  await fetchList()
}

async function handleDelegate(row: TaskView) {
  try {
    const { value } = await ElMessageBox.prompt(
      t('workflow.todo.delegatePrompt'),
      t('workflow.todo.delegateTitle'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel')
      }
    )
    await delegateTask(row.id, value)
    ElMessage.success(t('workflow.todo.delegatedOk'))
    await fetchList()
  } catch {
    // cancel
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="scaffold-page">
    <div class="scaffold-card">
      <el-form
        inline
        @submit.prevent="fetchList"
      >
        <el-form-item :label="t('workflow.common.taskName')">
          <el-input
            v-model="keyword"
            :placeholder="t('workflow.todo.taskNamePlaceholder')"
            clearable
            style="width: 220px"
            @clear="fetchList"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="fetchList"
          >
            {{ t('common.search') }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        border
      >
        <el-table-column
          :label="t('workflow.common.taskName')"
          min-width="200"
        >
          <template #default="{ row }">
            <span>{{ row.name }}</span>
            <el-tag
              v-if="row.blockedByTaskIds && row.blockedByTaskIds.length > 0"
              type="warning"
              size="small"
              effect="dark"
              class="presign-blocked-tag"
              :title="t('workflow.todo.blockedTitle', { n: row.blockedByTaskIds.length, ids: row.blockedByTaskIds.join(', ') })"
            >
              {{ t('workflow.todo.blocked', { n: row.blockedByTaskIds.length }) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="processInstanceId"
          :label="t('workflow.common.processInstance')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="processDefinitionId"
          :label="t('workflow.common.processDefinition')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="businessKey"
          :label="t('workflow.common.businessKey')"
          width="140"
        />
        <el-table-column
          prop="createTime"
          :label="t('common.arrivedAt')"
          width="180"
        />
        <el-table-column
          prop="priority"
          :label="t('workflow.common.priority')"
          width="80"
          align="center"
        />
        <el-table-column
          :label="t('common.operation')"
          width="500"
          fixed="right"
        >
          <template #default="{ row }">
            <el-tooltip
              v-if="isBlocked(row)"
              :content="t('workflow.todo.blockedHandle')"
              placement="top"
            >
              <span>
                <el-button
                  type="primary"
                  link
                  disabled
                >
                  {{ t('workflow.todo.actions.handle') }}
                </el-button>
              </span>
            </el-tooltip>
            <el-button
              v-else
              type="primary"
              link
              @click="openComplete(row)"
            >
              {{ t('workflow.todo.actions.handle') }}
            </el-button>
            <el-button
              type="primary"
              link
              @click="handleClaim(row)"
            >
              {{ t('workflow.todo.actions.claim') }}
            </el-button>
            <el-button
              type="warning"
              link
              @click="handleDelegate(row)"
            >
              {{ t('workflow.todo.actions.delegate') }}
            </el-button>
            <el-button
              type="success"
              link
              @click="openCc(row)"
            >
              {{ t('workflow.todo.actions.cc') }}
            </el-button>
            <el-button
              type="success"
              link
              @click="openAddSign(row)"
            >
              {{ t('workflow.todo.actions.addSign') }}
            </el-button>
            <el-tooltip
              v-if="isBlocked(row)"
              :content="t('workflow.todo.blockedAddBefore')"
              placement="top"
            >
              <span>
                <el-button
                  type="success"
                  link
                  disabled
                >
                  {{ t('workflow.todo.actions.addSignBefore') }}
                </el-button>
              </span>
            </el-tooltip>
            <el-button
              v-else
              type="success"
              link
              @click="openAddSignBefore(row)"
            >
              {{ t('workflow.todo.actions.addSignBefore') }}
            </el-button>
            <el-button
              type="danger"
              link
              @click="openSendBack(row)"
            >
              {{ t('workflow.todo.actions.sendBack') }}
            </el-button>
            <el-button
              type="info"
              link
              @click="openProgress(row)"
            >
              {{ t('workflow.common.progress') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="completeVisible"
      :title="t('workflow.todo.handleTitle')"
      width="640px"
    >
      <el-form label-width="100px">
        <el-form-item :label="t('workflow.common.taskName')">
          {{ completeTarget?.name }}
          <span
            v-if="completeTarget?.taskDefinitionKey"
            class="task-meta"
          >
            ({{ completeTarget.taskDefinitionKey }})
          </span>
        </el-form-item>
        <el-form-item :label="t('workflow.todo.handleComment')">
          <el-input
            v-model="completeComment"
            type="textarea"
            :rows="3"
            :placeholder="t('workflow.todo.handleCommentHint')"
          />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">
        {{ t('workflow.todo.taskForm') }}
      </el-divider>

      <div v-if="completeFormRule && completeFormRule.length > 0">
        <div class="form-meta">
          {{ t('workflow.todo.schemaLoaded', { name: completeFormSchemaName }) }}
        </div>
        <form-create
          v-model="completeFormModel"
          :rule="completeFormRule"
          :option="{ submitBtn: false, resetBtn: false }"
          @api="onCompleteFormApi"
        />
      </div>
      <el-form
        v-else
        label-width="100px"
      >
        <el-form-item :label="t('workflow.common.variablesJson')">
          <el-input
            v-model="completeVarsRaw"
            type="textarea"
            :rows="4"
            placeholder="{}"
          />
          <div class="form-hint">
            {{ t('workflow.todo.schemaMissing', { key: completeTarget?.taskDefinitionKey || '...' }) }}
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="completeVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="handleComplete"
        >
          {{ t('workflow.todo.completeBtn') }}
        </el-button>
      </template>
    </el-dialog>

    <ProcessProgressDialog
      v-model="progressVisible"
      :process-instance-id="progressTarget?.processInstanceId"
      :title="progressTarget?.name"
    />

    <el-dialog
      v-model="ccVisible"
      :title="t('workflow.todo.ccTitle')"
      width="480px"
    >
      <el-form label-width="100px">
        <el-form-item :label="t('workflow.common.taskName')">
          {{ ccTarget?.name }}
        </el-form-item>
        <el-form-item :label="t('workflow.todo.ccReceivers')">
          <el-input
            v-model="ccReceiversRaw"
            :placeholder="t('workflow.todo.ccReceiversHint')"
          />
        </el-form-item>
        <el-form-item :label="t('workflow.common.comment')">
          <el-input
            v-model="ccComment"
            type="textarea"
            :rows="3"
            :placeholder="t('workflow.todo.ccCommentHint')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ccVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="handleCc"
        >
          {{ t('workflow.todo.ccBtn') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="addSignVisible"
      :title="t('workflow.todo.addSignTitle')"
      width="480px"
    >
      <el-form label-width="100px">
        <el-form-item :label="t('workflow.common.taskName')">
          {{ addSignTarget?.name }}
        </el-form-item>
        <el-form-item :label="t('workflow.common.assignee')">
          <el-input
            v-model="addSignAssignee"
            :placeholder="t('workflow.common.assigneeHint')"
          />
        </el-form-item>
        <el-form-item :label="t('workflow.common.comment')">
          <el-input
            v-model="addSignComment"
            type="textarea"
            :rows="3"
            :placeholder="t('workflow.common.reason')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addSignVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="handleAddSign"
        >
          {{ t('workflow.todo.actions.addSign') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="addSignBeforeVisible"
      :title="t('workflow.todo.addSignBeforeTitle')"
      width="480px"
    >
      <div class="presign-tip">
        {{ t('workflow.todo.addSignBeforeTip') }}
      </div>
      <el-form label-width="100px">
        <el-form-item :label="t('workflow.common.taskName')">
          {{ addSignBeforeTarget?.name }}
        </el-form-item>
        <el-form-item :label="t('workflow.common.assignee')">
          <el-input
            v-model="addSignBeforeAssignee"
            :placeholder="t('workflow.common.assigneeHint')"
          />
        </el-form-item>
        <el-form-item :label="t('workflow.common.comment')">
          <el-input
            v-model="addSignBeforeComment"
            type="textarea"
            :rows="3"
            :placeholder="t('workflow.common.reason')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addSignBeforeVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="handleAddSignBefore"
        >
          {{ t('workflow.todo.actions.addSignBefore') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="sendBackVisible"
      :title="t('workflow.todo.sendBackTitle')"
      width="520px"
    >
      <el-form
        v-loading="sendBackLoading"
        label-width="100px"
      >
        <el-form-item :label="t('workflow.common.taskName')">
          {{ sendBackTarget?.name }}
        </el-form-item>
        <el-form-item :label="t('workflow.todo.sendBackTarget')">
          <el-select
            v-model="sendBackTargetActivityId"
            :placeholder="t('workflow.todo.sendBackTargetHint')"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="aid in sendBackTargetOptions"
              :key="aid"
              :label="aid"
              :value="aid"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('workflow.todo.sendBackReason')">
          <el-input
            v-model="sendBackComment"
            type="textarea"
            :rows="3"
            :placeholder="t('workflow.todo.sendBackReasonHint')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendBackVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="danger"
          @click="handleSendBack"
        >
          {{ t('workflow.todo.actions.sendBack') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.task-meta {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.form-meta {
  margin-bottom: 8px;
  color: var(--el-color-success);
  font-size: 12px;
}
.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.presign-tip {
  margin-bottom: 12px;
  padding: 8px 12px;
  border-left: 3px solid var(--el-color-warning);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
}
.presign-blocked-tag {
  margin-left: 8px;
}
</style>
