<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { saveAs } from 'file-saver'
import request from '@/utils/request'
import {
  exportReportUrl,
  getTemplate,
  runReport,
  type ParamDecl,
  type RunResult,
  type SysReportTemplate
} from '../api'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const exporting = ref(false)
const tpl = ref<SysReportTemplate | null>(null)
// 报表参数是动态键值（按模板 paramSchema 生成），用 any 让 v-model 直接接 element 输入
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const params = reactive<Record<string, any>>({})
const decls = ref<ParamDecl[]>([])
const result = ref<RunResult | null>(null)

const templateId = computed(() => Number(route.params.id))

async function fetch(): Promise<void> {
  const r = await getTemplate(templateId.value)
  tpl.value = r.data!
  parseDecls()
}

function parseDecls(): void {
  decls.value = []
  const raw = tpl.value?.paramSchema?.trim()
  if (!raw) return
  try {
    const parsed = JSON.parse(raw) as ParamDecl[]
    if (!Array.isArray(parsed)) return
    decls.value = parsed
    for (const d of parsed) {
      if (params[d.name] === undefined && d.default !== undefined) {
        params[d.name] = d.default
      }
    }
  } catch {
    /* ignore — 模板的 paramSchema 不是合法 JSON 时，让用户手动按名传值 */
  }
}

async function run(): Promise<void> {
  loading.value = true
  try {
    const r = await runReport({ templateId: templateId.value, params })
    result.value = r.data!
    if (result.value?.truncated) {
      ElMessage.warning(t('report.run.truncated', { rows: result.value.rowCount }))
    } else {
      ElMessage.success(
        t('report.run.runOk', { rows: result.value?.rowCount ?? 0, ms: result.value?.costMs ?? 0 })
      )
    }
  } finally {
    loading.value = false
  }
}

async function exportFile(format: 'csv' | 'xlsx'): Promise<void> {
  exporting.value = true
  try {
    const blob = (await request.post(
      `${exportReportUrl}?format=${format}`,
      { templateId: templateId.value, params },
      { responseType: 'blob' }
    )) as unknown as Blob
    const fname = `${tpl.value?.code ?? 'report'}-${Date.now()}.${format}`
    saveAs(new Blob([blob]), fname)
  } finally {
    exporting.value = false
  }
}

function back(): void {
  router.push({ name: 'ReportList' })
}

onMounted(fetch)
</script>

<template>
  <div
    v-loading="loading"
    class="report-run app-container"
  >
    <el-card
      v-if="tpl"
      shadow="never"
      class="run-meta"
    >
      <template #header>
        <span>{{ tpl.name }} <span class="muted">({{ tpl.code }})</span></span>
        <el-button
          text
          type="primary"
          @click="back"
        >
          {{ t('report.run.back') }}
        </el-button>
      </template>
      <div class="muted">
        {{ tpl.remark }}
      </div>
    </el-card>

    <el-card
      shadow="never"
      style="margin-top: 16px"
    >
      <template #header>
        <span>{{ t('report.run.params') }}</span>
      </template>
      <el-form
        v-if="decls.length"
        :inline="true"
        :model="params"
      >
        <el-form-item
          v-for="d in decls"
          :key="d.name"
          :label="d.label || d.name"
        >
          <el-date-picker
            v-if="d.type === 'date'"
            v-model="params[d.name]"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 200px"
          />
          <el-date-picker
            v-else-if="d.type === 'datetime'"
            v-model="params[d.name]"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 220px"
          />
          <el-input-number
            v-else-if="d.type === 'number'"
            v-model="params[d.name]"
          />
          <el-switch
            v-else-if="d.type === 'boolean'"
            v-model="params[d.name]"
          />
          <el-select
            v-else-if="d.options && d.options.length"
            v-model="params[d.name]"
            style="width: 200px"
          >
            <el-option
              v-for="o in d.options"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
          <el-input
            v-else
            v-model="params[d.name]"
          />
        </el-form-item>
      </el-form>
      <div
        v-else
        class="muted"
      >
        {{ t('report.run.noParams') }}
      </div>

      <div style="margin-top: 8px">
        <el-button
          v-hasPermi="['report:template:run']"
          type="primary"
          :loading="loading"
          @click="run"
        >
          {{ t('report.run.run') }}
        </el-button>
        <el-button
          v-hasPermi="['report:template:export']"
          :loading="exporting"
          @click="exportFile('csv')"
        >
          {{ t('report.run.exportCsv') }}
        </el-button>
        <el-button
          v-hasPermi="['report:template:export']"
          :loading="exporting"
          @click="exportFile('xlsx')"
        >
          {{ t('report.run.exportXlsx') }}
        </el-button>
      </div>
    </el-card>

    <el-card
      v-if="result"
      shadow="never"
      style="margin-top: 16px"
    >
      <template #header>
        <span>
          {{ t('report.run.resultMeta', { rows: result.rowCount, ms: result.costMs }) }}
          <el-tag
            v-if="result.truncated"
            type="warning"
            size="small"
            style="margin-left: 8px"
          >
            {{ t('report.run.truncatedTag') }}
          </el-tag>
        </span>
      </template>
      <el-table
        :data="result.rows.map((r) => Object.fromEntries(result!.columns.map((c, i) => [c, r[i]])))"
        border
        stripe
      >
        <el-table-column
          v-for="(c, i) in result.columns"
          :key="c"
          :prop="c"
          :label="c"
        >
          <template #default="{ row }">
            <span>{{ row[c] === null || row[c] === undefined ? '' : String(row[c]) }}</span>
            <span
              v-if="result.columnTypes[i]"
              class="col-type"
            > ({{ result.columnTypes[i] }})</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.muted { color: #909399; font-size: 13px }
.col-type { display: none }
.run-meta :deep(.el-card__header) { display: flex; justify-content: space-between; align-items: center }
</style>
