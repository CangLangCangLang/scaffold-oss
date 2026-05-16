<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAuditLog,
  getAuditLog,
  deleteAuditLogOlder,
  type AuditLogRecord,
  type AuditLogQuery
} from '@/api/system/audit'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import Pagination from '@/components/Pagination.vue'
import AuditDiffViewer from './AuditDiffViewer.vue'
import type { TableColumn, SearchField } from '@/types/table'

const loading = ref(false)
const list = ref<AuditLogRecord[]>([])
const total = ref(0)
const query = reactive<AuditLogQuery>({ pageNum: 1, pageSize: 10 })
const detailVisible = ref(false)
const detail = ref<AuditLogRecord>({})

const searchFields: SearchField[] = [
  { prop: 'module', label: '模块', type: 'input', placeholder: 'system.user / workflow.process' },
  { prop: 'action', label: '动作', type: 'input', placeholder: 'CREATE / UPDATE / ...' },
  { prop: 'resourceType', label: '资源类型', type: 'input' },
  { prop: 'resourceId', label: '资源 ID', type: 'input' },
  { prop: 'actor', label: '操作人', type: 'input' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '成功', value: 0 },
      { label: '失败', value: 1 }
    ]
  }
]

const columns: TableColumn<AuditLogRecord>[] = [
  { prop: 'id', label: '编号', width: 80, align: 'center' },
  { prop: 'module', label: '模块', minWidth: 140 },
  { prop: 'action', label: '动作', width: 140, align: 'center' },
  { prop: 'resourceType', label: '资源', width: 120, align: 'center' },
  { prop: 'resourceId', label: '资源 ID', width: 140 },
  { prop: 'actor', label: '操作人', width: 120 },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    align: 'center',
    formatter: (_row, value) => (Number(value) === 0 ? '成功' : '失败')
  },
  { prop: 'createdAt', label: '时间', render: 'date', width: 180 },
  { prop: 'costMs', label: '耗时', width: 100, align: 'center', formatter: (_row, v) => `${v ?? 0} ms` }
]

async function fetchList() {
  loading.value = true
  try {
    const res = await listAuditLog(query)
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    module: '',
    action: '',
    resourceType: '',
    resourceId: '',
    actor: '',
    status: undefined
  })
  fetchList()
}

async function handleDetail(row: AuditLogRecord) {
  if (!row.id) return
  const res = await getAuditLog(row.id)
  detail.value = res.data || row
  detailVisible.value = true
}

async function handlePurge() {
  try {
    const { value } = await ElMessageBox.prompt(
      '保留最近多少天的审计日志？（旧记录会被物理删除）',
      '清理审计日志',
      { confirmButtonText: '确认清理', cancelButtonText: '取消', inputPattern: /^\d+$/, inputValue: '180' }
    )
    const days = Number(value)
    if (Number.isNaN(days) || days < 1) return
    const res = await deleteAuditLogOlder(days)
    ElMessage.success(`已清理 ${res.data?.affected ?? 0} 条`)
    fetchList()
  } catch {
    // cancel
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="scaffold-page">
    <SearchForm
      v-model="query"
      :fields="searchFields"
      @search="fetchList"
      @reset="resetQuery"
    />
    <div class="scaffold-card">
      <PageToolbar
        hide-add
        hide-delete
        @refresh="fetchList"
      >
        <template #right>
          <el-button
            type="danger"
            plain
            @click="handlePurge"
          >
            清理旧数据
          </el-button>
        </template>
      </PageToolbar>
      <DataTable
        :data="list"
        :columns="columns"
        :loading="loading"
        row-key="id"
      >
        <template #action>
          <el-table-column
            label="操作"
            width="100"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                @click="handleDetail(row)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </template>
      </DataTable>
      <Pagination
        :page-num="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        @update:page-num="(v) => (query.pageNum = v)"
        @update:page-size="(v) => (query.pageSize = v)"
        @change="fetchList"
      />
    </div>

    <el-dialog
      v-model="detailVisible"
      title="审计日志详情"
      width="900px"
    >
      <el-descriptions
        :column="2"
        border
      >
        <el-descriptions-item label="模块">
          {{ detail.module }}
        </el-descriptions-item>
        <el-descriptions-item label="动作">
          <el-tag>{{ detail.action }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="资源类型">
          {{ detail.resourceType || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="资源 ID">
          {{ detail.resourceId || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ detail.actor }}
        </el-descriptions-item>
        <el-descriptions-item label="部门">
          {{ detail.actorDept || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="客户端 IP">
          {{ detail.clientIp || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="trace_id">
          {{ detail.traceId || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="请求 URI">
          {{ detail.requestUri || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ detail.costMs ?? 0 }} ms
        </el-descriptions-item>
        <el-descriptions-item
          label="说明"
          :span="2"
        >
          {{ detail.comment || '—' }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="detail.errorMessage"
          label="错误信息"
          :span="2"
        >
          <span style="color: var(--el-color-danger)">{{ detail.errorMessage }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <h4 class="audit-section">
        变更内容
      </h4>
      <AuditDiffViewer
        :before="detail.beforeValue"
        :after="detail.afterValue"
        :diff="detail.diff"
        :resource-type="detail.resourceType"
      />
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.audit-section {
  margin: 18px 0 8px;
  font-size: 14px;
  color: var(--el-text-color-primary);
}
</style>
