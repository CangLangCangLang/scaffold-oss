<script setup lang="ts">
/**
 * Inbox 全页面（/system/message/inbox）：
 * 顶部铃铛 popover 只显示最近未读 50 条；用户想回查、批量管理时进这个页面。
 *
 * 设计：
 * - 过滤：状态（多选）/ 类型 LIKE / 时间窗口
 * - 列：状态 tag / type / payload 摘要 / 时间 / 单条「已读」「删除」
 * - 选择 + 批量已读 / 批量删除（PageToolbar 内置批量删除按钮）
 * - 详情 dialog：完整 payload + meta（messageId / scope / target / status / read_at / expire_at）
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  pageInbox,
  ackInbox,
  ackBatchInbox,
  deleteOneInbox,
  deleteBatchInbox,
  type InboxEntry,
  type InboxQuery
} from '../api'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import Pagination from '@/components/Pagination.vue'
import { useNotificationStore } from '../store'
import type { TableColumn, SearchField } from '@/types/table'

const { t } = useI18n()

interface FilterForm {
  pageNum: number
  pageSize: number
  status?: number | ''
  typeKeyword?: string
  range?: [string, string] | null
}

const loading = ref(false)
const list = ref<InboxEntry[]>([])
const total = ref(0)
const selected = ref<InboxEntry[]>([])
const detailVisible = ref(false)
const detail = ref<InboxEntry | null>(null)

const query = reactive<FilterForm>({
  pageNum: 1,
  pageSize: 10,
  status: '',
  typeKeyword: '',
  range: null
})

const searchFields = computed<SearchField[]>(() => [
  {
    prop: 'typeKeyword',
    label: t('inbox.full.search.type'),
    type: 'input',
    placeholder: t('inbox.full.search.typePlaceholder')
  },
  {
    prop: 'status',
    label: t('inbox.full.search.status'),
    type: 'select',
    options: [
      { label: t('inbox.full.status.unread'), value: 0 },
      { label: t('inbox.full.status.read'), value: 1 },
      { label: t('inbox.full.status.expired'), value: 2 }
    ]
  },
  { prop: 'range', label: t('inbox.full.search.range'), type: 'date-range' }
])

const statusType: Record<number, 'warning' | 'success' | 'info'> = {
  0: 'warning',
  1: 'success',
  2: 'info'
}
const statusLabel = computed<Record<number, string>>(() => ({
  0: t('inbox.full.status.unread'),
  1: t('inbox.full.status.read'),
  2: t('inbox.full.status.expired')
}))

const columns = computed<TableColumn<InboxEntry>[]>(() => [
  { prop: 'id', label: t('inbox.full.column.id'), width: 80, align: 'center' },
  {
    prop: 'status',
    label: t('common.status'),
    width: 90,
    align: 'center',
    slot: 'status',
    render: 'custom'
  },
  { prop: 'type', label: t('inbox.full.column.type'), minWidth: 180 },
  {
    prop: 'payload',
    label: t('inbox.full.column.content'),
    minWidth: 260,
    showOverflowTooltip: true,
    formatter: (_row, value) => describePayload(value)
  },
  { prop: 'createdAt', label: t('inbox.full.column.time'), render: 'date', width: 180 }
])

function describePayload(payload: unknown): string {
  if (payload == null) return '—'
  if (typeof payload === 'string') {
    try {
      const parsed = JSON.parse(payload) as unknown
      return typeof parsed === 'object' && parsed !== null ? JSON.stringify(parsed) : String(parsed)
    } catch {
      return payload
    }
  }
  if (typeof payload === 'object') {
    try {
      return JSON.stringify(payload)
    } catch {
      return String(payload)
    }
  }
  return String(payload)
}

function buildApiQuery(): InboxQuery {
  const q: InboxQuery = { pageNum: query.pageNum, pageSize: query.pageSize }
  if (query.status === 0 || query.status === 1 || query.status === 2) q.statuses = [query.status]
  if (query.typeKeyword?.trim()) q.typeKeyword = query.typeKeyword.trim()
  if (query.range && query.range.length === 2) {
    q.fromTime = `${query.range[0]}T00:00:00`
    q.toTime = `${query.range[1]}T23:59:59`
  }
  return q
}

async function fetchList() {
  loading.value = true
  try {
    const res = await pageInbox(buildApiQuery())
    list.value = (res.rows ?? []).map(normalizeRow)
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function normalizeRow(row: InboxEntry): InboxEntry {
  if (typeof row.payload === 'string') {
    try {
      return { ...row, payload: JSON.parse(row.payload) }
    } catch {
      return row
    }
  }
  return row
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = 10
  query.status = ''
  query.typeKeyword = ''
  query.range = null
  fetchList()
}

function onSelectionChange(rows: InboxEntry[]) {
  selected.value = rows
}

const selectedIds = computed(() => selected.value.map((r) => r.id))

async function handleAckBatch() {
  if (selectedIds.value.length === 0) return
  const res = await ackBatchInbox(selectedIds.value)
  ElMessage.success(t('inbox.full.batch.ackedCount', { count: res.data?.count ?? 0 }))
  await refreshAfterMutation()
}

async function handleDeleteBatch() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      t('inbox.full.batch.confirmDelete', { count: selectedIds.value.length }),
      t('inbox.full.batch.deleteTitle'),
      {
        type: 'warning',
        confirmButtonText: t('inbox.full.batch.deleteAction'),
        cancelButtonText: t('common.cancel')
      }
    )
  } catch {
    return
  }
  const res = await deleteBatchInbox(selectedIds.value)
  ElMessage.success(t('inbox.full.batch.deletedCount', { count: res.data?.count ?? 0 }))
  await refreshAfterMutation()
}

async function handleAckOne(row: InboxEntry) {
  if (row.status !== 0) return
  await ackInbox(row.id)
  ElMessage.success(t('inbox.full.batch.ackOneOk'))
  await refreshAfterMutation()
}

async function handleDeleteOne(row: InboxEntry) {
  try {
    await ElMessageBox.confirm(
      t('inbox.full.batch.confirmDeleteOne'),
      t('inbox.full.batch.deleteOneTitle'),
      {
        type: 'warning',
        confirmButtonText: t('inbox.full.batch.deleteAction'),
        cancelButtonText: t('common.cancel')
      }
    )
  } catch {
    return
  }
  await deleteOneInbox(row.id)
  ElMessage.success(t('inbox.full.batch.deletedOne'))
  await refreshAfterMutation()
}

async function refreshAfterMutation() {
  selected.value = []
  await fetchList()
  try {
    const store = useNotificationStore()
    await store.loadUnreadFromInbox()
  } catch {
    // store 未就绪不影响主流程
  }
}

function handleDetail(row: InboxEntry) {
  detail.value = row
  detailVisible.value = true
}

function prettyJson(v: unknown): string {
  if (v == null) return '—'
  try {
    return JSON.stringify(v, null, 2)
  } catch {
    return String(v)
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
        :selected-count="selected.length"
        @delete="handleDeleteBatch"
        @refresh="fetchList"
      >
        <template #left>
          <el-button
            type="primary"
            plain
            :icon="Check"
            :disabled="selected.length === 0"
            @click="handleAckBatch"
          >
            {{ t('inbox.full.batch.ackBtn') }}
          </el-button>
        </template>
      </PageToolbar>
      <DataTable
        :data="list"
        :columns="columns"
        :loading="loading"
        row-key="id"
        selectable
        @selection-change="onSelectionChange"
      >
        <template #status="{ row }">
          <el-tag
            size="small"
            :type="statusType[row.status as number] ?? 'info'"
          >
            {{ statusLabel[row.status as number] ?? row.status }}
          </el-tag>
        </template>
        <template #action>
          <el-table-column
            :label="t('common.operation')"
            width="170"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                @click="handleDetail(row)"
              >
                {{ t('common.detail') }}
              </el-button>
              <el-button
                v-if="row.status === 0"
                type="success"
                link
                :icon="Check"
                @click="handleAckOne(row)"
              >
                {{ t('inbox.full.row.ackBtn') }}
              </el-button>
              <el-button
                type="danger"
                link
                :icon="Delete"
                @click="handleDeleteOne(row)"
              >
                {{ t('common.delete') }}
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
      :title="t('inbox.full.detail.title')"
      width="720px"
    >
      <el-descriptions
        v-if="detail"
        :column="2"
        border
      >
        <el-descriptions-item :label="t('inbox.full.detail.messageId')">
          {{ detail.messageId }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">
          <el-tag
            size="small"
            :type="statusType[detail.status] ?? 'info'"
          >
            {{ statusLabel[detail.status] ?? detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('inbox.full.detail.scope')">
          {{ detail.scope }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('inbox.full.detail.target')">
          {{ detail.target }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('inbox.full.detail.type')">
          {{ detail.type }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('inbox.full.detail.createdAt')">
          {{ detail.createdAt }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('inbox.full.detail.readAt')">
          {{ detail.readAt || '—' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('inbox.full.detail.expireAt')">
          {{ detail.expireAt || '—' }}
        </el-descriptions-item>
      </el-descriptions>
      <h4 class="inbox-section">
        {{ t('inbox.full.detail.payload') }}
      </h4>
      <pre class="inbox-payload">{{ prettyJson(detail?.payload) }}</pre>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.inbox-section {
  margin: 18px 0 8px;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.inbox-payload {
  background: var(--el-fill-color-light);
  border-radius: 4px;
  padding: 8px 12px;
  max-height: 320px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
