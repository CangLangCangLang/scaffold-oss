<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElInput,
  ElSelect,
  ElOption,
  ElFormItem,
  ElMessage,
  ElMessageBox,
  ElRadioGroup,
  ElRadio,
  type FormRules
} from 'element-plus'
import {
  listJob,
  getJob,
  addJob,
  updateJob,
  delJob,
  changeJobStatus,
  runJob,
  type JobRecord
} from '@/api/monitor/job'
import { useCrud } from '@/composables/useCrud'
import { useDict } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import FormDialog from '@/components/FormDialog.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface JobQuery {
  pageNum: number
  pageSize: number
  jobName?: string
  jobGroup?: string
  status?: string
}

const router = useRouter()
const dicts = useDict('sys_normal_disable', 'sys_job_group')

const crud = useCrud<JobQuery & Record<string, unknown>, JobRecord>({
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, jobName: '', jobGroup: '', status: '' }),
  defaultForm: () => ({ status: '0', misfirePolicy: '1', concurrent: '1', jobGroup: 'DEFAULT' }),
  fetchList: listJob,
  rowKey: 'jobId',
  getOne: (id) => getJob(id as number),
  create: addJob,
  update: updateJob,
  remove: delJob
})

async function handleStatusToggle(row: JobRecord, value: string) {
  try {
    await changeJobStatus(row.jobId!, value)
    row.status = value
    ElMessage.success('状态已更新')
  } catch {
    row.status = row.status === '0' ? '1' : '0'
  }
}

async function handleRun(row: JobRecord) {
  try {
    await ElMessageBox.confirm(`确认立即执行任务：${row.jobName}？`, '提示', { type: 'warning' })
    await runJob(row.jobId!, row.jobGroup!)
    ElMessage.success('已执行')
  } catch {
    // cancel
  }
}

const searchFields: SearchField[] = [
  { prop: 'jobName', label: '任务名称', type: 'input' },
  {
    prop: 'jobGroup',
    label: '任务分组',
    type: 'select',
    options: [
      { value: 'DEFAULT', label: '默认' },
      { value: 'SYSTEM', label: '系统' }
    ]
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { value: '0', label: '正常' },
      { value: '1', label: '暂停' }
    ]
  }
]

const columns: TableColumn<JobRecord>[] = [
  { prop: 'jobId', label: '编号', width: 80, align: 'center' },
  { prop: 'jobName', label: '任务名称', minWidth: 140 },
  { prop: 'jobGroup', label: '任务分组', dict: 'sys_job_group', render: 'tag', width: 100, align: 'center' },
  { prop: 'invokeTarget', label: '调用目标', minWidth: 200, showOverflowTooltip: true },
  { prop: 'cronExpression', label: 'Cron 表达式', width: 160 },
  { prop: 'status', label: '状态', render: 'switch', width: 100, align: 'center', onSwitchChange: handleStatusToggle }
]

const rules: FormRules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  invokeTarget: [{ required: true, message: '请输入调用目标', trigger: 'blur' }],
  cronExpression: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }]
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
        @add="crud.handleAdd"
        @delete="crud.handleDelete()"
        @refresh="crud.fetchList"
      >
        <template #right>
          <el-button
            type="warning"
            plain
            @click="router.push('/monitor/job-log')"
          >
            调度日志
          </el-button>
        </template>
      </PageToolbar>
      <DataTable
        :data="crud.list.value"
        :columns="columns"
        :loading="crud.loading.value"
        selectable
        row-key="jobId"
        @selection-change="crud.handleSelectionChange"
      >
        <template #action>
          <el-table-column
            label="操作"
            width="220"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                @click="crud.handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                type="success"
                link
                @click="handleRun(row)"
              >
                执行一次
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
    <FormDialog
      v-model="crud.dialogVisible.value"
      :title="crud.dialogTitle.value"
      :model="crud.form"
      :rules="rules"
      width="720px"
      @submit="crud.handleSubmit"
    >
      <ElFormItem
        label="任务名称"
        prop="jobName"
      >
        <ElInput v-model="crud.form.jobName" />
      </ElFormItem>
      <ElFormItem label="任务分组">
        <ElSelect
          v-model="crud.form.jobGroup"
          style="width: 100%"
        >
          <ElOption
            v-for="d in dicts.sys_job_group.value"
            :key="d.dictValue"
            :label="d.dictLabel"
            :value="d.dictValue"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem
        label="调用方法"
        prop="invokeTarget"
      >
        <ElInput
          v-model="crud.form.invokeTarget"
          placeholder="如 ryTask.ryParams('ry')"
        />
      </ElFormItem>
      <ElFormItem
        label="Cron 表达式"
        prop="cronExpression"
      >
        <ElInput
          v-model="crud.form.cronExpression"
          placeholder="如 0 0/30 * * * ?"
        />
      </ElFormItem>
      <ElFormItem label="执行策略">
        <ElRadioGroup v-model="crud.form.misfirePolicy">
          <ElRadio label="1">
            立即执行
          </ElRadio>
          <ElRadio label="2">
            执行一次
          </ElRadio>
          <ElRadio label="3">
            放弃执行
          </ElRadio>
        </ElRadioGroup>
      </ElFormItem>
      <ElFormItem label="并发执行">
        <ElRadioGroup v-model="crud.form.concurrent">
          <ElRadio label="0">
            允许
          </ElRadio>
          <ElRadio label="1">
            禁止
          </ElRadio>
        </ElRadioGroup>
      </ElFormItem>
      <ElFormItem label="状态">
        <ElSelect
          v-model="crud.form.status"
          style="width: 100%"
        >
          <ElOption
            v-for="d in dicts.sys_normal_disable.value"
            :key="d.dictValue"
            :label="d.dictLabel"
            :value="d.dictValue"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="备注">
        <ElInput
          v-model="crud.form.remark"
          type="textarea"
          :rows="2"
        />
      </ElFormItem>
    </FormDialog>
  </div>
</template>
