<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElInput, ElInputNumber, ElSelect, ElOption, ElFormItem, type FormRules } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import {
  listData,
  getData,
  addData,
  updateData,
  delData,
  type DictDataRecord
} from '@/api/system/dict'
import { useCrud } from '@/composables/useCrud'
import { useDict } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import FormDialog from '@/components/FormDialog.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface DictDataQuery {
  pageNum: number
  pageSize: number
  dictType?: string
  dictLabel?: string
  status?: string
}

const route = useRoute()
const router = useRouter()
const dictTypeParam = ref(String(route.params.dictType ?? ''))
const dicts = useDict('sys_normal_disable')

const crud = useCrud<DictDataQuery & Record<string, unknown>, DictDataRecord>({
  defaultQuery: () => ({
    pageNum: 1,
    pageSize: 10,
    dictType: dictTypeParam.value,
    dictLabel: '',
    status: ''
  }),
  defaultForm: () => ({ status: '0', isDefault: 'N', dictSort: 0, dictType: dictTypeParam.value }),
  fetchList: listData,
  rowKey: 'dictCode',
  getOne: (id) => getData(id as number),
  create: addData,
  update: updateData,
  remove: delData
})

const searchFields: SearchField[] = [
  { prop: 'dictLabel', label: '字典标签', type: 'input' },
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

const columns: TableColumn<DictDataRecord>[] = [
  { prop: 'dictCode', label: '编号', width: 90, align: 'center' },
  { prop: 'dictLabel', label: '字典标签', minWidth: 140 },
  { prop: 'dictValue', label: '字典键值', minWidth: 140 },
  { prop: 'dictSort', label: '排序', width: 90, align: 'center' },
  { prop: 'status', label: '状态', dict: 'sys_normal_disable', render: 'tag', width: 90, align: 'center' },
  { prop: 'createTime', label: '创建时间', render: 'date', width: 180 }
]

const rules: FormRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }]
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
        <template #left>
          <el-button
            :icon="Back"
            @click="router.push('/system/dict')"
          >
            返回字典列表
          </el-button>
          <el-tag>当前类型：{{ dictTypeParam }}</el-tag>
        </template>
      </PageToolbar>
      <DataTable
        :data="crud.list.value"
        :columns="columns"
        :loading="crud.loading.value"
        selectable
        row-key="dictCode"
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
        label="数据标签"
        prop="dictLabel"
      >
        <ElInput v-model="crud.form.dictLabel" />
      </ElFormItem>
      <ElFormItem
        label="数据键值"
        prop="dictValue"
      >
        <ElInput v-model="crud.form.dictValue" />
      </ElFormItem>
      <ElFormItem label="样式属性">
        <ElInput
          v-model="crud.form.cssClass"
          placeholder="可选"
        />
      </ElFormItem>
      <ElFormItem label="回显样式">
        <ElSelect
          v-model="crud.form.listClass"
          style="width: 100%"
        >
          <ElOption
            label="默认"
            value="default"
          />
          <ElOption
            label="主要"
            value="primary"
          />
          <ElOption
            label="成功"
            value="success"
          />
          <ElOption
            label="信息"
            value="info"
          />
          <ElOption
            label="警告"
            value="warning"
          />
          <ElOption
            label="危险"
            value="danger"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem
        label="显示排序"
        prop="dictSort"
      >
        <ElInputNumber
          v-model="crud.form.dictSort"
          :min="0"
        />
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
