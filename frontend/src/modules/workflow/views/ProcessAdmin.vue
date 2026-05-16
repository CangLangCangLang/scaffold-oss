<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import {
  cancelInstance,
  listProcessDefinitions,
  searchInstances,
  type ProcessDefinitionView,
  type ProcessInstanceView
} from '../api'
import ProcessProgressDialog from '../components/ProcessProgressDialog.vue'

const { t } = useI18n()

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles.includes('admin'))

const definitions = ref<ProcessDefinitionView[]>([])

const filter = reactive({
  processDefinitionKey: '' as string | undefined,
  businessKey: '' as string | undefined,
  startUserId: '' as string | undefined,
  status: 'all' as 'all' | 'running' | 'finished'
})

const list = ref<ProcessInstanceView[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)

const progressVisible = ref(false)
const progressTarget = ref<ProcessInstanceView | null>(null)

async function fetchDefinitions() {
  try {
    const res = await listProcessDefinitions()
    definitions.value = res.data ?? []
  } catch (e) {
    ElMessage.warning(t('workflow.admin.definitionsLoadFailed', { msg: (e as Error).message }))
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await searchInstances({
      processDefinitionKey: filter.processDefinitionKey || undefined,
      businessKey: filter.businessKey || undefined,
      startUserId: filter.startUserId || undefined,
      status: filter.status,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = res.rows ?? []
    total.value = res.total ?? 0
  } catch (e) {
    ElMessage.error(t('workflow.admin.searchFailed', { msg: (e as Error).message }))
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  void fetchList()
}

function onReset() {
  filter.processDefinitionKey = ''
  filter.businessKey = ''
  filter.startUserId = ''
  filter.status = 'all'
  pageNum.value = 1
  void fetchList()
}

function openProgress(row: ProcessInstanceView) {
  progressTarget.value = row
  progressVisible.value = true
}

async function handleCancel(row: ProcessInstanceView) {
  if (row.ended) {
    ElMessage.warning(t('workflow.admin.cancelEnded'))
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      t('workflow.admin.cancelPrompt', { id: row.id }),
      t('workflow.admin.cancelTitle'),
      {
        confirmButtonText: t('workflow.admin.cancelBtn'),
        cancelButtonText: t('workflow.admin.backBtn'),
        type: 'warning'
      }
    )
    await cancelInstance(row.id, value || 'admin_cancel')
    ElMessage.success(t('workflow.admin.cancelOk'))
    await fetchList()
  } catch {
    /* user cancel */
  }
}

onMounted(() => {
  void fetchDefinitions()
  void fetchList()
})
</script>

<template>
  <div class="scaffold-page">
    <div class="scaffold-card">
      <el-alert
        v-if="!isAdmin"
        type="info"
        :closable="false"
        show-icon
        :title="t('workflow.admin.alertNonAdmin')"
        class="role-tip"
      />
      <el-form
        inline
        @submit.prevent="onSearch"
      >
        <el-form-item :label="t('workflow.common.process')">
          <el-select
            v-model="filter.processDefinitionKey"
            :placeholder="t('common.all')"
            filterable
            clearable
            style="width: 220px"
          >
            <el-option
              v-for="d in definitions"
              :key="d.key"
              :label="`${d.name || d.key} (v${d.version})`"
              :value="d.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('workflow.common.businessKey')">
          <el-input
            v-model="filter.businessKey"
            :placeholder="t('workflow.common.businessKeyExact')"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item
          v-if="isAdmin"
          :label="t('workflow.admin.startUserId')"
        >
          <el-input
            v-model="filter.startUserId"
            placeholder="userId"
            clearable
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select
            v-model="filter.status"
            style="width: 140px"
          >
            <el-option
              :label="t('workflow.common.stateAll')"
              value="all"
            />
            <el-option
              :label="t('workflow.common.stateRunning')"
              value="running"
            />
            <el-option
              :label="t('workflow.common.stateFinished')"
              value="finished"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="onSearch"
          >
            {{ t('common.search') }}
          </el-button>
          <el-button @click="onReset">
            {{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        border
      >
        <el-table-column
          prop="id"
          :label="t('workflow.admin.instanceId')"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          prop="processDefinitionKey"
          :label="t('workflow.common.process')"
          min-width="160"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.processDefinitionName || row.processDefinitionKey || row.processDefinitionId }}
          </template>
        </el-table-column>
        <el-table-column
          prop="businessKey"
          :label="t('workflow.common.businessKey')"
          width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="startUserId"
          :label="t('workflow.admin.startUser')"
          width="100"
        />
        <el-table-column
          :label="t('common.status')"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.ended"
              type="info"
              size="small"
              effect="plain"
            >
              {{ t('workflow.common.stateFinished') }}
            </el-tag>
            <el-tag
              v-else
              type="primary"
              size="small"
              effect="plain"
            >
              {{ t('workflow.common.stateRunning') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="startTime"
          :label="t('common.startTime')"
          width="170"
        />
        <el-table-column
          prop="endTime"
          :label="t('common.endTime')"
          width="170"
        />
        <el-table-column
          :label="t('common.operation')"
          width="200"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="info"
              link
              @click="openProgress(row)"
            >
              {{ t('workflow.common.progress') }}
            </el-button>
            <el-button
              v-if="!row.ended"
              type="danger"
              link
              @click="handleCancel(row)"
            >
              {{ t('workflow.admin.cancelAction') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </div>

    <ProcessProgressDialog
      v-model="progressVisible"
      :process-instance-id="progressTarget?.id"
      :title="progressTarget?.processDefinitionKey"
    />
  </div>
</template>

<style scoped lang="scss">
.role-tip {
  margin-bottom: 12px;
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
