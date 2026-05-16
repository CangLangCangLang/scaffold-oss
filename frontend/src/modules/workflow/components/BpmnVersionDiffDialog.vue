<script setup lang="ts">
/**
 * BPMN 版本对比对话框：左右两块画布，顶部各一个版本下拉，下方 diff 统计 + 增/删/改名节点列表。
 *
 * 对比算法（足够轻，不依赖 jsondiffpatch）：
 *   1. 用 DOMParser 解析 XML；
 *   2. 把每个 BPMN 节点（`<bpmn:*>` 含 `id` 属性）抽成 `{ id, name, tag }` 三元组；
 *      其中 BPMNDiagram 段（visualization-only）排除——它们的 id 跟随业务节点变化，
 *      算"业务变更"会冗余、噪声大。
 *   3. 按 id 求集合差：左有右无 → removed；右有左无 → added；
 *   4. id 同 name 不同 → renamed。
 *
 * 设计取舍：
 * - 没有用现成 diff 算法（如 dmn-js / camunda-bpmn-diff）：
 *   它们体积大、强假设节点 id 稳定（不能发现 add/remove），且需要适配自己的 BPMN 数据模型。
 *   对于"流程节点改没改"这一目标，按 id 求集合已经够用。
 * - 算法运行在前端而不是后端：让后端只暴露 versions + xml 的纯读 API，对比逻辑前端自定义；
 *   切换 i18n 不用动后端。
 * - sequenceFlow 同样纳入对比：路径变化通常代表流程拓扑变化，是有效信息。
 * - i18n: workflow.process.diff.*（不为这个组件开新 namespace；diff 行为强耦合 process 页面）。
 */
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { listVersionsByKey, getBpmnXml, type ProcessDefinitionView } from '../api'
import BpmnDesigner from './BpmnDesigner.vue'

const { t } = useI18n()

const props = defineProps<{
  /** 流程定义 key（用来拉版本列表） */
  processDefinitionKey: string
  /** 流程显示名（标题用，可选）*/
  processName?: string
}>()

const visible = defineModel<boolean>('modelValue', { default: false })

const loading = ref(false)
const versions = ref<ProcessDefinitionView[]>([])
const leftId = ref<string | undefined>()
const rightId = ref<string | undefined>()
const leftXml = ref<string>('')
const rightXml = ref<string>('')

interface BpmnNode {
  id: string
  name: string
  tag: string
}

function parseBpmnNodes(xml: string): BpmnNode[] {
  if (!xml) return []
  let dom: Document
  try {
    dom = new DOMParser().parseFromString(xml, 'text/xml')
  } catch {
    return []
  }
  // parseError 会以 <parsererror> 节点回写到 DOM
  if (dom.getElementsByTagName('parsererror').length > 0) return []
  const nodes: BpmnNode[] = []
  // 选所有 BPMN namespace 下的元素（命名空间 prefix 可能是 bpmn / bpmn2 / 不带 prefix），
  // 用 *|local 通配 namespace；同时排除 BPMNDiagram 子树（visualization-only）。
  const allWithId = Array.from(dom.querySelectorAll('[id]'))
  for (const el of allWithId) {
    const localName = el.localName
    if (!localName) continue
    if (isInDiagramSubtree(el)) continue
    // 业务节点白名单：process / 各种 event / task / gateway / sequenceFlow / lane / subProcess / dataObject 等
    if (!isBusinessNode(localName)) continue
    const id = el.getAttribute('id') || ''
    if (!id) continue
    nodes.push({
      id,
      name: el.getAttribute('name') || '',
      tag: localName
    })
  }
  return nodes
}

function isInDiagramSubtree(el: Element): boolean {
  // 一路向上看是否在 BPMNDiagram / BPMNPlane / BPMNShape / BPMNEdge / BPMNLabel 之下
  let cur: Element | null = el
  while (cur) {
    const ln = cur.localName
    if (ln === 'BPMNDiagram' || ln === 'BPMNPlane' || ln === 'BPMNShape' || ln === 'BPMNEdge' || ln === 'BPMNLabel') {
      return true
    }
    cur = cur.parentElement
  }
  return false
}

function isBusinessNode(localName: string): boolean {
  // 收紧白名单避免把 definitions / extensionElements 等顶层容器当作节点
  return (
    localName === 'process' ||
    localName === 'subProcess' ||
    localName === 'task' ||
    localName === 'userTask' ||
    localName === 'serviceTask' ||
    localName === 'scriptTask' ||
    localName === 'sendTask' ||
    localName === 'receiveTask' ||
    localName === 'manualTask' ||
    localName === 'businessRuleTask' ||
    localName === 'callActivity' ||
    localName === 'startEvent' ||
    localName === 'endEvent' ||
    localName === 'intermediateCatchEvent' ||
    localName === 'intermediateThrowEvent' ||
    localName === 'boundaryEvent' ||
    localName === 'exclusiveGateway' ||
    localName === 'parallelGateway' ||
    localName === 'inclusiveGateway' ||
    localName === 'eventBasedGateway' ||
    localName === 'complexGateway' ||
    localName === 'sequenceFlow' ||
    localName === 'lane' ||
    localName === 'laneSet' ||
    localName === 'dataObject' ||
    localName === 'dataObjectReference' ||
    localName === 'textAnnotation'
  )
}

interface DiffResult {
  added: BpmnNode[]
  removed: BpmnNode[]
  renamed: { id: string; tag: string; leftName: string; rightName: string }[]
  unchanged: number
}

const diff = computed<DiffResult>(() => {
  const leftNodes = parseBpmnNodes(leftXml.value)
  const rightNodes = parseBpmnNodes(rightXml.value)
  const leftMap = new Map<string, BpmnNode>(leftNodes.map((n) => [n.id, n]))
  const rightMap = new Map<string, BpmnNode>(rightNodes.map((n) => [n.id, n]))
  const added: BpmnNode[] = []
  const removed: BpmnNode[] = []
  const renamed: { id: string; tag: string; leftName: string; rightName: string }[] = []
  let unchanged = 0
  rightMap.forEach((rn, id) => {
    const ln = leftMap.get(id)
    if (!ln) {
      added.push(rn)
    } else if ((ln.name || '') !== (rn.name || '')) {
      renamed.push({ id, tag: rn.tag, leftName: ln.name, rightName: rn.name })
    } else {
      unchanged += 1
    }
  })
  leftMap.forEach((ln, id) => {
    if (!rightMap.has(id)) removed.push(ln)
  })
  return { added, removed, renamed, unchanged }
})

const hasAnyChange = computed(
  () => diff.value.added.length + diff.value.removed.length + diff.value.renamed.length > 0
)

const leftMeta = computed(() => versions.value.find((v) => v.id === leftId.value))
const rightMeta = computed(() => versions.value.find((v) => v.id === rightId.value))

async function fetchVersions(): Promise<void> {
  loading.value = true
  try {
    const res = await listVersionsByKey(props.processDefinitionKey)
    versions.value = res.data ?? []
    if (versions.value.length < 1) {
      ElMessage.warning(t('workflow.process.diffNoVersions'))
      visible.value = false
      return
    }
    if (versions.value.length === 1) {
      ElMessage.info(t('workflow.process.diffOnlyOne'))
    }
    // 默认：右侧 = 最新版（[0]，desc 排序），左侧 = 上一版（[1]）；只有 1 个版本就两侧一样
    rightId.value = versions.value[0]?.id
    leftId.value = versions.value[1]?.id ?? versions.value[0]?.id
    await Promise.all([loadXml('left'), loadXml('right')])
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.error(t('workflow.process.diffLoadFailed', { msg }))
    visible.value = false
  } finally {
    loading.value = false
  }
}

async function loadXml(side: 'left' | 'right'): Promise<void> {
  const id = side === 'left' ? leftId.value : rightId.value
  if (!id) {
    if (side === 'left') leftXml.value = ''
    else rightXml.value = ''
    return
  }
  try {
    const res = await getBpmnXml(id)
    const xml = res.data?.xml ?? ''
    if (side === 'left') leftXml.value = xml
    else rightXml.value = xml
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.error(t('workflow.process.diffXmlFailed', { msg }))
  }
}

watch(leftId, () => {
  if (leftId.value && rightId.value && leftId.value === rightId.value && versions.value.length > 1) {
    ElMessage.info(t('workflow.process.diffSameVersion'))
  }
  void loadXml('left')
})

watch(rightId, () => {
  if (leftId.value && rightId.value && leftId.value === rightId.value && versions.value.length > 1) {
    ElMessage.info(t('workflow.process.diffSameVersion'))
  }
  void loadXml('right')
})

watch(visible, (v) => {
  if (v) void fetchVersions()
})

function fmtVersionLabel(v: ProcessDefinitionView): string {
  const time = v.deploymentTime ?? ''
  const suspended = v.suspended ? ` · ${t('workflow.process.diffMetaSuspended')}` : ''
  return `v${v.version} · ${time}${suspended}`
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('workflow.process.diffTitle', { name: processName || processDefinitionKey })"
    width="92%"
    top="3vh"
    destroy-on-close
  >
    <div
      v-loading="loading"
      class="bpmn-diff-dialog"
    >
      <div class="diff-pickers">
        <div class="diff-picker">
          <span class="diff-picker-label">{{ t('workflow.process.diffLeftVersion') }}</span>
          <el-select
            v-model="leftId"
            :placeholder="t('workflow.process.diffPickPlaceholder')"
            style="width: 320px"
          >
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="fmtVersionLabel(v)"
              :value="v.id"
            />
          </el-select>
          <span
            v-if="leftMeta"
            class="diff-meta"
          >
            {{ t('workflow.process.diffMetaVersion') }}: v{{ leftMeta.version }}
            ·
            {{ t('workflow.process.diffMetaDeployTime') }}: {{ leftMeta.deploymentTime || '—' }}
            ·
            <el-tag
              size="small"
              :type="leftMeta.suspended ? 'info' : 'success'"
            >
              {{ leftMeta.suspended ? t('workflow.process.diffMetaSuspended') : t('workflow.process.diffMetaActive') }}
            </el-tag>
          </span>
        </div>
        <div class="diff-picker">
          <span class="diff-picker-label">{{ t('workflow.process.diffRightVersion') }}</span>
          <el-select
            v-model="rightId"
            :placeholder="t('workflow.process.diffPickPlaceholder')"
            style="width: 320px"
          >
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="fmtVersionLabel(v)"
              :value="v.id"
            />
          </el-select>
          <span
            v-if="rightMeta"
            class="diff-meta"
          >
            {{ t('workflow.process.diffMetaVersion') }}: v{{ rightMeta.version }}
            ·
            {{ t('workflow.process.diffMetaDeployTime') }}: {{ rightMeta.deploymentTime || '—' }}
            ·
            <el-tag
              size="small"
              :type="rightMeta.suspended ? 'info' : 'success'"
            >
              {{ rightMeta.suspended ? t('workflow.process.diffMetaSuspended') : t('workflow.process.diffMetaActive') }}
            </el-tag>
          </span>
        </div>
      </div>

      <div class="diff-canvas-grid">
        <div class="diff-canvas-col">
          <BpmnDesigner
            v-if="visible"
            :model-value="leftXml"
            readonly
          />
        </div>
        <div class="diff-canvas-col">
          <BpmnDesigner
            v-if="visible"
            :model-value="rightXml"
            readonly
          />
        </div>
      </div>

      <div class="diff-summary">
        <el-tag
          v-if="!hasAnyChange"
          type="success"
          effect="plain"
          size="large"
        >
          {{ t('workflow.process.diffStatNoChange') }}
        </el-tag>
        <el-tag
          v-else
          type="warning"
          effect="plain"
          size="large"
        >
          {{ t('workflow.process.diffStatChanged', {
            added: diff.added.length,
            removed: diff.removed.length,
            renamed: diff.renamed.length
          }) }}
        </el-tag>
      </div>

      <div
        v-if="hasAnyChange"
        class="diff-sections"
      >
        <div class="diff-section diff-section-added">
          <h4>{{ t('workflow.process.diffNodeAdded') }} ({{ diff.added.length }})</h4>
          <div
            v-if="diff.added.length === 0"
            class="diff-empty"
          >
            {{ t('workflow.process.diffEmptySection') }}
          </div>
          <ul v-else>
            <li
              v-for="n in diff.added"
              :key="`add-${n.id}`"
            >
              <code class="diff-tag">{{ n.tag }}</code>
              <span class="diff-id">{{ n.id }}</span>
              <span class="diff-name">{{ n.name || '—' }}</span>
            </li>
          </ul>
        </div>
        <div class="diff-section diff-section-removed">
          <h4>{{ t('workflow.process.diffNodeRemoved') }} ({{ diff.removed.length }})</h4>
          <div
            v-if="diff.removed.length === 0"
            class="diff-empty"
          >
            {{ t('workflow.process.diffEmptySection') }}
          </div>
          <ul v-else>
            <li
              v-for="n in diff.removed"
              :key="`rm-${n.id}`"
            >
              <code class="diff-tag">{{ n.tag }}</code>
              <span class="diff-id">{{ n.id }}</span>
              <span class="diff-name">{{ n.name || '—' }}</span>
            </li>
          </ul>
        </div>
        <div class="diff-section diff-section-renamed">
          <h4>{{ t('workflow.process.diffNodeRenamed') }} ({{ diff.renamed.length }})</h4>
          <div
            v-if="diff.renamed.length === 0"
            class="diff-empty"
          >
            {{ t('workflow.process.diffEmptySection') }}
          </div>
          <ul v-else>
            <li
              v-for="r in diff.renamed"
              :key="`rn-${r.id}`"
            >
              <code class="diff-tag">{{ r.tag }}</code>
              <span class="diff-id">{{ r.id }}</span>
              <span class="diff-rename">
                <span class="diff-rename-old">{{ r.leftName || '—' }}</span>
                <span class="diff-rename-arrow">→</span>
                <span class="diff-rename-new">{{ r.rightName || '—' }}</span>
              </span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.bpmn-diff-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.diff-pickers {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.diff-picker {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.diff-picker-label {
  font-weight: 500;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.diff-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.diff-canvas-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  height: 60vh;
  min-height: 360px;
}

.diff-canvas-col {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.diff-summary {
  display: flex;
  justify-content: center;
}

.diff-sections {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.diff-section {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 10px 12px;

  h4 {
    margin: 0 0 8px;
    font-size: 13px;
    font-weight: 600;
  }

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
    max-height: 200px;
    overflow: auto;
  }

  li {
    font-size: 12px;
    line-height: 1.7;
    border-bottom: 1px dashed var(--el-border-color-lighter);
    padding: 4px 0;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  li:last-child {
    border-bottom: none;
  }
}

.diff-section-added h4 {
  color: var(--el-color-success);
}

.diff-section-removed h4 {
  color: var(--el-color-danger);
}

.diff-section-renamed h4 {
  color: var(--el-color-warning);
}

.diff-tag {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  background: var(--el-fill-color-light);
  padding: 1px 6px;
  border-radius: 3px;
  color: var(--el-text-color-regular);
}

.diff-id {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.diff-name {
  color: var(--el-text-color-primary);
}

.diff-rename {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.diff-rename-old {
  text-decoration: line-through;
  color: var(--el-color-danger);
}

.diff-rename-new {
  color: var(--el-color-success);
}

.diff-rename-arrow {
  color: var(--el-text-color-secondary);
}

.diff-empty {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
