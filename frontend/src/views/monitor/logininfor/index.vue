<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listLogininfor,
  delLogininfor,
  cleanLogininfor,
  unlockLogininfor,
  type LoginInforRecord
} from '@/api/monitor/logininfor'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface LogininforQuery {
  pageNum: number
  pageSize: number
  ipaddr?: string
  userName?: string
  status?: string
  beginTime?: string
  endTime?: string
}

const loading = ref(false)
const list = ref<LoginInforRecord[]>([])
const total = ref(0)
const selected = ref<LoginInforRecord[]>([])
const query = reactive<LogininforQuery>({ pageNum: 1, pageSize: 10 })

const searchFields: SearchField[] = [
  { prop: 'userName', label: '登录账号', type: 'input' },
  { prop: 'ipaddr', label: '登录地址', type: 'input' },
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

const columns: TableColumn<LoginInforRecord>[] = [
  { prop: 'infoId', label: '编号', width: 80, align: 'center' },
  { prop: 'userName', label: '登录账号', minWidth: 120 },
  { prop: 'ipaddr', label: '登录 IP', width: 140 },
  { prop: 'loginLocation', label: '登录地点', minWidth: 140 },
  { prop: 'browser', label: '浏览器', width: 140 },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    align: 'center',
    render: 'custom',
    slot: 'status'
  },
  { prop: 'msg', label: '描述', minWidth: 160, showOverflowTooltip: true },
  { prop: 'loginTime', label: '登录时间', render: 'date', width: 180 }
]

async function fetchList() {
  loading.value = true
  try {
    const res = await listLogininfor(query)
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 10, ipaddr: '', userName: '', status: '' })
  fetchList()
}

async function handleDelete(row?: LoginInforRecord) {
  const ids = row ? [row.infoId!] : selected.value.map((s) => s.infoId!)
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中 ${ids.length} 条登录日志？`, '提示', { type: 'warning' })
    await delLogininfor(ids)
    ElMessage.success('已删除')
    fetchList()
  } catch {
    // cancel
  }
}

async function handleClean() {
  try {
    await ElMessageBox.confirm('确认清空所有登录日志？', '提示', { type: 'warning' })
    await cleanLogininfor()
    ElMessage.success('已清空')
    fetchList()
  } catch {
    // cancel
  }
}

async function handleUnlock() {
  try {
    const { value } = await ElMessageBox.prompt('请输入要解锁的账号', '账号解锁', {
      inputPattern: /\S+/,
      inputErrorMessage: '账号不能为空'
    })
    await unlockLogininfor(value)
    ElMessage.success(`${value} 已解锁`)
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
        <template #right>
          <el-button
            type="warning"
            plain
            @click="handleUnlock"
          >
            账号解锁
          </el-button>
          <el-button
            type="danger"
            plain
            @click="handleClean"
          >
            清空日志
          </el-button>
        </template>
      </PageToolbar>
      <DataTable
        :data="list"
        :columns="columns"
        :loading="loading"
        selectable
        row-key="infoId"
        @selection-change="(rows) => (selected = rows)"
      >
        <template #status="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">
            {{ row.status === '0' ? '成功' : '失败' }}
          </el-tag>
        </template>
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
