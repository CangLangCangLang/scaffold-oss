<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElInput, ElSelect, ElOption, ElFormItem, ElMessage, type FormRules } from 'element-plus'
import {
  listType,
  getType,
  addType,
  updateType,
  delType,
  refreshDictCache,
  type DictTypeRecord
} from '@/api/system/dict'
import { useCrud } from '@/composables/useCrud'
import { useDict, clearDictCache } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import FormDialog from '@/components/FormDialog.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface DictTypeQuery {
  pageNum: number
  pageSize: number
  dictName?: string
  dictType?: string
  status?: string
}

const router = useRouter()
const dicts = useDict('sys_normal_disable')

const crud = useCrud<DictTypeQuery & Record<string, unknown>, DictTypeRecord>({
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, dictName: '', dictType: '', status: '' }),
  defaultForm: () => ({ status: '0' }),
  fetchList: listType,
  rowKey: 'dictId',
  getOne: (id) => getType(id as number),
  create: addType,
  update: updateType,
  remove: delType
})

const searchFields: SearchField[] = [
  { prop: 'dictName', label: '字典名称', type: 'input' },
  { prop: 'dictType', label: '字典类型', type: 'input' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { value: '0', label: '正常' },
      { value: '1', label: '停用' }
    ]
  }
]

const columns: TableColumn<DictTypeRecord>[] = [
  { prop: 'dictId', label: '编号', width: 90, align: 'center' },
  { prop: 'dictName', label: '字典名称', minWidth: 140 },
  { prop: 'dictType', label: '字典类型', minWidth: 160 },
  { prop: 'status', label: '状态', dict: 'sys_normal_disable', render: 'tag', width: 90, align: 'center' },
  { prop: 'remark', label: '备注', minWidth: 160, showOverflowTooltip: true },
  { prop: 'createTime', label: '创建时间', render: 'date', width: 180 }
]

const rules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

async function handleRefreshCache() {
  await refreshDictCache()
  clearDictCache()
  ElMessage.success('字典缓存已刷新')
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
        row-key="dictId"
        @selection-change="crud.handleSelectionChange"
      >
        <template #action>
          <el-table-column
            label="操作"
            width="200"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                @click="router.push(`/system/dict-data/${row.dictType}`)"
              >
                数据
              </el-button>
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
        label="字典名称"
        prop="dictName"
      >
        <ElInput v-model="crud.form.dictName" />
      </ElFormItem>
      <ElFormItem
        label="字典类型"
        prop="dictType"
      >
        <ElInput v-model="crud.form.dictType" />
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
