<script setup lang="ts">
/**
 * Q-3 业务指标实时采样（菜单 9003）。
 *
 * - 顶部：已注册的业务表清单（Gauge：scaffold.business.rows）
 * - 中部：从 /actuator/prometheus 抓 scaffold_business_rows 指标，实时显示数值
 * - 下部：JVM / HTTP P95 / DataSource 关键指标摘要（同样来自 prometheus 文本）
 *
 * 不引第三方 prom 解析器：自己写一个极简正则（line based）。
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listBusinessMetrics, fetchActuator } from '@/api/monitor/observability'

interface MetricRow {
  table: string
  rows: number
}

interface JvmRow {
  metric: string
  value: number
  unit?: string
}

const trackedTables = ref<string[]>([])
const tableRows = ref<MetricRow[]>([])
const jvmStats = ref<JvmRow[]>([])
const httpStats = ref<{ uri: string; count: number; max: number; sum: number }[]>([])
const loading = ref(false)
const lastFetched = ref<string>('-')
let timer: ReturnType<typeof setInterval> | null = null

async function fetchOnce() {
  loading.value = true
  try {
    const meta = await listBusinessMetrics()
    trackedTables.value = (meta.data?.tables ?? []) as string[]

    const text = await fetchActuator('prometheus') as unknown as string
    parsePrometheus(text)
    lastFetched.value = new Date().toLocaleTimeString()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.warning(`抓取 /actuator/prometheus 失败：${msg}`)
  } finally {
    loading.value = false
  }
}

function parsePrometheus(text: string) {
  if (typeof text !== 'string' || !text) return
  const lines = text.split(/\r?\n/)

  // 业务行数：scaffold_business_rows{...table="cms_article",...} 1234.0
  const tableValues = new Map<string, number>()
  // JVM 关键
  const jvmKeys = new Map<string, { value: number; unit?: string }>()
  // HTTP — 按 uri 聚合 count / max / sum_seconds
  const httpAgg = new Map<string, { count: number; max: number; sum: number }>()

  for (const line of lines) {
    if (!line || line.startsWith('#')) continue

    // scaffold_business_rows{...table="..."} value
    const bizMatch = line.match(/^scaffold_business_rows\{[^}]*table="([^"]+)"[^}]*\}\s+([\d.eE+-]+)/)
    if (bizMatch) {
      tableValues.set(bizMatch[1], Number(bizMatch[2]))
      continue
    }

    // jvm_memory_used_bytes{area="heap",...} value  → 单一聚合
    if (line.startsWith('jvm_memory_used_bytes{') && line.includes('area="heap"')) {
      const v = Number((line.split(/\s+/).pop() ?? 0))
      const cur = jvmKeys.get('heap.used') ?? { value: 0, unit: 'bytes' }
      jvmKeys.set('heap.used', { value: cur.value + v, unit: 'bytes' })
      continue
    }
    if (line.startsWith('jvm_threads_live_threads ')) {
      jvmKeys.set('threads.live', { value: Number(line.split(/\s+/).pop()), unit: 'count' })
      continue
    }
    if (line.startsWith('process_uptime_seconds ')) {
      jvmKeys.set('process.uptime', { value: Number(line.split(/\s+/).pop()), unit: 'seconds' })
      continue
    }

    // http.server.requests
    if (line.startsWith('http_server_requests_seconds_count{') ||
        line.startsWith('http_server_requests_seconds_max{') ||
        line.startsWith('http_server_requests_seconds_sum{')) {
      const uriMatch = line.match(/uri="([^"]+)"/)
      if (!uriMatch) continue
      const uri = uriMatch[1]
      const v = Number((line.split(/\s+/).pop() ?? 0))
      const cur = httpAgg.get(uri) ?? { count: 0, max: 0, sum: 0 }
      if (line.startsWith('http_server_requests_seconds_count{')) cur.count += v
      else if (line.startsWith('http_server_requests_seconds_max{')) cur.max = Math.max(cur.max, v)
      else cur.sum += v
      httpAgg.set(uri, cur)
    }
  }

  // 业务表对齐
  tableRows.value = trackedTables.value.map((t) => ({ table: t, rows: tableValues.get(t) ?? 0 }))

  // JVM 摘要
  jvmStats.value = []
  jvmKeys.forEach((info, key) => jvmStats.value.push({ metric: key, value: info.value, unit: info.unit }))
  jvmStats.value.sort((a, b) => a.metric.localeCompare(b.metric))

  // HTTP TOP10 by max latency
  httpStats.value = Array.from(httpAgg.entries())
    .map(([uri, v]) => ({ uri, ...v }))
    .filter((r) => r.count > 0)
    .sort((a, b) => b.max - a.max)
    .slice(0, 10)
}

const heapMb = computed(() => {
  const heap = jvmStats.value.find((s) => s.metric === 'heap.used')
  return heap ? (heap.value / 1024 / 1024).toFixed(1) : '-'
})

function startAutoRefresh() {
  if (timer) return
  timer = setInterval(fetchOnce, 30_000)
}
function stopAutoRefresh() {
  if (timer) { clearInterval(timer); timer = null }
}

onMounted(() => {
  fetchOnce()
  startAutoRefresh()
})
onBeforeUnmount(stopAutoRefresh)
</script>

<template>
  <div class="app-container">
    <el-card class="mb-2" shadow="never">
      <div class="flex items-center gap-3 flex-wrap">
        <span class="text-sm text-gray-500">已注册业务指标 Gauge：<b>{{ trackedTables.length }}</b> 张表</span>
        <span class="text-xs text-gray-400">最近刷新：{{ lastFetched }}</span>
        <span class="text-xs text-gray-400">每 30 秒自动刷新</span>
        <el-button size="small" type="primary" :loading="loading" @click="fetchOnce" class="ml-auto">手动刷新</el-button>
      </div>
    </el-card>

    <el-row :gutter="12">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>业务表行数</template>
          <el-table :data="tableRows" border stripe size="small">
            <el-table-column prop="table" label="表" />
            <el-table-column prop="rows" label="rows" width="120" align="right">
              <template #default="{ row }">{{ row.rows.toLocaleString() }}</template>
            </el-table-column>
          </el-table>
          <div v-if="tableRows.length === 0" class="text-center text-gray-400 py-4">
            暂无业务表 — 检查模块是否启用 + Liquibase 是否完成迁移
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>JVM / 进程</template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="堆内存（已用）">{{ heapMb }} MB</el-descriptions-item>
            <el-descriptions-item v-for="r in jvmStats" :key="r.metric" :label="r.metric">
              {{ r.value.toLocaleString() }} {{ r.unit }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt-2">
      <template #header>HTTP TOP10（按最大耗时排序）</template>
      <el-table :data="httpStats" border stripe size="small">
        <el-table-column prop="uri" label="URI" min-width="280" show-overflow-tooltip />
        <el-table-column label="调用次数" width="120" align="right">
          <template #default="{ row }">{{ row.count.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="最大耗时" width="120" align="right">
          <template #default="{ row }">{{ (row.max * 1000).toFixed(1) }} ms</template>
        </el-table-column>
        <el-table-column label="累计耗时" width="120" align="right">
          <template #default="{ row }">{{ row.sum.toFixed(2) }} s</template>
        </el-table-column>
        <el-table-column label="平均" width="120" align="right">
          <template #default="{ row }">
            {{ row.count > 0 ? ((row.sum / row.count) * 1000).toFixed(1) : '0' }} ms
          </template>
        </el-table-column>
      </el-table>
      <div v-if="httpStats.length === 0" class="text-center text-gray-400 py-4">
        暂无 HTTP 调用样本（应用刚启动可能要等几秒）
      </div>
    </el-card>

    <el-card shadow="never" class="mt-2">
      <template #header>更详细的实时指标</template>
      <p class="text-sm text-gray-500">
        所有指标暴露在 <code>/actuator/prometheus</code>（已配置在 application.yml）。
        生产建议接入外部 Prometheus + Grafana 或公司监控平台获取持久化 + 报警。
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.mb-2 { margin-bottom: 8px; }
.mt-2 { margin-top: 8px; }
.ml-auto { margin-left: auto; }
.flex { display: flex; }
.items-center { align-items: center; }
.gap-3 { gap: 12px; }
.flex-wrap { flex-wrap: wrap; }
.py-4 { padding: 1rem 0; }
.text-center { text-align: center; }
.text-gray-400 { color: #9ca3af; }
.text-gray-500 { color: #6b7280; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 14px; }
</style>
