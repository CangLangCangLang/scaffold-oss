<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  listProcessDefinitions,
  deployBpmn,
  startProcess,
  deleteDeployment,
  getBpmnXml,
  getActiveFormSchema,
  type ProcessDefinitionView
} from '../api'
import BpmnDesigner from '../components/BpmnDesigner.vue'
import BpmnVersionDiffDialog from '../components/BpmnVersionDiffDialog.vue'

const { t } = useI18n()

const router = useRouter()

const loading = ref(false)
const list = ref<ProcessDefinitionView[]>([])
const keyword = ref('')

const uploadVisible = ref(false)
const fileToUpload = ref<File | null>(null)
const deployName = ref('')

const startVisible = ref(false)
const startTarget = ref<ProcessDefinitionView | null>(null)
const startBusinessKey = ref('')
const startName = ref('')
const startVarsRaw = ref<string>('{}')

// 启动表单（动态表单）支持
const startFormRule = ref<any[] | null>(null) // null = 没有 schema，走 JSON 输入；[] = 有 schema 但空（也走 JSON）
const startFormApi = ref<any>(null)
const startFormModel = ref<Record<string, unknown>>({})

function onStartFormApi(api: any) {
  startFormApi.value = api
}

const xmlVisible = ref(false)
const xmlContent = ref('')

const diffVisible = ref(false)
const diffTarget = ref<ProcessDefinitionView | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await listProcessDefinitions(keyword.value || undefined)
    list.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

function onPickFile(e: Event) {
  const target = e.target as HTMLInputElement
  fileToUpload.value = target.files?.[0] ?? null
}

async function handleDeploy() {
  if (!fileToUpload.value) {
    ElMessage.warning(t('workflow.process.pickBpmnTip'))
    return
  }
  await deployBpmn(fileToUpload.value, deployName.value || undefined)
  ElMessage.success(t('workflow.process.deployOk'))
  uploadVisible.value = false
  fileToUpload.value = null
  deployName.value = ''
  await fetchList()
}

async function handleStart() {
  if (!startTarget.value) return
  let vars: Record<string, unknown> = {}
  if (startFormRule.value && startFormRule.value.length > 0 && startFormApi.value) {
    try {
      await startFormApi.value.validate?.()
    } catch {
      ElMessage.error(t('workflow.process.formValidateFailed'))
      return
    }
    vars = startFormApi.value.formData?.() ?? startFormModel.value ?? {}
  } else {
    try {
      vars = startVarsRaw.value.trim() ? JSON.parse(startVarsRaw.value) : {}
    } catch {
      ElMessage.error(t('workflow.process.variablesInvalid'))
      return
    }
  }
  await startProcess({
    processDefinitionKey: startTarget.value.key,
    businessKey: startBusinessKey.value || undefined,
    name: startName.value || undefined,
    variables: vars
  })
  ElMessage.success(t('workflow.process.startOk'))
  startVisible.value = false
}

async function handleDelete(row: ProcessDefinitionView) {
  if (!row.deploymentId) return
  await ElMessageBox.confirm(
    t('workflow.process.deleteConfirm', { name: row.name || row.key }),
    t('workflow.process.dangerTitle'),
    {
      type: 'error',
      confirmButtonText: t('workflow.process.dangerConfirm'),
      cancelButtonText: t('common.cancel')
    }
  )
  await deleteDeployment(row.deploymentId, true)
  ElMessage.success(t('workflow.process.deleteOk'))
  await fetchList()
}

async function viewXml(row: ProcessDefinitionView) {
  const res = await getBpmnXml(row.id)
  xmlContent.value = res.data?.xml || ''
  xmlVisible.value = true
}

async function openStart(row: ProcessDefinitionView) {
  startTarget.value = row
  startBusinessKey.value = ''
  startName.value = ''
  startVarsRaw.value = '{}'
  startFormRule.value = null
  startFormModel.value = {}
  startVisible.value = true

  try {
    const res = await getActiveFormSchema(row.key)
    const data = res.data
    if (data?.schemaJson) {
      try {
        const rule = JSON.parse(data.schemaJson)
        if (Array.isArray(rule) && rule.length > 0) {
          startFormRule.value = rule
        }
      } catch {
        // schema 不是合法 JSON 则降级到 JSON 输入
      }
    }
  } catch {
    // 加载 schema 失败不阻断流程启动
  }
}

function gotoFormDesigner(row: ProcessDefinitionView) {
  void router.push({
    name: 'WorkflowFormDesigner',
    query: { processDefinitionKey: row.key }
  })
}

function openDiff(row: ProcessDefinitionView) {
  diffTarget.value = row
  diffVisible.value = true
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
        <el-form-item :label="t('common.keyword')">
          <el-input
            v-model="keyword"
            :placeholder="t('workflow.process.searchPlaceholder')"
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
          <el-button
            type="success"
            plain
            @click="uploadVisible = true"
          >
            {{ t('workflow.process.deployBtn') }}
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
          prop="key"
          :label="t('workflow.common.processKey')"
          width="180"
        />
        <el-table-column
          prop="name"
          :label="t('common.name')"
          min-width="160"
        />
        <el-table-column
          prop="version"
          :label="t('workflow.common.version')"
          width="80"
          align="center"
        />
        <el-table-column
          prop="resourceName"
          :label="t('workflow.common.definitionResource')"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="deploymentTime"
          :label="t('workflow.common.deployTime')"
          width="180"
        />
        <el-table-column
          :label="t('common.operation')"
          width="430"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="openStart(row)"
            >
              {{ t('workflow.common.start') }}
            </el-button>
            <el-button
              type="primary"
              link
              @click="viewXml(row)"
            >
              {{ t('workflow.common.view') }}
            </el-button>
            <el-button
              type="warning"
              link
              @click="openDiff(row)"
            >
              {{ t('workflow.process.diffBtn') }}
            </el-button>
            <el-button
              type="success"
              link
              @click="gotoFormDesigner(row)"
            >
              {{ t('workflow.common.form') }}
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
            >
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="uploadVisible"
      :title="t('workflow.process.deployTitle')"
      width="520px"
    >
      <el-form label-width="100px">
        <el-form-item :label="t('workflow.process.deployName')">
          <el-input
            v-model="deployName"
            :placeholder="t('workflow.process.deployNameHint')"
          />
        </el-form-item>
        <el-form-item :label="t('workflow.process.bpmnFile')">
          <input
            type="file"
            accept=".bpmn,.xml,.bpmn20.xml"
            @change="onPickFile"
          >
          <div
            v-if="fileToUpload"
            class="upload-hint"
          >
            {{ t('workflow.process.pickedHint', { name: fileToUpload.name, size: Math.round(fileToUpload.size / 1024) }) }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="handleDeploy"
        >
          {{ t('workflow.common.deploy') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="startVisible"
      :title="t('workflow.process.startTitle')"
      width="640px"
    >
      <el-form label-width="100px">
        <el-form-item :label="t('workflow.common.process')">
          {{ startTarget?.name || startTarget?.key }} (v{{ startTarget?.version }})
        </el-form-item>
        <el-form-item :label="t('workflow.process.startName')">
          <el-input
            v-model="startName"
            :placeholder="t('workflow.process.startNameHint')"
          />
        </el-form-item>
        <el-form-item :label="t('workflow.common.businessKey')">
          <el-input
            v-model="startBusinessKey"
            :placeholder="t('workflow.common.businessKeyExample')"
          />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">
        {{ t('workflow.process.startForm') }}
      </el-divider>

      <div v-if="startFormRule && startFormRule.length > 0">
        <form-create
          v-model="startFormModel"
          :rule="startFormRule"
          :option="{ submitBtn: false, resetBtn: false }"
          @api="onStartFormApi"
        />
      </div>
      <el-form
        v-else
        label-width="100px"
      >
        <el-form-item :label="t('workflow.common.variablesJson')">
          <el-input
            v-model="startVarsRaw"
            type="textarea"
            :rows="4"
            placeholder="{}"
          />
          <div class="form-hint">
            {{ t('workflow.process.startNoSchema') }}
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="startVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="handleStart"
        >
          {{ t('workflow.common.start') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="xmlVisible"
      :title="t('workflow.process.xmlTitle')"
      width="80%"
      top="5vh"
    >
      <div style="height: 70vh">
        <BpmnDesigner
          v-if="xmlVisible"
          :model-value="xmlContent"
          readonly
        />
      </div>
    </el-dialog>

    <BpmnVersionDiffDialog
      v-if="diffTarget"
      v-model="diffVisible"
      :process-definition-key="diffTarget.key"
      :process-name="diffTarget.name || diffTarget.key"
    />
  </div>
</template>

<style scoped lang="scss">
.upload-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
