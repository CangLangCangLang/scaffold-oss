<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOnline, forceLogout, type OnlineUserRecord } from '@/api/monitor/online'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface OnlineQuery {
  pageNum: number
  pageSize: number
  ipaddr?: string
  userName?: string
}

const loading = ref(false)
const list = ref<OnlineUserRecord[]>([])
const total = ref(0)
const query = reactive<OnlineQuery>({ pageNum: 1, pageSize: 10, ipaddr: '', userName: '' })

const searchFields: SearchField[] = [
  { prop: 'userName', label: '登录账号', type: 'input' },
  { prop: 'ipaddr', label: '登录地址', type: 'input' }
]

const columns: TableColumn<OnlineUserRecord>[] = [
  { type: 'index', label: '序号', width: 60 },
  { prop: 'tokenId', label: 'Token', width: 240, showOverflowTooltip: true },
  { prop: 'userName', label: '登录账号', minWidth: 120 },
  { prop: 'ipaddr', label: '登录 IP', width: 160 },
  { prop: 'loginLocation', label: '登录地点', minWidth: 140 },
  { prop: 'browser', label: '浏览器', width: 140 },
  { prop: 'os', label: '操作系统', width: 140 },
  { prop: 'loginTime', label: '登录时间', render: 'date', width: 180 }
]

async function fetchList() {
  loading.value = true
  try {
    const res = await listOnline(query)
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 10, ipaddr: '', userName: '' })
  fetchList()
}

async function handleForceLogout(row: OnlineUserRecord) {
  try {
    await ElMessageBox.confirm(`确认强退用户：${row.userName}？`, '提示', { type: 'warning' })
    await forceLogout(row.tokenId!)
    ElMessage.success('已强退')
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
      />
      <DataTable
        :data="list"
        :columns="columns"
        :loading="loading"
      >
        <template #action>
          <el-table-column
            label="操作"
            width="120"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="danger"
                link
                @click="handleForceLogout(row)"
              >
                强退
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
