<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

interface BpmnHighlights {
  /** 当前正在执行的节点 id（来自运行时 active activity） */
  active?: string[]
  /** 历史已完成节点 id */
  completed?: string[]
  /** 被退回 / 拒绝过的节点 id */
  rejected?: string[]
}

const props = withDefaults(
  defineProps<{
    modelValue?: string
    /** 是否只读：true 时改用 NavigatedViewer，仅平移/缩放，不能编辑 */
    readonly?: boolean
    /** 运行时态高亮：把节点 id 分到三类，分别给 CSS marker */
    highlights?: BpmnHighlights
  }>(),
  { modelValue: '', readonly: false, highlights: () => ({}) }
)
const emit = defineEmits<{
  (e: 'update:modelValue', xml: string): void
  (e: 'save', xml: string): void
}>()

const canvasRef = ref<HTMLDivElement>()
let viewer: any = null
let isReadonly = false

const DEFAULT_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  id="Definitions_1" targetNamespace="http://example.com/scaffold">
  <bpmn:process id="Process_demo" name="示例流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始"/>
    <bpmn:task id="Task_1" name="审批"/>
    <bpmn:endEvent id="EndEvent_1" name="结束"/>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="EndEvent_1"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_demo">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="160" y="120" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Task_1_di" bpmnElement="Task_1">
        <dc:Bounds x="260" y="98" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="420" y="120" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="196" y="138"/>
        <di:waypoint x="260" y="138"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="360" y="138"/>
        <di:waypoint x="420" y="138"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

const ALL_MARKERS = ['scaffold-active', 'scaffold-completed', 'scaffold-rejected']

async function loadXml(xml: string) {
  if (!viewer) return
  try {
    await viewer.importXML(xml || DEFAULT_BPMN)
    viewer.get('canvas').zoom('fit-viewport')
    applyHighlights(props.highlights)
  } catch (err) {
    console.error('BPMN 解析失败', err)
    ElMessage.error('BPMN 解析失败：' + (err as Error).message)
  }
}

function clearHighlights() {
  if (!viewer) return
  const canvas = viewer.get('canvas')
  const registry = viewer.get('elementRegistry')
  registry.getAll().forEach((el: any) => {
    ALL_MARKERS.forEach((m) => canvas.removeMarker(el.id, m))
  })
}

function applyHighlights(h: BpmnHighlights | undefined) {
  if (!viewer || !h) return
  const canvas = viewer.get('canvas')
  clearHighlights()
  ;(h.completed || []).forEach((id) => canvas.addMarker(id, 'scaffold-completed'))
  ;(h.active || []).forEach((id) => canvas.addMarker(id, 'scaffold-active'))
  ;(h.rejected || []).forEach((id) => canvas.addMarker(id, 'scaffold-rejected'))
}

async function getXml(): Promise<string> {
  if (!viewer) return ''
  const { xml } = await viewer.saveXML({ format: true })
  return xml
}

/** 导出当前画布为 SVG 字符串。Modeler / NavigatedViewer 都带 saveSVG。 */
async function getSvg(): Promise<string> {
  if (!viewer) return ''
  const { svg } = await viewer.saveSVG()
  return svg
}

async function emitSave() {
  if (isReadonly) return
  const xml = await getXml()
  emit('update:modelValue', xml)
  emit('save', xml)
}

defineExpose({ getXml, getSvg, loadXml, applyHighlights })

onMounted(async () => {
  if (!canvasRef.value) return
  isReadonly = props.readonly === true
  viewer = isReadonly
    ? new BpmnViewer({ container: canvasRef.value })
    : new BpmnModeler({ container: canvasRef.value })
  await loadXml(props.modelValue || DEFAULT_BPMN)
  if (!isReadonly) {
    viewer.on('commandStack.changed', () => {
      void emitSave()
    })
  }
})

onBeforeUnmount(() => {
  if (viewer) {
    viewer.destroy()
    viewer = null
  }
})

watch(
  () => props.modelValue,
  async (val) => {
    if (viewer && val !== undefined) {
      const current = await getXml()
      if (current !== val) await loadXml(val)
    }
  }
)

watch(
  () => props.highlights,
  (val) => applyHighlights(val),
  { deep: true }
)
</script>

<template>
  <div class="bpmn-designer">
    <div
      ref="canvasRef"
      class="bpmn-canvas"
      :class="{ 'is-readonly': isReadonly }"
    />
  </div>
</template>

<style lang="scss">
/* 必须用全局选择器才能命中 bpmn-js 在 svg 内动态加上的 class */
.bpmn-canvas {
  .scaffold-completed:not(.djs-connection) .djs-visual > :first-child {
    fill: rgba(103, 194, 58, 0.2) !important;
    stroke: #67c23a !important;
  }
  .scaffold-completed.djs-connection .djs-visual > :first-child {
    stroke: #67c23a !important;
  }
  .scaffold-active:not(.djs-connection) .djs-visual > :first-child {
    fill: rgba(64, 158, 255, 0.25) !important;
    stroke: #409eff !important;
    stroke-width: 2.5 !important;
    animation: scaffold-pulse 1.6s ease-in-out infinite;
  }
  .scaffold-rejected:not(.djs-connection) .djs-visual > :first-child {
    fill: rgba(245, 108, 108, 0.18) !important;
    stroke: #f56c6c !important;
  }
}

@keyframes scaffold-pulse {
  0%,
  100% {
    stroke-opacity: 1;
  }
  50% {
    stroke-opacity: 0.45;
  }
}
</style>

<style scoped lang="scss">
.bpmn-designer {
  width: 100%;
  height: 100%;
  min-height: 480px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}

.bpmn-canvas {
  width: 100%;
  height: 100%;
  min-height: 480px;
}
</style>
