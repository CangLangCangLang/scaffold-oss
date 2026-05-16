<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { listRunLogs, purgeLogsNow, type SysReportRunLog } from '../api'

const { t } = useI18n()

const loading = ref(false)
const list = ref<SysReportRunLog[]>([])
const total = ref(0)
interface RunLogQueryForm {
  templateId?: number
  status: string
  pageNum: number
  pageSize: number
}
const query = reactive<RunLogQueryForm>({
  templateId: undefined,
  status: '',
  pageNum: 1,
  pageSize: 10
})

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const r = await listRunLogs(query)
    list.value = r.rows ?? []
    total.value = r.total ?? 0
  } finally {
    loading.value = false
  }
}

function reset(): void {
  query.templateId = undefined
  query.status = ''
  query.pageNum = 1
  fetchList()
}

async function purge(): Promise<void> {
  await purgeLogsNow(90)
  ElMessage.success(t('report.runlog.purgeOk'))
  fetchList()
}

type TagType = 'success' | 'danger' | 'warning' | 'info' | 'primary'
function statusType(s?: string): TagType {
  if (s === '0') return 'success'
  if (s === '1') return 'danger'
  if (s === '2') return 'warning'
  return 'info'
}

function statusLabel(s?: string): string {
  if (s === '0') return t('report.runlog.statusOk')
  if (s === '1') return t('report.runlog.statusFail')
  if (s === '2') return t('report.runlog.statusTimeout')
  return s ?? ''
}

onMounted(fetchList)
</script>

<template>
  <div class="run-log-list app-container">
    <el-form
      :inline="true"
      :model="query"
    >
      <el-form-item :label="t('report.runlog.colTemplateId')">
        <el-input-number
          v-model="query.templateId"
          :min="1"
        />
      </el-form-item>
      <el-form-item :label="t('report.runlog.colStatus')">
        <el-select
          v-model="query.status"
          clearable
          style="width: 140px"
        >
          <el-option
            :label="t('report.runlog.statusOk')"
            value="0"
          />
          <el-option
            :label="t('report.runlog.statusFail')"
            value="1"
          />
          <el-option
            :label="t('report.runlog.statusTimeout')"
            value="2"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          @click="fetchList"
        >
          {{ t('report.common.search') }}
        </el-button>
        <el-button @click="reset">
          {{ t('report.common.reset') }}
        </el-button>
        <el-button
          v-hasPermi="['report:log:list']"
          type="warning"
          @click="purge"
        >
          {{ t('report.runlog.purge') }}
        </el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="list"
      border
    >
      <el-table-column
        prop="id"
        label="ID"
        width="80"
      />
      <el-table-column
        prop="templateId"
        :label="t('report.runlog.colTemplateId')"
        width="100"
      />
      <el-table-column
        prop="templateCode"
        :label="t('report.runlog.colTemplateCode')"
        width="160"
      />
      <el-table-column
        prop="datasourceId"
        :label="t('report.runlog.colDataSource')"
        width="100"
      />
      <el-table-column
        prop="rowCount"
        :label="t('report.runlog.colRowCount')"
        width="100"
      />
      <el-table-column
        prop="costMs"
        :label="t('report.runlog.colCostMs')"
        width="100"
      />
      <el-table-column
        prop="status"
        :label="t('report.runlog.colStatus')"
        width="100"
      >
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="errorMsg"
        :label="t('report.runlog.colError')"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column
        prop="sqlPreview"
        :label="t('report.runlog.colSqlPreview')"
        min-width="240"
        show-overflow-tooltip
      />
      <el-table-column
        prop="createBy"
        :label="t('report.template.colCreator')"
        width="140"
      />
      <el-table-column
        prop="createTime"
        :label="t('report.template.colCreateTime')"
        width="160"
      />
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 16px"
      @current-change="fetchList"
      @size-change="fetchList"
    />
  </div>
</template>
