<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElDialog, ElDescriptions, ElDescriptionsItem } from 'element-plus'
import { listOperlog, delOperlog, cleanOperlog, type OperLogRecord } from '@/api/monitor/operlog'
import { useDict } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface OperlogQuery {
  pageNum: number
  pageSize: number
  title?: string
  operName?: string
  businessType?: number
  status?: number
  beginTime?: string
  endTime?: string
}

const dicts = useDict('sys_oper_type', 'sys_common_status')
const loading = ref(false)
const list = ref<OperLogRecord[]>([])
const total = ref(0)
const selected = ref<OperLogRecord[]>([])
const query = reactive<OperlogQuery>({ pageNum: 1, pageSize: 10 })
const detailVisible = ref(false)
const detail = ref<OperLogRecord>({})

const searchFields: SearchField[] = [
  { prop: 'title', label: '系统模块', type: 'input' },
  { prop: 'operName', label: '操作人员', type: 'input' }
]

const columns: TableColumn<OperLogRecord>[] = [
  { prop: 'operId', label: '编号', width: 80, align: 'center' },
  { prop: 'title', label: '系统模块', minWidth: 120 },
  { prop: 'businessType', label: '业务类型', dict: 'sys_oper_type', render: 'tag', width: 110, align: 'center' },
  { prop: 'operName', label: '操作人员', width: 120 },
  { prop: 'operIp', label: '主机', width: 140 },
  { prop: 'status', label: '状态', dict: 'sys_common_status', render: 'tag', width: 90, align: 'center' },
  { prop: 'operTime', label: '操作日期', render: 'date', width: 180 },
  { prop: 'costTime', label: '耗时', width: 100, align: 'center', formatter: (_row, value) => `${value ?? 0} ms` }
]

async function fetchList() {
  loading.value = true
  try {
    const res = await listOperlog(query)
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 10, title: '', operName: '', status: undefined })
  fetchList()
}

async function handleDelete(row?: OperLogRecord) {
  const ids = row ? [row.operId!] : selected.value.map((s) => s.operId!)
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确认删除 ${ids.length} 条操作日志？`, '提示', { type: 'warning' })
    await delOperlog(ids)
    ElMessage.success('已删除')
    fetchList()
  } catch {
    // cancel
  }
}

async function handleClean() {
  try {
    await ElMessageBox.confirm('确认清空所有操作日志？', '提示', { type: 'warning' })
    await cleanOperlog()
    ElMessage.success('已清空')
    fetchList()
  } catch {
    // cancel
  }
}

function handleDetail(row: OperLogRecord) {
  detail.value = row
  detailVisible.value = true
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
        :selected-count="selected.length"
        hide-add
        @delete="handleDelete()"
        @refresh="fetchList"
      >
        <template #right>
          <el-button
            type="danger"
            plain
            @click="handleClean"
          >
            清空
          </el-button>
        </template>
      </PageToolbar>
      <DataTable
        :data="list"
        :columns="columns"
        :loading="loading"
        selectable
        row-key="operId"
        @selection-change="(rows) => (selected = rows)"
      >
        <template #action>
          <el-table-column
            label="操作"
            width="160"
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
              <el-button
                type="danger"
                link
                @click="handleDelete(row)"
              >
                删除
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
    <ElDialog
      v-model="detailVisible"
      title="操作日志详情"
      width="720px"
    >
      <ElDescriptions
        :column="2"
        border
      >
        <ElDescriptionsItem label="系统模块">
          {{ detail.title }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="操作类型">
          <el-tag>{{ detail.businessType }}</el-tag>
        </ElDescriptionsItem>
        <ElDescriptionsItem label="操作人员">
          {{ detail.operName }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="主机地址">
          {{ detail.operIp }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="请求方法">
          {{ detail.method }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="操作 URL">
          {{ detail.operUrl }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="操作日期">
          {{ detail.operTime }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="耗时">
          {{ detail.costTime }} ms
        </ElDescriptionsItem>
        <ElDescriptionsItem
          label="请求参数"
          :span="2"
        >
          <pre class="operlog__pre">{{ detail.operParam }}</pre>
        </ElDescriptionsItem>
        <ElDescriptionsItem
          label="返回参数"
          :span="2"
        >
          <pre class="operlog__pre">{{ detail.jsonResult }}</pre>
        </ElDescriptionsItem>
        <ElDescriptionsItem
          v-if="detail.errorMsg"
          label="错误信息"
          :span="2"
        >
          <pre class="operlog__pre operlog__pre--error">{{ detail.errorMsg }}</pre>
        </ElDescriptionsItem>
      </ElDescriptions>
    </ElDialog>
  </div>
</template>

<style scoped lang="scss">
.operlog__pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  font-size: 12px;
  background: #f3f4f6;
  padding: 8px;
  border-radius: 4px;
  max-height: 220px;
  overflow: auto;

  &--error {
    color: #b91c1c;
    background: #fef2f2;
  }
}
</style>
