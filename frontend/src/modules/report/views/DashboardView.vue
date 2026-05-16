<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  addDashboard,
  getDashboard,
  listTemplates,
  runReport,
  updateDashboard,
  type RunResult,
  type SysReportDashboard,
  type SysReportDashboardCard,
  type SysReportTemplate
} from '../api'

const EchartsCard = defineAsyncComponent(() => import('../components/EchartsCard.vue'))

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const isNew = computed(() => route.params.id === 'new')
const isEdit = computed(() => route.params.mode === 'edit')

const dash = reactive<SysReportDashboard>({
  code: '',
  name: '',
  category: '',
  layoutJson: '{}',
  permKey: '',
  status: '0'
})

const cards = ref<SysReportDashboardCard[]>([])
/** 卡片对应运行结果，按 cards 数组下标对齐 */
const cardResults = ref<(RunResult | null)[]>([])
const allTemplates = ref<SysReportTemplate[]>([])

async function loadTemplates(): Promise<void> {
  const r = await listTemplates({ pageNum: 1, pageSize: 200, status: '0' })
  allTemplates.value = r.rows ?? []
}

async function load(): Promise<void> {
  loading.value = true
  try {
    if (!isNew.value) {
      const r = await getDashboard(Number(route.params.id))
      Object.assign(dash, r.data!.dashboard)
      cards.value = r.data!.cards ?? []
      cardResults.value = cards.value.map(() => null)
      if (!isEdit.value) {
        await refreshAllCards()
      }
    } else {
      cards.value = []
      cardResults.value = []
    }
  } finally {
    loading.value = false
  }
}

function addCard(): void {
  if (!allTemplates.value.length) {
    ElMessage.warning(t('report.dashboard.noActiveTemplate'))
    return
  }
  cards.value.push({
    templateId: allTemplates.value[0].id!,
    title: t('report.dashboard.untitledCard'),
    chartType: 'table',
    posW: 12,
    posH: 6,
    orderNum: cards.value.length
  })
  cardResults.value.push(null)
}

function removeCard(idx: number): void {
  cards.value.splice(idx, 1)
  cardResults.value.splice(idx, 1)
}

async function save(): Promise<void> {
  loading.value = true
  try {
    const body = { dashboard: { ...dash }, cards: cards.value }
    if (isNew.value) {
      await addDashboard(body)
    } else {
      await updateDashboard(body)
    }
    ElMessage.success(t('report.common.saveOk'))
    router.push({ name: 'DashboardList' })
  } finally {
    loading.value = false
  }
}

async function refreshOne(idx: number): Promise<void> {
  const c = cards.value[idx]
  if (!c?.templateId) return
  let p: Record<string, unknown> = {}
  if (c.paramJson) {
    try {
      p = JSON.parse(c.paramJson) as Record<string, unknown>
    } catch {
      p = {}
    }
  }
  try {
    const r = await runReport({ templateId: c.templateId, params: p })
    cardResults.value[idx] = r.data ?? null
  } catch {
    cardResults.value[idx] = null
  }
}

async function refreshAllCards(): Promise<void> {
  await Promise.all(cards.value.map((_c, i) => refreshOne(i)))
}

function back(): void {
  router.push({ name: 'DashboardList' })
}

async function confirmRefreshAll(): Promise<void> {
  await ElMessageBox.confirm(t('report.dashboard.confirmRefreshAll'), t('report.common.confirm'), {
    type: 'info'
  })
  await refreshAllCards()
  ElMessage.success(t('report.dashboard.refreshOk'))
}

onMounted(async () => {
  await loadTemplates()
  await load()
})
</script>

<template>
  <div
    v-loading="loading"
    class="dashboard-view app-container"
  >
    <el-card shadow="never">
      <template #header>
        <span>{{ isNew ? t('report.dashboard.add') : dash.name }}</span>
        <span style="float: right">
          <el-button
            v-if="!isEdit"
            type="primary"
            @click="confirmRefreshAll"
          >
            {{ t('report.dashboard.refreshAll') }}
          </el-button>
          <el-button @click="back">{{ t('report.common.back') }}</el-button>
        </span>
      </template>

      <el-form
        v-if="isEdit"
        :model="dash"
        :inline="true"
        label-width="100px"
      >
        <el-form-item
          :label="t('report.dashboard.colCode')"
          required
        >
          <el-input
            v-model="dash.code"
            :disabled="!isNew"
            :placeholder="t('report.dashboard.codePh')"
          />
        </el-form-item>
        <el-form-item
          :label="t('report.dashboard.colName')"
          required
        >
          <el-input v-model="dash.name" />
        </el-form-item>
        <el-form-item :label="t('report.dashboard.colCategory')">
          <el-input v-model="dash.category" />
        </el-form-item>
        <el-form-item :label="t('report.template.colPermKey')">
          <el-input
            v-model="dash.permKey"
            :placeholder="t('report.template.permKeyPh')"
          />
        </el-form-item>
        <el-form-item :label="t('report.template.colStatus')">
          <el-select
            v-model="dash.status"
            style="width: 120px"
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
      </el-form>

      <div
        v-if="isEdit"
        style="margin-bottom: 12px"
      >
        <el-button
          type="success"
          size="small"
          @click="addCard"
        >
          {{ t('report.dashboard.addCard') }}
        </el-button>
        <el-button
          type="primary"
          size="small"
          @click="save"
        >
          {{ t('report.common.save') }}
        </el-button>
      </div>
    </el-card>

    <!-- 卡片网格 -->
    <el-row
      :gutter="16"
      style="margin-top: 16px"
    >
      <el-col
        v-for="(c, idx) in cards"
        :key="idx"
        :span="Math.min(24, c.posW || 12)"
      >
        <el-card
          shadow="hover"
          class="card-item"
        >
          <template #header>
            <span>{{ c.title || t('report.dashboard.untitledCard') }}</span>
            <span
              v-if="isEdit"
              style="float: right"
            >
              <el-button
                size="small"
                type="danger"
                link
                @click="removeCard(idx)"
              >
                {{ t('report.common.remove') }}
              </el-button>
            </span>
            <span
              v-else
              style="float: right"
            >
              <el-button
                size="small"
                type="primary"
                link
                @click="refreshOne(idx)"
              >
                {{ t('report.dashboard.refresh') }}
              </el-button>
            </span>
          </template>

          <!-- 编辑：卡片元信息编辑 -->
          <el-form
            v-if="isEdit"
            :model="c"
            label-width="90px"
          >
            <el-form-item :label="t('report.dashboard.cardTitle')">
              <el-input v-model="c.title" />
            </el-form-item>
            <el-form-item :label="t('report.dashboard.cardTemplate')">
              <el-select
                v-model="c.templateId"
                filterable
              >
                <el-option
                  v-for="tpl in allTemplates"
                  :key="tpl.id"
                  :label="`${tpl.name} (${tpl.code})`"
                  :value="tpl.id as number"
                />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('report.dashboard.cardChart')">
              <el-select
                v-model="c.chartType"
                style="width: 140px"
              >
                <el-option
                  label="table"
                  value="table"
                />
                <el-option
                  label="line"
                  value="line"
                />
                <el-option
                  label="bar"
                  value="bar"
                />
                <el-option
                  label="pie"
                  value="pie"
                />
                <el-option
                  label="number"
                  value="number"
                />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('report.dashboard.cardLayout')">
              <el-input-number
                v-model="c.posW"
                :min="6"
                :max="24"
                :step="2"
                style="margin-right: 8px"
              />
              <el-input-number
                v-model="c.posH"
                :min="3"
                :max="20"
              />
            </el-form-item>
            <el-form-item :label="t('report.dashboard.cardConfig')">
              <el-input
                v-model="c.configJson"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                :placeholder="t('report.dashboard.cardConfigPh')"
              />
            </el-form-item>
            <el-form-item :label="t('report.dashboard.cardParam')">
              <el-input
                v-model="c.paramJson"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                :placeholder="t('report.dashboard.cardParamPh')"
              />
            </el-form-item>
          </el-form>

          <!-- 查看：渲染图表或表格 -->
          <template v-else>
            <div
              v-if="!cardResults[idx]"
              class="muted"
            >
              {{ t('report.dashboard.notLoaded') }}
            </div>
            <template v-else-if="c.chartType === 'table'">
              <el-table
                :data="cardResults[idx]!.rows.map((r) => Object.fromEntries(cardResults[idx]!.columns.map((col, i) => [col, r[i]])))"
                border
                stripe
                size="small"
              >
                <el-table-column
                  v-for="col in cardResults[idx]!.columns"
                  :key="col"
                  :prop="col"
                  :label="col"
                />
              </el-table>
            </template>
            <template v-else-if="c.chartType === 'number'">
              <div class="kpi">
                {{ cardResults[idx]?.rows?.[0]?.[0] !== undefined ? cardResults[idx]!.rows[0][0] : '-' }}
              </div>
            </template>
            <template v-else>
              <EchartsCard
                :result="cardResults[idx]"
                :chart-type="c.chartType"
                :config-json="c.configJson"
              />
            </template>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.card-item { margin-bottom: 16px }
.muted { color: #909399; font-size: 13px; padding: 8px }
.kpi { font-size: 36px; font-weight: 600; text-align: center; padding: 24px 0 }
</style>
