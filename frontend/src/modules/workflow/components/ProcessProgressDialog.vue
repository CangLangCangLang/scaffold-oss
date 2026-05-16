<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import {
  cancelAddSignBeforeTask,
  getInstanceTimeline,
  getInstanceXml,
  type ProcessRuntimeStateView,
  type TimelineEntry,
  type TimelineEntryCode
} from '../api'
import BpmnDesigner from './BpmnDesigner.vue'

const { t } = useI18n()

const props = defineProps<{
  modelValue: boolean
  processInstanceId?: string
  /** 显示在标题上的辅助信息（流程名 / 业务 key 等） */
  title?: string
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const activeTab = ref<'graph' | 'timeline'>('graph')

const xml = ref('')
const state = ref<ProcessRuntimeStateView | null>(null)
const loadingGraph = ref(false)

const timeline = ref<TimelineEntry[]>([])
const loadingTimeline = ref(false)

interface BpmnDesignerExpose {
  getSvg: () => Promise<string>
}
const designerRef = ref<BpmnDesignerExpose | null>(null)

const highlights = computed(() => ({
  active: state.value?.activeActivityIds ?? [],
  completed: state.value?.completedActivityIds ?? [],
  rejected: state.value?.rejectedActivityIds ?? []
}))

const TIMELINE_STYLE: Record<
  TimelineEntryCode,
  { color: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }
> = {
  'process.start': { color: '#909399', type: 'info' },
  'process.end': { color: '#909399', type: 'info' },
  'activity.start': { color: '#409eff', type: 'primary' },
  'activity.end': { color: '#67c23a', type: 'success' },
  'task.complete': { color: '#67c23a', type: 'success' },
  'task.cc': { color: '#909399', type: 'info' },
  'task.addsign.after': { color: '#e6a23c', type: 'warning' },
  'task.addsign.before': { color: '#e6a23c', type: 'warning' },
  'task.sendback': { color: '#f56c6c', type: 'danger' },
  'task.comment': { color: '#909399', type: 'info' }
}

function styleOf(entry: TimelineEntry) {
  const known = TIMELINE_STYLE[entry.code]
  if (known) {
    return {
      color: known.color,
      type: known.type,
      label: t(`workflow.progress.timeline.${entry.code}`)
    }
  }
  return { color: '#909399', type: 'info' as const, label: entry.code }
}

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles.includes('admin'))
const currentUserId = computed(() => (userStore.userId == null ? '' : String(userStore.userId)))

interface PreSignExtra {
  childTaskId?: string
  cancelled?: boolean
  cancelledBy?: string
  addedAssignee?: string
}

function presignExtra(entry: TimelineEntry): PreSignExtra {
  return (entry.extra ?? {}) as PreSignExtra
}

/** 仅当：是 task.addsign.before 条目 + 子任务 id 存在 + 未撤销 + 当前用户=发起人或 admin 才显示"撤销"按钮 */
function canCancelPreSign(entry: TimelineEntry): boolean {
  if (entry.code !== 'task.addsign.before') return false
  const ext = presignExtra(entry)
  if (!ext.childTaskId) return false
  if (ext.cancelled) return false
  if (isAdmin.value) return true
  return entry.actor != null && entry.actor === currentUserId.value
}

/** 导出当前流程图为 SVG 文件。利用 bpmn-js Viewer 自带的 saveSVG。 */
async function exportSvg() {
  try {
    if (!designerRef.value) {
      ElMessage.error(t('workflow.progress.designerNotReady'))
      return
    }
    const svg = await designerRef.value.getSvg()
    if (!svg) {
      ElMessage.error(t('workflow.progress.svgEmpty'))
      return
    }
    const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const baseName = props.title ? props.title.replace(/[\\/:*?"<>|]/g, '_') : 'process'
    a.href = url
    a.download = `${baseName}.svg`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(t('workflow.progress.exportSvgFailed', { msg: (e as Error).message }))
  }
}

/**
 * 用浏览器原生打印窗口"打印"时间轴。打印样式由 @media print 控制：隐藏对话框框架与按钮，
 * 只把 .timeline-wrap 里的内容铺平。用户在打印对话框里选 "Save as PDF" 即可生成 PDF。
 */
function printTimeline() {
  if (timeline.value.length === 0) {
    ElMessage.warning(t('workflow.progress.emptyTimeline'))
    return
  }
  document.body.classList.add('wf-printing')
  requestAnimationFrame(() => {
    window.print()
    setTimeout(() => document.body.classList.remove('wf-printing'), 300)
  })
}

async function handleCancelPreSign(entry: TimelineEntry) {
  const ext = presignExtra(entry)
  if (!ext.childTaskId) return
  try {
    await ElMessageBox.confirm(
      t('workflow.progress.presignCancelPrompt', { name: ext.addedAssignee ?? '?' }),
      t('workflow.progress.presignCancelTitle'),
      {
        type: 'warning',
        confirmButtonText: t('workflow.progress.presignCancelBtn'),
        cancelButtonText: t('common.cancel')
      }
    )
  } catch {
    return
  }
  try {
    await cancelAddSignBeforeTask(ext.childTaskId)
    ElMessage.success(t('workflow.progress.presignCancelOk'))
    await loadTimeline()
  } catch (e) {
    ElMessage.error(t('workflow.progress.presignCancelFailed', { msg: (e as Error).message }))
  }
}

async function loadGraph() {
  if (!props.processInstanceId) return
  loadingGraph.value = true
  try {
    const res = await getInstanceXml(props.processInstanceId)
    xml.value = res.data?.xml ?? ''
    state.value = res.data?.state ?? null
  } catch (e) {
    ElMessage.error(t('workflow.progress.loadGraphFailed', { msg: (e as Error).message }))
  } finally {
    loadingGraph.value = false
  }
}

async function loadTimeline() {
  if (!props.processInstanceId) return
  loadingTimeline.value = true
  try {
    const res = await getInstanceTimeline(props.processInstanceId)
    timeline.value = res.data ?? []
  } catch (e) {
    ElMessage.error(t('workflow.progress.loadTimelineFailed', { msg: (e as Error).message }))
    timeline.value = []
  } finally {
    loadingTimeline.value = false
  }
}

function onTabChange(name: string | number) {
  if (name === 'timeline' && timeline.value.length === 0 && !loadingTimeline.value) {
    void loadTimeline()
  }
}

watch(
  () => [props.modelValue, props.processInstanceId],
  ([show]) => {
    if (show) {
      activeTab.value = 'graph'
      xml.value = ''
      state.value = null
      timeline.value = []
      void loadGraph()
    }
  },
  { immediate: true }
)
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title ? t('workflow.progress.dialogTitleFull', { title }) : t('workflow.progress.dialogTitle')"
    width="1080px"
    top="6vh"
    destroy-on-close
  >
    <el-tabs
      v-model="activeTab"
      class="progress-tabs"
      @tab-change="onTabChange"
    >
      <el-tab-pane
        :label="t('workflow.common.flowChart')"
        name="graph"
      >
        <div
          v-loading="loadingGraph"
          class="progress-meta"
        >
          <el-tag
            v-if="state?.ended"
            type="info"
            effect="plain"
          >
            {{ t('workflow.progress.endedAt', { time: state.endTime }) }}
          </el-tag>
          <el-tag
            v-else
            type="primary"
            effect="plain"
          >
            {{ t('workflow.progress.activeAt', { ids: (state?.activeActivityIds ?? []).join(', ') || '-' }) }}
          </el-tag>
          <el-tag
            v-if="(state?.rejectedActivityIds ?? []).length"
            type="warning"
            effect="plain"
          >
            {{ t('workflow.progress.rejectedAt', { ids: state?.rejectedActivityIds?.join(', ') }) }}
          </el-tag>

          <span class="legend">
            <i class="dot dot-completed" /> {{ t('workflow.progress.legendCompleted') }}
            <i class="dot dot-active" /> {{ t('workflow.progress.legendActive') }}
            <i class="dot dot-rejected" /> {{ t('workflow.progress.legendRejected') }}
          </span>

          <el-button
            v-if="xml"
            size="small"
            type="primary"
            link
            class="export-btn"
            @click="exportSvg"
          >
            {{ t('workflow.progress.exportSvg') }}
          </el-button>
        </div>

        <div class="canvas-wrap">
          <BpmnDesigner
            v-if="xml"
            ref="designerRef"
            :model-value="xml"
            readonly
            :highlights="highlights"
          />
          <el-empty
            v-else-if="!loadingGraph"
            :description="t('workflow.progress.notLoaded')"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane
        :label="t('workflow.common.timeline')"
        name="timeline"
      >
        <div class="timeline-toolbar wf-no-print">
          <el-button
            size="small"
            type="primary"
            link
            :disabled="timeline.length === 0"
            @click="printTimeline"
          >
            {{ t('workflow.progress.printPdf') }}
          </el-button>
          <span class="timeline-tip">
            {{ t('workflow.progress.printTip') }}
          </span>
        </div>
        <div
          v-loading="loadingTimeline"
          class="timeline-wrap wf-printable"
        >
          <el-empty
            v-if="!loadingTimeline && timeline.length === 0"
            :description="t('workflow.progress.emptyTimeline')"
          />
          <el-timeline v-else>
            <el-timeline-item
              v-for="(item, idx) in timeline"
              :key="idx"
              :timestamp="item.occurredAt"
              :color="styleOf(item).color"
              placement="top"
            >
              <div class="t-row">
                <el-tag
                  size="small"
                  :type="styleOf(item).type"
                  effect="light"
                >
                  {{ styleOf(item).label }}
                </el-tag>
                <span
                  v-if="item.actor"
                  class="t-actor"
                >
                  · {{ item.actor }}
                </span>
                <span
                  v-if="item.activityId"
                  class="t-activity"
                >
                  · {{ item.activityId }}
                </span>
                <el-tag
                  v-if="item.code === 'task.addsign.before' && presignExtra(item).cancelled"
                  size="small"
                  type="info"
                  effect="dark"
                  class="t-cancelled"
                >
                  {{ t('workflow.progress.presignCancelled') }}
                </el-tag>
                <el-button
                  v-if="canCancelPreSign(item)"
                  link
                  type="danger"
                  size="small"
                  class="t-action"
                  @click="handleCancelPreSign(item)"
                >
                  {{ t('workflow.progress.presignCancelBtn') }}
                </el-button>
              </div>
              <div class="t-message">
                {{ item.message }}
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<style scoped lang="scss">
.progress-tabs {
  --el-tabs-header-height: 36px;
}

.progress-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;

  .legend {
    margin-left: auto;
    color: var(--el-text-color-secondary);
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
  }

  .dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 2px;
    margin-right: 4px;
    margin-left: 4px;
    vertical-align: middle;
  }
  .dot-completed { background: #67c23a; }
  .dot-active    { background: #409eff; }
  .dot-rejected  { background: #f56c6c; }
}

.canvas-wrap {
  height: 580px;
}

.timeline-wrap {
  max-height: 600px;
  overflow-y: auto;
  padding: 8px 12px 8px 0;

  .t-row {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    flex-wrap: wrap;
  }

  .t-actor {
    color: var(--el-color-primary);
    font-weight: 500;
  }

  .t-activity {
    color: var(--el-text-color-secondary);
    font-family: var(--el-font-family-monospace, Consolas, Menlo, monospace);
    font-size: 12px;
  }

  .t-message {
    margin-top: 4px;
    color: var(--el-text-color-regular);
    line-height: 1.5;
    font-size: 13px;
  }

  .t-action {
    margin-left: auto;
  }
  .t-cancelled {
    margin-left: 4px;
  }
}

.timeline-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;

  .timeline-tip {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.export-btn {
  margin-left: 12px;
}
</style>

<style lang="scss">
/**
 * 全局打印样式（不能用 scoped——el-dialog 是 teleport 到 body 的）。
 * body.wf-printing 在 printTimeline() 临时设上，让 .wf-no-print 隐藏、
 * .wf-printable 单独占满 A4 页面。
 */
@media print {
  body.wf-printing {
    background: #fff !important;
  }

  body.wf-printing .wf-no-print {
    display: none !important;
  }

  body.wf-printing .wf-printable {
    max-height: none !important;
    overflow: visible !important;
    padding: 0 !important;
  }

  body.wf-printing .el-dialog,
  body.wf-printing .el-overlay,
  body.wf-printing .el-dialog__wrapper {
    position: static !important;
    box-shadow: none !important;
    background: transparent !important;
    width: 100% !important;
  }

  body.wf-printing .el-dialog__header,
  body.wf-printing .el-dialog__footer,
  body.wf-printing .el-tabs__header {
    display: none !important;
  }

  body.wf-printing .el-tab-pane {
    display: block !important;
  }
  body.wf-printing .canvas-wrap,
  body.wf-printing .progress-meta {
    display: none !important;
  }
}
</style>
