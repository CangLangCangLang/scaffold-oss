<script setup lang="ts">
import { onMounted } from 'vue'
import { ElInput, ElSelect, ElOption, ElFormItem, ElMessage, type FormRules } from 'element-plus'
import {
  listConfig,
  getConfig,
  addConfig,
  updateConfig,
  delConfig,
  refreshConfigCache,
  type ConfigRecord
} from '@/api/system/config'
import { useCrud } from '@/composables/useCrud'
import { useDict } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import FormDialog from '@/components/FormDialog.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface ConfigQuery {
  pageNum: number
  pageSize: number
  configName?: string
  configKey?: string
  configType?: string
}

const dicts = useDict('sys_yes_no')

const crud = useCrud<ConfigQuery & Record<string, unknown>, ConfigRecord>({
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, configName: '', configKey: '', configType: '' }),
  defaultForm: () => ({ configType: 'Y' }),
  fetchList: listConfig,
  rowKey: 'configId',
  getOne: (id) => getConfig(id as number),
  create: addConfig,
  update: updateConfig,
  remove: delConfig
})

const searchFields: SearchField[] = [
  { prop: 'configName', label: '参数名称', type: 'input' },
  { prop: 'configKey', label: '参数键名', type: 'input' },
  {
    prop: 'configType',
    label: '系统内置',
    type: 'select',
    options: [
      { value: 'Y', label: '是' },
      { value: 'N', label: '否' }
    ]
  }
]

const columns: TableColumn<ConfigRecord>[] = [
  { prop: 'configId', label: '编号', width: 90, align: 'center' },
  { prop: 'configName', label: '参数名称', minWidth: 140 },
  { prop: 'configKey', label: '参数键名', minWidth: 160 },
  { prop: 'configValue', label: '参数键值', minWidth: 160, showOverflowTooltip: true },
  { prop: 'configType', label: '系统内置', dict: 'sys_yes_no', render: 'tag', width: 100, align: 'center' },
  { prop: 'createTime', label: '创建时间', render: 'date', width: 180 }
]

const rules: FormRules = {
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入参数键值', trigger: 'blur' }]
}

async function handleRefreshCache() {
  await refreshConfigCache()
  ElMessage.success('参数缓存已刷新')
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
            @click="handleRefreshCache"
          >
            刷新缓存
          </el-button>
        </template>
      </PageToolbar>
      <DataTable
        :data="crud.list.value"
        :columns="columns"
        :loading="crud.loading.value"
        selectable
        row-key="configId"
        @selection-change="crud.handleSelectionChange"
      >
        <template #action>
          <el-table-column
            label="操作"
            width="160"
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
      @submit="crud.handleSubmit"
    >
      <ElFormItem
        label="参数名称"
        prop="configName"
      >
        <ElInput v-model="crud.form.configName" />
      </ElFormItem>
      <ElFormItem
        label="参数键名"
        prop="configKey"
      >
        <ElInput v-model="crud.form.configKey" />
      </ElFormItem>
      <ElFormItem
        label="参数键值"
        prop="configValue"
      >
        <ElInput v-model="crud.form.configValue" />
      </ElFormItem>
      <ElFormItem label="系统内置">
        <ElSelect
          v-model="crud.form.configType"
          style="width: 100%"
        >
          <ElOption
            v-for="d in dicts.sys_yes_no.value"
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
