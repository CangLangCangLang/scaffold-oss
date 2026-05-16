<script setup lang="ts">
/**
 * Q-3 慢请求 / 5xx 列表（菜单 9002）。
 * - 顶部：阈值描述 + pending 待告警数 / 立即扫描
 * - 中部：reason / uri / 时间窗口筛选
 * - 表格：URI / 方法 / 状态 / 耗时 / TraceId / 用户名 / 时间
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listSlowRequests,
  purgeSlowRequests,
  deleteSlowRequest,
  scanSlowAlertNow,
  type SlowRequest
} from '@/api/monitor/observability'

const loading = ref(false)
const list = ref<SlowRequest[]>([])
const pending = ref(0)
const query = reactive({
  reason: '' as '' | 'SLOW' | 'SERVER_ERROR' | 'CLIENT_ERROR',
  requestUri: '',
  beginTime: '',
  endTime: ''
})

async function refresh() {
  loading.value = true
  try {
    const res = await listSlowRequests({
      reason: query.reason || undefined,
      requestUri: query.requestUri || undefined,
      beginTime: query.beginTime || undefined,
      endTime: query.endTime || undefined
    })
    list.value = (res.data?.rows ?? []) as SlowRequest[]
    pending.value = res.data?.pending ?? 0
  } finally {
    loading.value = false
  }
}

async function onPurge() {
  try {
    const days = await ElMessageBox.prompt('清理多少天之前的记录？（默认 30）', '清理慢请求', {
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入非负整数',
      inputValue: '30'
    })
    await purgeSlowRequests(Number(days.value))
    ElMessage.success('清理完成')
    refresh()
  } catch {
    /* canceled */
  }
}

async function onDelete(row: SlowRequest) {
  await ElMessageBox.confirm(`确定删除 #${row.id} ?`, '提示', { type: 'warning' })
  await deleteSlowRequest(row.id)
  ElMessage.success('已删除')
  refresh()
}

async function onScan() {
  await scanSlowAlertNow()
  ElMessage.success('扫描完成')
  refresh()
}

const tagOf = computed(() => (reason: string) => {
  if (reason === 'SLOW') return 'warning'
  if (reason === 'SERVER_ERROR') return 'danger'
  return 'info'
})

onMounted(refresh)
</script>

<template>
  <div class="app-container">
    <el-card class="mb-2" shadow="never">
      <div class="flex items-center gap-3 flex-wrap">
        <span class="text-sm text-gray-500">未告警待发：<b>{{ pending }}</b> 条</span>
        <el-button size="small" type="primary" @click="onScan">立即扫描并发告警</el-button>
        <el-button size="small" type="warning" @click="onPurge">清理 N 天前</el-button>
        <span class="text-xs text-gray-400 ml-auto">
          阈值与告警节奏来自 app.observability.* 配置；建议保留近 30 天
        </span>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-form inline class="mb-2">
        <el-form-item label="原因">
          <el-select v-model="query.reason" placeholder="全部" clearable style="width: 160px">
            <el-option label="慢" value="SLOW" />
            <el-option label="5xx 错误" value="SERVER_ERROR" />
            <el-option label="4xx 客户端错" value="CLIENT_ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item label="URI">
          <el-input v-model="query.requestUri" clearable placeholder="按 URI 模糊匹配" style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refresh">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="#" width="70" />
        <el-table-column label="原因" width="120">
          <template #default="{ row }">
            <el-tag :type="tagOf(row.reason)" size="small">{{ row.reason }}</el-tag>
            <el-tag v-if="row.alerted === '1'" type="success" size="small" class="ml-1">已告警</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="方法" width="80" />
        <el-table-column prop="requestUri" label="URI" min-width="240" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center" />
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="clientIp" label="IP" width="130" />
        <el-table-column prop="traceId" label="TraceId" width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="list.length === 0 && !loading" class="text-center text-gray-400 py-4">
        无慢请求 / 错误请求记录 — 系统正常 ✓
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.mb-2 { margin-bottom: 8px; }
.ml-1 { margin-left: 4px; }
.ml-auto { margin-left: auto; }
.flex { display: flex; }
.items-center { align-items: center; }
.gap-3 { gap: 12px; }
.flex-wrap { flex-wrap: wrap; }
</style>
