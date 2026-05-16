<script setup lang="ts">
import { onMounted } from 'vue'
import { ElInput, ElSelect, ElOption, ElFormItem, type FormRules } from 'element-plus'
import {
  listNotice,
  getNotice,
  addNotice,
  updateNotice,
  delNotice,
  type NoticeRecord
} from '@/api/system/notice'
import { useCrud } from '@/composables/useCrud'
import { useDict } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import FormDialog from '@/components/FormDialog.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface NoticeQuery {
  pageNum: number
  pageSize: number
  noticeTitle?: string
  noticeType?: string
  createBy?: string
}

const dicts = useDict('sys_notice_status', 'sys_notice_type')

const crud = useCrud<NoticeQuery & Record<string, unknown>, NoticeRecord>({
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, noticeTitle: '', noticeType: '', createBy: '' }),
  defaultForm: () => ({ status: '0', noticeType: '1' }),
  fetchList: listNotice,
  rowKey: 'noticeId',
  getOne: (id) => getNotice(id as number),
  create: addNotice,
  update: updateNotice,
  remove: delNotice
})

const searchFields: SearchField[] = [
  { prop: 'noticeTitle', label: '公告标题', type: 'input' },
  {
    prop: 'noticeType',
    label: '公告类型',
    type: 'select',
    options: [
      { value: '1', label: '通知' },
      { value: '2', label: '公告' }
    ]
  }
]

const columns: TableColumn<NoticeRecord>[] = [
  { prop: 'noticeId', label: '编号', width: 80, align: 'center' },
  { prop: 'noticeTitle', label: '公告标题', minWidth: 180 },
  { prop: 'noticeType', label: '公告类型', dict: 'sys_notice_type', render: 'tag', width: 100, align: 'center' },
  { prop: 'status', label: '状态', dict: 'sys_notice_status', render: 'tag', width: 100, align: 'center' },
  { prop: 'createTime', label: '创建时间', render: 'date', width: 180 }
]

const rules: FormRules = {
  noticeTitle: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择公告类型', trigger: 'change' }]
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
        row-key="noticeId"
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
      width="720px"
      @submit="crud.handleSubmit"
    >
      <ElFormItem
        label="公告标题"
        prop="noticeTitle"
      >
        <ElInput v-model="crud.form.noticeTitle" />
      </ElFormItem>
      <ElFormItem
        label="公告类型"
        prop="noticeType"
      >
        <ElSelect
          v-model="crud.form.noticeType"
          style="width: 100%"
        >
          <ElOption
            v-for="d in dicts.sys_notice_type.value"
            :key="d.dictValue"
            :label="d.dictLabel"
            :value="d.dictValue"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="状态">
        <ElSelect
          v-model="crud.form.status"
          style="width: 100%"
        >
          <ElOption
            v-for="d in dicts.sys_notice_status.value"
            :key="d.dictValue"
            :label="d.dictLabel"
            :value="d.dictValue"
          />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="公告内容">
        <ElInput
          v-model="crud.form.noticeContent"
          type="textarea"
          :rows="6"
        />
      </ElFormItem>
    </FormDialog>
  </div>
</template>
