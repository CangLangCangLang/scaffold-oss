<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { listTemplates, removeTemplate, type SysReportTemplate } from '../api'

const { t } = useI18n()
const router = useRouter()

const loading = ref(false)
const list = ref<SysReportTemplate[]>([])
const total = ref(0)
interface TemplateQueryForm {
  name: string
  category: string
  status: string
  pageNum: number
  pageSize: number
}
const query = reactive<TemplateQueryForm>({
  name: '',
  category: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await listTemplates(query)
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function reset(): void {
  query.name = ''
  query.category = ''
  query.status = ''
  query.pageNum = 1
  fetchList()
}

function goAdd(): void {
  router.push({ name: 'ReportEdit', params: { id: 'new' } })
}

function goEdit(row: SysReportTemplate): void {
  router.push({ name: 'ReportEdit', params: { id: String(row.id) } })
}

function goRun(row: SysReportTemplate): void {
  router.push({ name: 'ReportRun', params: { id: String(row.id) } })
}

async function doRemove(row: SysReportTemplate): Promise<void> {
  await ElMessageBox.confirm(
    t('report.template.confirmDelete', { name: row.name }),
    t('report.common.confirm'),
    { type: 'warning' }
  )
  await removeTemplate(row.id!)
  ElMessage.success(t('report.template.deleteOk'))
  fetchList()
}

function statusLabel(s?: string): string {
  return s === '1' ? t('report.template.statusDisabled') : t('report.template.statusActive')
}

onMounted(fetchList)
</script>

<template>
  <div class="report-list app-container">
    <el-form
      :inline="true"
      :model="query"
    >
      <el-form-item :label="t('report.template.colName')">
        <el-input
          v-model="query.name"
          :placeholder="t('report.template.searchNamePh')"
          clearable
          @keyup.enter="fetchList"
        />
      </el-form-item>
      <el-form-item :label="t('report.template.colCategory')">
        <el-input
          v-model="query.category"
          clearable
          @keyup.enter="fetchList"
        />
      </el-form-item>
      <el-form-item :label="t('report.template.colStatus')">
        <el-select
          v-model="query.status"
          clearable
          style="width: 140px"
        >
          <el-option
            :label="t('report.template.statusActive')"
            value="0"
          />
          <el-option
            :label="t('report.template.statusDisabled')"
            value="1"
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
          v-hasPermi="['report:template:add']"
          type="success"
          @click="goAdd"
        >
          {{ t('report.template.add') }}
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
        prop="code"
        :label="t('report.template.colCode')"
        min-width="160"
      />
      <el-table-column
        prop="name"
        :label="t('report.template.colName')"
        min-width="180"
      />
      <el-table-column
        prop="category"
        :label="t('report.template.colCategory')"
        width="140"
      />
      <el-table-column
        prop="rowLimit"
        :label="t('report.template.colRowLimit')"
        width="120"
      />
      <el-table-column
        prop="timeoutMs"
        :label="t('report.template.colTimeoutMs')"
        width="120"
      />
      <el-table-column
        prop="permKey"
        :label="t('report.template.colPermKey')"
        width="200"
      />
      <el-table-column
        prop="status"
        :label="t('report.template.colStatus')"
        width="100"
      >
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'info'">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
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
      <el-table-column
        :label="t('report.common.action')"
        width="240"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            v-hasPermi="['report:template:run']"
            size="small"
            type="primary"
            link
            @click="goRun(row)"
          >
            {{ t('report.template.actionRun') }}
          </el-button>
          <el-button
            v-hasPermi="['report:template:edit']"
            size="small"
            type="warning"
            link
            @click="goEdit(row)"
          >
            {{ t('report.template.actionEdit') }}
          </el-button>
          <el-button
            v-hasPermi="['report:template:remove']"
            size="small"
            type="danger"
            link
            @click="doRemove(row)"
          >
            {{ t('report.template.actionRemove') }}
          </el-button>
        </template>
      </el-table-column>
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
