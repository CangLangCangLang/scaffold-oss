<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listDoneTasks, type TaskView } from '../api'
import ProcessProgressDialog from '../components/ProcessProgressDialog.vue'

const { t } = useI18n()

const list = ref<TaskView[]>([])
const loading = ref(false)

const progressVisible = ref(false)
const progressTarget = ref<TaskView | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await listDoneTasks()
    list.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

function openProgress(row: TaskView) {
  progressTarget.value = row
  progressVisible.value = true
}

onMounted(fetchList)
</script>

<template>
  <div class="scaffold-page">
    <div class="scaffold-card">
      <el-table
        v-loading="loading"
        :data="list"
        stripe
        border
      >
        <el-table-column
          prop="name"
          :label="t('workflow.common.taskName')"
          min-width="160"
        />
        <el-table-column
          prop="processInstanceId"
          :label="t('workflow.common.processInstance')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="processDefinitionId"
          :label="t('workflow.common.processDefinition')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="createTime"
          :label="t('common.arrivedAt')"
          width="180"
        />
        <el-table-column
          prop="endTime"
          :label="t('common.finishedAt')"
          width="180"
        />
        <el-table-column
          :label="t('common.operation')"
          width="120"
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
          </template>
        </el-table-column>
      </el-table>
    </div>

    <ProcessProgressDialog
      v-model="progressVisible"
      :process-instance-id="progressTarget?.processInstanceId"
      :title="progressTarget?.name"
    />
  </div>
</template>
