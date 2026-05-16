<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  addTemplate,
  getTemplate,
  listDataSources,
  updateTemplate,
  validateTemplate,
  type ParamDecl,
  type SysReportDataSource,
  type SysReportTemplate
} from '../api'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const isNew = computed(() => route.params.id === 'new')

const form = reactive<SysReportTemplate>({
  code: '',
  name: '',
  category: '',
  datasourceId: 0,
  sqlText: 'SELECT 1 AS hello',
  paramSchema: '[]',
  rowLimit: 1000,
  timeoutMs: 5000,
  permKey: '',
  status: '0',
  remark: ''
})

const datasources = ref<SysReportDataSource[]>([])

async function fetchDatasources(): Promise<void> {
  const r = await listDataSources()
  datasources.value = r.data ?? []
}

async function fetchDetail(id: number): Promise<void> {
  loading.value = true
  try {
    const r = await getTemplate(id)
    if (r.data) {
      Object.assign(form, r.data)
    }
  } finally {
    loading.value = false
  }
}

async function doValidate(): Promise<void> {
  loading.value = true
  try {
    await validateTemplate(form)
    ElMessage.success(t('report.template.validateOk'))
  } finally {
    loading.value = false
  }
}

async function save(): Promise<void> {
  loading.value = true
  try {
    // paramSchema 校验：必须是 JSON 数组
    let parsed: ParamDecl[] = []
    if (form.paramSchema && form.paramSchema.trim().length) {
      try {
        parsed = JSON.parse(form.paramSchema)
      } catch (_e) {
        ElMessage.error(t('report.template.paramSchemaInvalid'))
        return
      }
      if (!Array.isArray(parsed)) {
        ElMessage.error(t('report.template.paramSchemaNotArray'))
        return
      }
    }
    if (isNew.value) {
      await addTemplate(form)
    } else {
      await updateTemplate(form)
    }
    ElMessage.success(t('report.common.saveOk'))
    router.push({ name: 'ReportList' })
  } finally {
    loading.value = false
  }
}

function back(): void {
  router.push({ name: 'ReportList' })
}

const paramSchemaTip = computed(() =>
  t('report.template.paramSchemaTip', {
    sample: '[{"name":"minId","type":"number","label":"最小 ID","required":true,"default":1}]'
  })
)

onMounted(async () => {
  await fetchDatasources()
  if (!isNew.value) {
    await fetchDetail(Number(route.params.id))
  }
})
</script>

<template>
  <div
    v-loading="loading"
    class="report-edit app-container"
  >
    <el-form
      :model="form"
      label-width="120px"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item
            :label="t('report.template.colCode')"
            required
          >
            <el-input
              v-model="form.code"
              :disabled="!isNew"
              :placeholder="t('report.template.codePh')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item
            :label="t('report.template.colName')"
            required
          >
            <el-input v-model="form.name" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="t('report.template.colCategory')">
            <el-input v-model="form.category" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('report.template.colDataSource')">
            <el-select
              v-model="form.datasourceId"
              :placeholder="t('report.template.colDataSource')"
            >
              <el-option
                :label="t('report.template.dsMain')"
                :value="0"
              />
              <el-option
                v-for="ds in datasources"
                :key="ds.id"
                :label="`${ds.name} (${ds.code})`"
                :value="ds.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
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
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="t('report.template.colRowLimit')">
            <el-input-number
              v-model="form.rowLimit"
              :min="1"
              :max="10000"
              :step="100"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('report.template.colTimeoutMs')">
            <el-input-number
              v-model="form.timeoutMs"
              :min="100"
              :max="30000"
              :step="500"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('report.template.colPermKey')">
            <el-input
              v-model="form.permKey"
              :placeholder="t('report.template.permKeyPh')"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item
        :label="t('report.template.colSql')"
        required
      >
        <el-input
          v-model="form.sqlText"
          type="textarea"
          :autosize="{ minRows: 8, maxRows: 22 }"
          :placeholder="t('report.template.sqlPh')"
        />
      </el-form-item>

      <el-form-item :label="t('report.template.paramSchemaLabel')">
        <el-input
          v-model="form.paramSchema"
          type="textarea"
          :autosize="{ minRows: 4, maxRows: 12 }"
          :placeholder="paramSchemaTip"
        />
      </el-form-item>

      <el-form-item :label="t('report.template.colRemark')">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          @click="save"
        >
          {{ t('report.common.save') }}
        </el-button>
        <el-button @click="doValidate">
          {{ t('report.template.actionValidate') }}
        </el-button>
        <el-button @click="back">
          {{ t('report.common.cancel') }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
