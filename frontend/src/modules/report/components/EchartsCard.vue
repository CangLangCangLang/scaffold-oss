<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { RunResult } from '../api'

/**
 * ECharts 卡片：把 RunResult 按 chartType + configJson 渲染为图。
 *
 * 仅在本组件挂载时才动态 import 'echarts'，使 ECharts 单独成 chunk，主 bundle 不会被拖累。
 */

interface Props {
  result: RunResult | null
  chartType: 'line' | 'bar' | 'pie' | 'number' | 'table'
  configJson?: string
  title?: string
}
const props = defineProps<Props>()

const containerRef = ref<HTMLDivElement | null>(null)
let chart: { setOption: (o: object) => void; resize: () => void; dispose: () => void } | null = null

async function ensureChart(): Promise<void> {
  if (chart || !containerRef.value) return
  const echarts = await import('echarts')
  chart = echarts.init(containerRef.value)
  window.addEventListener('resize', onResize)
}

function onResize(): void {
  chart?.resize()
}

function buildOption(): object | null {
  const r = props.result
  if (!r || !r.columns?.length || !r.rows?.length) return null

  let cfg: Record<string, unknown> = {}
  if (props.configJson) {
    try {
      cfg = JSON.parse(props.configJson)
    } catch {
      cfg = {}
    }
  }
  const xField = (cfg.x as string) || r.columns[0]
  const yField = (cfg.y as string) || r.columns[1] || r.columns[0]
  const xIdx = r.columns.indexOf(xField)
  const yIdx = r.columns.indexOf(yField)
  if (xIdx < 0 || yIdx < 0) return null

  const labels = r.rows.map((row) => String(row[xIdx]))
  const values = r.rows.map((row) => Number(row[yIdx]) || 0)

  if (props.chartType === 'pie') {
    return {
      title: props.title ? { text: props.title } : undefined,
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: '60%',
          data: r.rows.map((row) => ({
            name: String(row[xIdx]),
            value: Number(row[yIdx]) || 0
          }))
        }
      ]
    }
  }
  return {
    title: props.title ? { text: props.title } : undefined,
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value' },
    series: [{ type: props.chartType, data: values, smooth: props.chartType === 'line' }]
  }
}

async function render(): Promise<void> {
  await ensureChart()
  const opt = buildOption()
  if (chart && opt) {
    chart.setOption(opt)
    chart.resize()
  }
}

watch(() => [props.result, props.chartType, props.configJson], render, { deep: true })

onMounted(render)
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div
    ref="containerRef"
    class="echarts-card"
  />
</template>

<style scoped>
.echarts-card {
  width: 100%;
  height: 320px;
}
</style>
