<script setup lang="ts">
/**
 * Q-3 健康检查（菜单 9004）。
 *
 * - 抓 /actuator/health（含 details）— 显示总体 status 与各 component 状态
 * - 显示 /actuator/health/scaffoldModules 中的模块清单
 * - 提供 /actuator/scaffold-modules 详细信息
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchActuator } from '@/api/monitor/observability'

interface HealthDetail {
  status: string
  components?: Record<string, { status: string; details?: Record<string, unknown> }>
  details?: Record<string, unknown>
}

const overall = ref<HealthDetail | null>(null)
const modules = ref<{ name: string; version?: string; enabled?: boolean }[]>([])
const loading = ref(false)
const lastFetched = ref('-')

async function refresh() {
  loading.value = true
  try {
    const h = await fetchActuator('health') as unknown as HealthDetail
    overall.value = h
    const m = await fetchActuator('scaffold-modules') as unknown as { modules: typeof modules.value }
    modules.value = m?.modules ?? []
    lastFetched.value = new Date().toLocaleTimeString()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.warning(`抓取 actuator 失败：${msg}`)
  } finally {
    loading.value = false
  }
}

const componentRows = computed(() => {
  const c = overall.value?.components
  if (!c) return []
  return Object.entries(c).map(([name, info]) => ({
    name,
    status: info.status,
    details: info.details ?? {}
  }))
})

function tagOf(status: string) {
  if (status === 'UP') return 'success'
  if (status === 'OUT_OF_SERVICE' || status === 'UNKNOWN') return 'warning'
  return 'danger'
}

onMounted(refresh)
</script>

<template>
  <div class="app-container">
    <el-card class="mb-2" shadow="never">
      <div class="flex items-center gap-3 flex-wrap">
        <span class="text-sm">总体：
          <el-tag v-if="overall" :type="tagOf(overall.status)">{{ overall.status }}</el-tag>
        </span>
        <span class="text-xs text-gray-400">最近刷新：{{ lastFetched }}</span>
        <el-button size="small" type="primary" class="ml-auto" :loading="loading" @click="refresh">刷新</el-button>
      </div>
    </el-card>

    <el-row :gutter="12">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>组件健康（/actuator/health）</template>
          <el-table :data="componentRows" border stripe size="small">
            <el-table-column prop="name" label="组件" min-width="160" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="tagOf(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="详情" min-width="220">
              <template #default="{ row }">
                <pre class="text-xs">{{ JSON.stringify(row.details, null, 2) }}</pre>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="componentRows.length === 0" class="text-center text-gray-400 py-4">
            未加载到组件 — 确认 management.endpoint.health.show-details=when-authorized 且当前已登录
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <template #header>已启用的脚手架模块</template>
          <el-table :data="modules" border stripe size="small">
            <el-table-column prop="name" label="模块" />
            <el-table-column prop="version" label="版本" width="100" />
            <el-table-column label="启用" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                  {{ row.enabled ? 'YES' : 'NO' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="modules.length === 0" class="text-center text-gray-400 py-4">
            没有已启用的业务模块
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt-2">
      <template #header>更多端点</template>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="/actuator/health/scaffoldModules">
          模块自检（同上面的列表，含 status=UP 兜底）
        </el-descriptions-item>
        <el-descriptions-item label="/actuator/prometheus">
          Prometheus 指标全量（业务表 / JVM / HTTP / DB）
        </el-descriptions-item>
        <el-descriptions-item label="/actuator/metrics">
          人类可读的指标列表（按 /actuator/metrics/&lt;name&gt; 查具体值）
        </el-descriptions-item>
        <el-descriptions-item label="/actuator/scaffold-modules">
          已启用模块详细信息（version + description）
        </el-descriptions-item>
      </el-descriptions>
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
.text-xs { font-size: 12px; }
.text-sm { font-size: 14px; }
pre { white-space: pre-wrap; max-width: 400px; }
</style>
