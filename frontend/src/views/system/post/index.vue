<script setup lang="ts">
import { onMounted } from 'vue'
import { ElInput, ElInputNumber, ElSelect, ElOption, ElFormItem, type FormRules } from 'element-plus'
import {
  listPost,
  getPost,
  addPost,
  updatePost,
  delPost,
  type PostRecord
} from '@/api/system/post'
import { useCrud } from '@/composables/useCrud'
import { useDict } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import FormDialog from '@/components/FormDialog.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface PostQuery {
  pageNum: number
  pageSize: number
  postCode?: string
  postName?: string
  status?: string
}

const dicts = useDict('sys_normal_disable')

const crud = useCrud<PostQuery, PostRecord>({
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, postCode: '', postName: '', status: '' }),
  defaultForm: () => ({ postSort: 0, status: '0', remark: '' }),
  fetchList: listPost,
  rowKey: 'postId',
  getOne: (id) => getPost(id as number),
  create: addPost,
  update: updatePost,
  remove: delPost
})

const searchFields: SearchField[] = [
  { prop: 'postCode', label: '岗位编码', type: 'input' },
  { prop: 'postName', label: '岗位名称', type: 'input' },
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

const columns: TableColumn<PostRecord>[] = [
  { prop: 'postId', label: '编号', width: 90, align: 'center' },
  { prop: 'postCode', label: '岗位编码', minWidth: 120 },
  { prop: 'postName', label: '岗位名称', minWidth: 120 },
  { prop: 'postSort', label: '排序', width: 100, align: 'center' },
  { prop: 'status', label: '状态', dict: 'sys_normal_disable', render: 'tag', width: 100, align: 'center' },
  { prop: 'createTime', label: '创建时间', render: 'date', width: 180 }
]

const rules: FormRules = {
  postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  postSort: [{ required: true, message: '请输入岗位顺序', trigger: 'blur' }]
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
      />
      <DataTable
        :data="crud.list.value"
        :columns="columns"
        :loading="crud.loading.value"
        selectable
        row-key="postId"
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
        label="岗位名称"
        prop="postName"
      >
        <ElInput
          v-model="crud.form.postName"
          placeholder="请输入岗位名称"
        />
      </ElFormItem>
      <ElFormItem
        label="岗位编码"
        prop="postCode"
      >
        <ElInput
          v-model="crud.form.postCode"
          placeholder="请输入岗位编码"
        />
      </ElFormItem>
      <ElFormItem
        label="岗位顺序"
        prop="postSort"
      >
        <ElInputNumber
          v-model="crud.form.postSort"
          :min="0"
        />
      </ElFormItem>
      <ElFormItem
        label="岗位状态"
        prop="status"
      >
        <ElSelect
          v-model="crud.form.status"
          placeholder="请选择"
          style="width: 100%"
        >
          <ElOption
            v-for="item in dicts.sys_normal_disable.value"
            :key="item.dictValue"
            :label="item.dictLabel"
            :value="item.dictValue"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem
        label="备注"
        prop="remark"
      >
        <ElInput
          v-model="crud.form.remark"
          type="textarea"
          :rows="3"
        />
      </ElFormItem>
    </FormDialog>
  </div>
</template>
