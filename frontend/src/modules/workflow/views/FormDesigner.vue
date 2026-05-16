<script setup lang="ts">
import { computed, defineAsyncComponent, markRaw, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

/**
 * 设计器 ~1MB（拖拽 + codemirror + 大量 element-plus 子组件），仅"表单设计"路由打开时
 * 才需要。defineAsyncComponent 懒加载，与 vite manualChunks 中的
 * vendor-form-create-designer 一一对应，保证主 bundle / TodoList / ProcessList 用 runtime
 * 时不会被拉进来。删除整个工作流模块目录后，这条 chunk 也跟着消失。
 */
const FcDesigner = defineAsyncComponent(async () => {
  const mod = await import('@form-create/designer')
  return markRaw(mod.default)
})
import {
  ACTIVITY_START_FORM,
  getActiveFormSchema,
  getBpmnXml,
  listProcessDefinitions,
  saveFormSchema,
  type ProcessDefinitionView
} from '../api'

const { t } = useI18n()

const route = useRoute()

const definitions = ref<ProcessDefinitionView[]>([])
const processDefinitionKey = ref<string>('')
const activityId = ref<string>(ACTIVITY_START_FORM)
const formName = ref<string>(t('workflow.formDesigner.formStartLabel'))

const schemaRef = ref<unknown[] | null>(null)
const designerRef = ref<{ setRule?: (r: unknown) => void; getRule?: () => unknown[] } | null>(null)
const loading = ref(false)
const saving = ref(false)
const advancedMode = ref(false)

/** BPMN 解析出的可选 userTask activityId 列表 */
const taskOptions = ref<{ id: string; name: string }[]>([])
const taskOptionsLoading = ref(false)

const designerVisible = computed(() => Boolean(processDefinitionKey.value))

async function fetchDefinitions() {
  loading.value = true
  try {
    const res = await listProcessDefinitions()
    definitions.value = res.data ?? []
    if (route.query.processDefinitionKey) {
      processDefinitionKey.value = String(route.query.processDefinitionKey)
    }
    if (!processDefinitionKey.value && definitions.value.length > 0) {
      processDefinitionKey.value = definitions.value[0].key
    }
  } finally {
    loading.value = false
  }
}

/** 通过当前选中流程定义的 BPMN XML 解析出全部 userTask 节点。 */
async function loadTaskOptions() {
  if (!processDefinitionKey.value) return
  const def = definitions.value.find((d) => d.key === processDefinitionKey.value)
  if (!def) return
  taskOptionsLoading.value = true
  try {
    const res = await getBpmnXml(def.id)
    const xml = res.data?.xml
    if (!xml) {
      taskOptions.value = []
      return
    }
    const dom = new DOMParser().parseFromString(xml, 'application/xml')
    const tasks = Array.from(dom.getElementsByTagNameNS('*', 'userTask'))
    taskOptions.value = tasks
      .map((el) => ({
        id: el.getAttribute('id') || '',
        name: el.getAttribute('name') || el.getAttribute('id') || ''
      }))
      .filter((o) => o.id)
  } catch (e) {
    ElMessage.warning(t('workflow.formDesigner.parseUserTaskFailed', { msg: (e as Error).message }))
    taskOptions.value = []
  } finally {
    taskOptionsLoading.value = false
  }
}

async function loadSchemaForCurrent() {
  if (!processDefinitionKey.value) return
  try {
    const res = await getActiveFormSchema(processDefinitionKey.value, activityId.value)
    const data = res.data
    if (data) {
      formName.value = data.name || formName.value
      try {
        schemaRef.value = JSON.parse(data.schemaJson)
      } catch {
        ElMessage.warning(t('workflow.formDesigner.schemaInvalidJson'))
        schemaRef.value = []
      }
    } else {
      schemaRef.value = []
    }
    designerRef.value?.setRule?.(schemaRef.value)
  } catch (e) {
    ElMessage.error(t('workflow.formDesigner.schemaLoadFailed', { msg: (e as Error).message }))
  }
}

async function handleSave() {
  if (!processDefinitionKey.value) {
    ElMessage.error(t('workflow.formDesigner.schemaPickRequired'))
    return
  }
  const rule = designerRef.value?.getRule?.() ?? schemaRef.value ?? []
  saving.value = true
  try {
    await saveFormSchema({
      processDefinitionKey: processDefinitionKey.value,
      activityId: activityId.value,
      name: formName.value,
      schemaJson: JSON.stringify(rule)
    })
    ElMessage.success(t('workflow.formDesigner.savedAsNewVersion'))
  } finally {
    saving.value = false
  }
}

function handlePreview() {
  const rule = designerRef.value?.getRule?.() ?? []
  ElMessage.info(t('workflow.formDesigner.previewCount', { n: rule.length }))
  // eslint-disable-next-line no-console
  console.log('[FormDesigner] preview rule', rule)
}

watch([processDefinitionKey, activityId], () => {
  if (processDefinitionKey.value) void loadSchemaForCurrent()
})

watch(processDefinitionKey, () => {
  taskOptions.value = []
  if (processDefinitionKey.value) void loadTaskOptions()
})

onMounted(async () => {
  await fetchDefinitions()
  if (processDefinitionKey.value) {
    await loadSchemaForCurrent()
    void loadTaskOptions()
  }
})
</script>

<template>
  <div class="scaffold-page form-designer-page">
    <div class="designer-hero">
      <div>
        <p class="eyebrow">
          Workflow Form
        </p>
        <h2>表单设计</h2>
        <p class="hero-desc">
          先选择流程节点，再把左侧常用字段拖到中间画布。高级属性默认收起，避免干扰。
        </p>
      </div>
      <div class="hero-actions">
        <span class="mode-label">{{ advancedMode ? '高级模式' : '简洁模式' }}</span>
        <el-switch
          v-model="advancedMode"
          active-text="高级"
          inactive-text="简洁"
        />
      </div>
    </div>

    <div class="setup-card">
      <div class="step-card active">
        <span>1</span>
        <strong>选流程</strong>
        <small>确定这张表单挂在哪个流程上</small>
      </div>
      <div class="step-card">
        <span>2</span>
        <strong>选节点</strong>
        <small>启动表单或某个审批节点</small>
      </div>
      <div class="step-card">
        <span>3</span>
        <strong>保存版本</strong>
        <small>保存后立即给流程使用</small>
      </div>
      <el-form class="setup-form">
        <el-form-item :label="t('workflow.common.process')">
          <el-select
            v-model="processDefinitionKey"
            :placeholder="t('workflow.formDesigner.processPick')"
            filterable
          >
            <el-option
              v-for="d in definitions"
              :key="d.key"
              :label="`${d.name || d.key} (v${d.version})`"
              :value="d.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('workflow.common.activityNode')">
          <el-select
            v-model="activityId"
            filterable
            allow-create
            default-first-option
            :placeholder="t('workflow.formDesigner.activityIdHint')"
            :loading="taskOptionsLoading"
          >
            <el-option
              :label="`${ACTIVITY_START_FORM}${t('workflow.formDesigner.startFormSuffix')}`"
              :value="ACTIVITY_START_FORM"
            />
            <el-option
              v-for="task in taskOptions"
              :key="task.id"
              :label="`${task.id}${task.name && task.name !== task.id ? ' · ' + task.name : ''}`"
              :value="task.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.name')">
          <el-input
            v-model="formName"
            :placeholder="t('workflow.formDesigner.formNameLabel')"
          />
        </el-form-item>
        <el-form-item class="form-actions">
          <el-button
            type="primary"
            size="large"
            :loading="saving"
            @click="handleSave"
          >
            {{ t('workflow.formDesigner.saveBtn') }}
          </el-button>
          <el-button
            size="large"
            @click="handlePreview"
          >
            {{ t('workflow.formDesigner.previewBtn') }}
          </el-button>
          <el-button @click="loadSchemaForCurrent">
            {{ t('workflow.formDesigner.reloadBtn') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div
      v-if="designerVisible"
      class="designer-wrap"
      :class="{ 'is-simple': !advancedMode }"
    >
      <div
        v-if="!advancedMode"
        class="designer-tip"
      >
        <strong>常用控件已优先展示：</strong>
        输入框、多行输入、单选、多选、选择器、日期、上传。需要字段校验、脚本、布局等高级能力时再打开高级模式。
      </div>
      <FcDesigner ref="designerRef" />
    </div>
    <el-empty
      v-else
      :description="t('workflow.formDesigner.pickFirst')"
    />
  </div>
</template>

<style scoped lang="scss">
.form-designer-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: calc(100vh - 84px);
  background:
    radial-gradient(circle at 12% 0%, rgba(64, 158, 255, 0.12), transparent 28%),
    linear-gradient(180deg, #f6f8fc 0%, #eef3f9 100%);
  padding: 14px;

  .designer-hero,
  .setup-card,
  .designer-wrap {
    border: 1px solid rgba(32, 80, 129, 0.08);
    box-shadow: 0 14px 36px rgba(28, 48, 84, 0.08);
  }

  .designer-hero {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 20px 22px;
    color: #172033;
    background: linear-gradient(135deg, #ffffff 0%, #edf6ff 100%);
    border-radius: 18px;
  }

  .eyebrow {
    margin: 0 0 6px;
    color: #3a77d8;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
  }

  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 800;
  }

  .hero-desc {
    margin: 8px 0 0;
    color: #64748b;
    font-size: 14px;
  }

  .hero-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    background: rgba(255, 255, 255, 0.72);
    border: 1px solid rgba(58, 119, 216, 0.16);
    border-radius: 999px;
    white-space: nowrap;
  }

  .mode-label {
    color: #334155;
    font-size: 13px;
    font-weight: 700;
  }

  .setup-card {
    display: grid;
    grid-template-columns: repeat(3, minmax(120px, 170px)) minmax(420px, 1fr);
    gap: 12px;
    padding: 14px;
    background: rgba(255, 255, 255, 0.92);
    border-radius: 18px;
  }

  .step-card {
    display: grid;
    grid-template-columns: 34px 1fr;
    grid-template-rows: auto auto;
    column-gap: 10px;
    align-items: center;
    padding: 12px;
    background: #f8fafc;
    border: 1px solid #e6edf5;
    border-radius: 14px;

    span {
      grid-row: 1 / 3;
      display: inline-grid;
      width: 34px;
      height: 34px;
      color: #2f6fce;
      font-weight: 800;
      background: #e9f2ff;
      border-radius: 12px;
      place-items: center;
    }

    strong {
      color: #172033;
      font-size: 14px;
    }

    small {
      color: #738195;
      font-size: 12px;
    }
  }

  .step-card.active {
    background: #eff6ff;
    border-color: #bfdbfe;
  }

  .setup-form {
    display: grid;
    grid-template-columns: minmax(180px, 1fr) minmax(220px, 1fr) minmax(160px, 0.8fr) auto;
    gap: 12px;
    align-items: end;

    :deep(.el-form-item) {
      margin: 0;
    }

    :deep(.el-select),
    :deep(.el-input) {
      width: 100%;
    }
  }

  .form-actions {
    white-space: nowrap;
  }

  .designer-wrap {
    overflow: hidden;
    flex: 1 1 auto;
    height: calc(100vh - 330px);
    min-height: 520px;
    background: #fff;
    border-radius: 18px;
  }

  .designer-tip {
    position: relative;
    z-index: 2;
    padding: 10px 16px;
    color: #42526b;
    font-size: 13px;
    background: #f8fbff;
    border-bottom: 1px solid #e6edf5;
  }

  .designer-wrap.is-simple {
    :deep(._fc-designer) {
      height: calc(100% - 40px);
      border: none;
    }

    :deep(._fc-l-menu),
    :deep(._fc-r),
    :deep(._fc-m-tools ._fc-m-tools-l),
    :deep(._fc-m-tools .icon-preview) {
      display: none !important;
    }

    :deep(._fc-l) {
      width: 220px !important;
      border-right: 1px solid #eef2f7;
      box-shadow: 8px 0 24px rgba(15, 23, 42, 0.04);
    }

    :deep(._fc-l-tabs) {
      height: 44px;
      padding: 0 14px;
      font-weight: 700;
      background: #fbfdff;
    }

    :deep(._fc-l-tab:not(.active)) {
      display: none;
    }

    :deep(._fc-l-group:nth-of-type(n + 2)) {
      display: none;
    }

    :deep(._fc-l-title) {
      padding: 14px 14px 8px;
      color: #1f2a44;
      font-size: 13px;
    }

    :deep(._fc-l-list) {
      display: grid;
      grid-template-columns: 1fr;
      gap: 8px;
      padding: 0 12px 14px;
    }

    :deep(._fc-l-item) {
      display: flex;
      align-items: center;
      min-height: 38px;
      padding: 8px 10px;
      background: #f8fafc;
      border: 1px solid #e6edf5;
      border-radius: 12px;
    }

    :deep(._fc-l-item:nth-child(n + 14)) {
      display: none;
    }

    :deep(._fc-m) {
      min-width: 0;
    }

    :deep(._fc-m-tools) {
      height: 44px;
      min-height: 44px;
      padding: 0 16px;
      background: #ffffff;
      border-bottom: 1px solid #eef2f7;
    }

    :deep(._fc-m-tools-r::before) {
      margin-right: 10px;
      color: #64748b;
      font-size: 13px;
      content: "拖字段到下方空白区域";
    }

    :deep(._fc-m-drag) {
      background:
        linear-gradient(#f1f5f9 1px, transparent 1px),
        linear-gradient(90deg, #f1f5f9 1px, transparent 1px);
      background-color: #ffffff;
      background-size: 48px 48px;
    }
  }

  @media (max-width: 1280px) {
    .setup-card {
      grid-template-columns: repeat(3, 1fr);
    }

    .setup-form {
      grid-column: 1 / -1;
    }
  }

  @media (max-width: 900px) {
    .designer-hero,
    .setup-card,
    .setup-form {
      grid-template-columns: 1fr;
    }

    .designer-hero {
      align-items: flex-start;
      flex-direction: column;
    }
  }
}
</style>
