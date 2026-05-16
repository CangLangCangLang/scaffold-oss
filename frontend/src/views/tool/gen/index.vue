<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listTable,
  listDbTable,
  importTable,
  previewTable,
  delTable,
  genCode,
  synchDb,
  type GenTableRecord
} from '@/api/tool/gen'
import { useCrud } from '@/composables/useCrud'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface GenQuery {
  pageNum: number
  pageSize: number
  tableName?: string
  tableComment?: string
}

const router = useRouter()
const importDialog = ref(false)
const dbTables = ref<GenTableRecord[]>([])
const dbSelected = ref<GenTableRecord[]>([])
const dbLoading = ref(false)
const dbQuery = ref({ pageNum: 1, pageSize: 10, tableName: '', tableComment: '' })
const dbTotal = ref(0)
const previewVisible = ref(false)
const previewMap = ref<Record<string, string>>({})
const previewKey = ref('')

const crud = useCrud<GenQuery & Record<string, unknown>, GenTableRecord>({
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, tableName: '', tableComment: '' }),
  fetchList: listTable,
  rowKey: 'tableId',
  remove: delTable
})

const searchFields: SearchField[] = [
  { prop: 'tableName', label: '表名称', type: 'input' },
  { prop: 'tableComment', label: '表描述', type: 'input' }
]

const columns: TableColumn<GenTableRecord>[] = [
  { prop: 'tableId', label: '编号', width: 80, align: 'center' },
  { prop: 'tableName', label: '表名称', minWidth: 160 },
  { prop: 'tableComment', label: '表描述', minWidth: 160 },
  { prop: 'className', label: '实体名', minWidth: 140 },
  { prop: 'createTime', label: '创建时间', render: 'date', width: 180 }
]

async function openImport() {
  importDialog.value = true
  await loadDbTables()
}

async function loadDbTables() {
  dbLoading.value = true
  try {
    const res = await listDbTable(dbQuery.value)
    dbTables.value = res.rows ?? []
    dbTotal.value = res.total ?? 0
  } finally {
    dbLoading.value = false
  }
}

async function confirmImport() {
  if (!dbSelected.value.length) {
    ElMessage.warning('请至少选择一张表')
    return
  }
  const tables = dbSelected.value.map((t) => t.tableName).join(',')
  await importTable({ tables })
  ElMessage.success('已导入')
  importDialog.value = false
  await crud.fetchList()
}

async function handlePreview(row: GenTableRecord) {
  const res = (await previewTable(row.tableId!)) as { data?: Record<string, string> }
  previewMap.value = res.data ?? {}
  previewKey.value = Object.keys(previewMap.value)[0] ?? ''
  previewVisible.value = true
}

async function handleGen(row: GenTableRecord) {
  await genCode(row.tableName!)
  ElMessage.success('代码已下载到服务器配置目录')
}

async function handleSync(row: GenTableRecord) {
  try {
    await ElMessageBox.confirm('同步会从数据库重新拉取字段，可能覆盖手动编辑，确认？', '提示', { type: 'warning' })
    await synchDb(row.tableName!)
    ElMessage.success('已同步')
    crud.fetchList()
  } catch {
    // cancel
  }
}

function handleEdit(row: GenTableRecord) {
  router.push(`/tool/gen/edit/${row.tableId}`)
}

onMounted(crud.fetchList)
</script>

<template>
  <div class="scaffold-page">
    <SearchForm
      v-model="crud.query"
      :fields="searchFields"
      @search="crud.fetchList"
      @reset="crud.resetQuery"
    />
    <div class="scaffold-card">
      <PageToolbar
        :selected-count="crud.selected.value.length"
        hide-add
        @delete="crud.handleDelete()"
        @refresh="crud.fetchList"
      >
        <template #left>
          <el-button
            type="primary"
            @click="openImport"
          >
            导入数据表
          </el-button>
        </template>
      </PageToolbar>
      <DataTable
        :data="crud.list.value"
        :columns="columns"
        :loading="crud.loading.value"
        selectable
        row-key="tableId"
        @selection-change="crud.handleSelectionChange"
      >
        <template #action>
          <el-table-column
            label="操作"
            width="280"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                @click="handlePreview(row)"
              >
                预览
              </el-button>
              <el-button
                type="primary"
                link
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                type="warning"
                link
                @click="handleSync(row)"
              >
                同步
              </el-button>
              <el-button
                type="success"
                link
                @click="handleGen(row)"
              >
                生成
              </el-button>
              <el-button
                type="danger"
                link
                @click="crud.handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </template>
      </DataTable>
      <Pagination
        :page-num="crud.query.pageNum"
        :page-size="crud.query.pageSize"
        :total="crud.total.value"
        @update:page-num="(v) => (crud.query.pageNum = v)"
        @update:page-size="(v) => (crud.query.pageSize = v)"
        @change="crud.fetchList"
      />
    </div>

    <el-dialog
      v-model="importDialog"
      title="导入表"
      width="800px"
    >
      <el-input
        v-model="dbQuery.tableName"
        placeholder="过滤表名"
        size="small"
        clearable
        style="width: 240px; margin-bottom: 12px"
        @keyup.enter="loadDbTables"
      />
      <el-table
        v-loading="dbLoading"
        :data="dbTables"
        :max-height="400"
        @selection-change="(rows) => (dbSelected = rows)"
      >
        <el-table-column
          type="selection"
          width="48"
        />
        <el-table-column
          prop="tableName"
          label="表名"
          min-width="180"
        />
        <el-table-column
          prop="tableComment"
          label="描述"
          min-width="180"
        />
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
        />
      </el-table>
      <template #footer>
        <el-button @click="importDialog = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="confirmImport"
        >
          导入
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewVisible"
      title="代码预览"
      width="900px"
    >
      <div class="preview">
        <el-tabs
          v-model="previewKey"
          tab-position="left"
        >
          <el-tab-pane
            v-for="(content, key) in previewMap"
            :key="key"
            :label="String(key).split('/').pop()"
            :name="key"
          >
            <pre class="preview__code">{{ content }}</pre>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.preview {
  height: 480px;

  &__code {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-all;
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 12px;
    background: #1f2937;
    color: #e5e7eb;
    padding: 12px;
    border-radius: 6px;
    height: 440px;
    overflow: auto;
  }
}
</style>
