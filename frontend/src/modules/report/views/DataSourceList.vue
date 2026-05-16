<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  addDataSource,
  listDataSources,
  removeDataSource,
  testDataSource,
  updateDataSource,
  type DataSourceUpsertRequest,
  type SysReportDataSource
} from '../api'

const { t } = useI18n()

const loading = ref(false)
const list = ref<SysReportDataSource[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive<DataSourceUpsertRequest>({
  code: '',
  name: '',
  type: 'mysql',
  jdbcUrl: '',
  driverClass: '',
  username: '',
  password: '', // 新建：明文；编辑：留空表示不动
  status: '0',
  remark: ''
})
/** 编辑时若用户点击 "清空密码" 复选 → 把 password 显式发空串 */
const clearPassword = ref(false)

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const r = await listDataSources()
    list.value = r.data ?? []
  } finally {
    loading.value = false
  }
}

function openAdd(): void {
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    code: '',
    name: '',
    type: 'mysql',
    jdbcUrl: '',
    driverClass: '',
    username: '',
    password: '',
    status: '0',
    remark: ''
  })
  clearPassword.value = false
  dialogVisible.value = true
}

function openEdit(row: SysReportDataSource): void {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    code: row.code,
    name: row.name,
    type: row.type ?? 'mysql',
    jdbcUrl: row.jdbcUrl,
    driverClass: row.driverClass,
    username: row.username,
    password: null, // 编辑：默认不动
    status: row.status,
    remark: row.remark
  })
  clearPassword.value = false
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  loading.value = true
  try {
    const body = { ...form }
    if (isEdit.value) {
      // 编辑：根据用户意图修复 password 字段
      if (clearPassword.value) {
        body.password = '' // 显式清空
      } else if (!body.password) {
        body.password = null // null 不动
      }
      await updateDataSource(body)
    } else {
      await addDataSource(body)
    }
    ElMessage.success(t('report.common.saveOk'))
    dialogVisible.value = false
    fetchList()
  } finally {
    loading.value = false
  }
}

async function doTest(): Promise<void> {
  loading.value = true
  try {
    const body = { ...form }
    if (isEdit.value && !body.password) body.password = null
    await testDataSource(body)
    ElMessage.success(t('report.datasource.testOk'))
  } finally {
    loading.value = false
  }
}

async function doRemove(row: SysReportDataSource): Promise<void> {
  await ElMessageBox.confirm(
    t('report.datasource.confirmDelete', { name: row.name }),
    t('report.common.confirm'),
    { type: 'warning' }
  )
  await removeDataSource(row.id)
  ElMessage.success(t('report.common.deleteOk'))
  fetchList()
}

function statusLabel(s?: string): string {
  return s === '1' ? t('report.template.statusDisabled') : t('report.template.statusActive')
}

onMounted(fetchList)
</script>

<template>
  <div class="ds-list app-container">
    <div style="margin-bottom: 12px">
      <el-button
        v-hasPermi="['report:datasource:add']"
        type="primary"
        @click="openAdd"
      >
        {{ t('report.datasource.add') }}
      </el-button>
    </div>

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
        :label="t('report.datasource.colCode')"
        width="160"
      />
      <el-table-column
        prop="name"
        :label="t('report.datasource.colName')"
        min-width="160"
      />
      <el-table-column
        prop="type"
        :label="t('report.datasource.colType')"
        width="100"
      />
      <el-table-column
        prop="jdbcUrl"
        :label="t('report.datasource.colJdbcUrl')"
        min-width="240"
        show-overflow-tooltip
      />
      <el-table-column
        prop="username"
        :label="t('report.datasource.colUsername')"
        width="140"
      />
      <el-table-column
        :label="t('report.datasource.colPassword')"
        width="120"
      >
        <template #default="{ row }">
          <span class="muted">{{ row.passwordMask || '-' }}</span>
        </template>
      </el-table-column>
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
        :label="t('report.common.action')"
        width="220"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            v-hasPermi="['report:datasource:edit']"
            size="small"
            type="primary"
            link
            @click="openEdit(row)"
          >
            {{ t('report.common.edit') }}
          </el-button>
          <el-button
            v-hasPermi="['report:datasource:remove']"
            size="small"
            type="danger"
            link
            @click="doRemove(row)"
          >
            {{ t('report.common.remove') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('report.datasource.edit') : t('report.datasource.add')"
      width="600px"
    >
      <el-form
        :model="form"
        label-width="120px"
      >
        <el-form-item
          :label="t('report.datasource.colCode')"
          required
        >
          <el-input
            v-model="form.code"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item
          :label="t('report.datasource.colName')"
          required
        >
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('report.datasource.colType')">
          <el-select
            v-model="form.type"
            style="width: 200px"
          >
            <el-option
              label="mysql"
              value="mysql"
            />
            <el-option
              label="postgres"
              value="postgres"
            />
            <el-option
              label="sqlserver"
              value="sqlserver"
            />
            <el-option
              label="oracle"
              value="oracle"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="t('report.datasource.colJdbcUrl')"
          required
        >
          <el-input
            v-model="form.jdbcUrl"
            :placeholder="t('report.datasource.jdbcUrlPh')"
          />
        </el-form-item>
        <el-form-item :label="t('report.datasource.colDriverClass')">
          <el-input
            v-model="form.driverClass"
            :placeholder="t('report.datasource.driverClassPh')"
          />
        </el-form-item>
        <el-form-item :label="t('report.datasource.colUsername')">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item :label="t('report.datasource.colPassword')">
          <el-input
            v-model="(form.password as string | null) as string"
            :placeholder="isEdit ? t('report.datasource.passwordEditPh') : t('report.datasource.passwordPh')"
            show-password
            :disabled="isEdit && clearPassword"
          />
          <el-checkbox
            v-if="isEdit"
            v-model="clearPassword"
            style="margin-top: 4px"
          >
            {{ t('report.datasource.clearPassword') }}
          </el-checkbox>
        </el-form-item>
        <el-form-item :label="t('report.template.colStatus')">
          <el-select v-model="form.status">
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
        <el-form-item :label="t('report.template.colRemark')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button
          v-hasPermi="['report:datasource:test']"
          @click="doTest"
        >
          {{ t('report.datasource.test') }}
        </el-button>
        <el-button @click="dialogVisible = false">
          {{ t('report.common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="submit"
        >
          {{ t('report.common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.muted { color: #909399 }
</style>
