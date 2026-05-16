<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import { listJobLog, delJobLog, cleanJobLog, type JobLogRecord } from '@/api/monitor/job'
import { useDict } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface JobLogQuery {
  pageNum: number
  pageSize: number
  jobName?: string
  jobGroup?: string
  status?: string
}

const router = useRouter()
useDict('sys_common_status', 'sys_job_group')
const loading = ref(false)
const list = ref<JobLogRecord[]>([])
const total = ref(0)
const selected = ref<JobLogRecord[]>([])
const query = reactive<JobLogQuery>({ pageNum: 1, pageSize: 10 })

const searchFields: SearchField[] = [
  { prop: 'jobName', label: '任务名称', type: 'input' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { value: '0', label: '成功' },
      { value: '1', label: '失败' }
    ]
  }
]

const columns: TableColumn<JobLogRecord>[] = [
  { prop: 'jobLogId', label: '日志编号', width: 100, align: 'center' },
  { prop: 'jobName', label: '任务名称', minWidth: 140 },
  { prop: 'jobGroup', label: '任务分组', dict: 'sys_job_group', render: 'tag', width: 100, align: 'center' },
  { prop: 'invokeTarget', label: '调用目标', minWidth: 200, showOverflowTooltip: true },
  { prop: 'jobMessage', label: '日志信息', minWidth: 200, showOverflowTooltip: true },
  { prop: 'status', label: '执行状态', dict: 'sys_common_status', render: 'tag', width: 100, align: 'center' },
  { prop: 'createTime', label: '执行时间', render: 'date', width: 180 }
]

async function fetchList() {
  loading.value = true
  try {
    const res = await listJobLog(query)
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 10, jobName: '', jobGroup: '', status: '' })
  fetchList()
}

async function handleDelete(row?: JobLogRecord) {
  const ids = row ? [row.jobLogId!] : selected.value.map((s) => s.jobLogId!)
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确认删除 ${ids.length} 条调度日志？`, '提示', { type: 'warning' })
    await delJobLog(ids)
    ElMessage.success('已删除')
    fetchList()
  } catch {
    // cancel
  }
}

async function handleClean() {
  try {
    await ElMessageBox.confirm('确认清空所有调度日志？', '提示', { type: 'warning' })
    await cleanJobLog()
    ElMessage.success('已清空')
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
        :selected-count="selected.length"
        hide-add
        @delete="handleDelete()"
        @refresh="fetchList"
      >
        <template #left>
          <el-button
            :icon="Back"
            @click="router.push('/monitor/job')"
          >
            返回任务列表
          </el-button>
        </template>
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
        row-key="jobLogId"
        @selection-change="(rows) => (selected = rows)"
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
  </div>
</template>
