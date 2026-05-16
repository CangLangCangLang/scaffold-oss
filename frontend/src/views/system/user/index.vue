<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, ElInput, ElSelect, ElOption, ElFormItem, type FormRules } from 'element-plus'
import {
  listUser,
  getUser,
  addUser,
  updateUser,
  delUser,
  resetUserPwd,
  changeUserStatus,
  deptTreeSelect,
  type UserRecord,
  type UserQuery
} from '@/api/system/user'
import { useCrud } from '@/composables/useCrud'
import { useDict } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import FormDialog from '@/components/FormDialog.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

interface DeptNode {
  id: number
  label: string
  children?: DeptNode[]
}

const dicts = useDict('sys_normal_disable', 'sys_user_sex')
const deptTree = ref<DeptNode[]>([])

const crud = useCrud<UserQuery & Record<string, unknown>, UserRecord>({
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, userName: '', phonenumber: '', status: '', deptId: undefined }),
  defaultForm: () => ({ status: '0', sex: '0', password: '', userName: '', nickName: '', email: '', phonenumber: '' }),
  fetchList: listUser,
  rowKey: 'userId',
  getOne: (id) => getUser(id),
  create: addUser,
  update: updateUser,
  remove: delUser
})

const searchFields: SearchField[] = [
  { prop: 'userName', label: '用户名称', type: 'input' },
  { prop: 'phonenumber', label: '手机号', type: 'input' },
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

async function handleStatusToggle(row: UserRecord, value: string) {
  try {
    await changeUserStatus(row.userId!, value)
    row.status = value
    ElMessage.success('状态已更新')
  } catch {
    row.status = row.status === '0' ? '1' : '0'
  }
}

const columns: TableColumn<UserRecord>[] = [
  { prop: 'userId', label: '编号', width: 80, align: 'center' },
  { prop: 'userName', label: '用户名称', minWidth: 110 },
  { prop: 'nickName', label: '用户昵称', minWidth: 110 },
  { prop: 'phonenumber', label: '手机号', width: 130 },
  { prop: 'sex', label: '性别', dict: 'sys_user_sex', render: 'tag', width: 90, align: 'center' },
  {
    prop: 'status',
    label: '状态',
    render: 'switch',
    width: 100,
    align: 'center',
    onSwitchChange: handleStatusToggle
  },
  { prop: 'createTime', label: '创建时间', render: 'date', width: 180 }
]

const rules: FormRules = {
  userName: [{ required: true, message: '请输入用户名称', trigger: 'blur' }],
  nickName: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur', min: 5, max: 20 }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phonenumber: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

async function handleResetPwd(row: UserRecord) {
  try {
    const { value } = await ElMessageBox.prompt(`请输入新密码（用户：${row.userName}）`, '重置密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.{5,20}/,
      inputErrorMessage: '密码长度 5~20'
    })
    await resetUserPwd(row.userId!, value)
    ElMessage.success('密码已重置')
  } catch {
    // canceled
  }
}

onMounted(async () => {
  await crud.fetchList()
  try {
    const res = (await deptTreeSelect()) as { data?: DeptNode[] }
    deptTree.value = res.data ?? []
  } catch {
    deptTree.value = []
  }
})
</script>

<template>
  <div class="scaffold-page user-page">
    <SearchForm
      v-model="crud.query"
      :fields="searchFields"
      @search="crud.fetchList"
      @reset="crud.resetQuery"
    />
    <div class="user-page__layout">
      <aside
        v-if="deptTree.length"
        class="user-page__tree scaffold-card"
      >
        <div class="user-page__tree-title">
          部门
        </div>
        <el-tree
          :data="deptTree"
          :props="{ label: 'label', children: 'children' }"
          node-key="id"
          highlight-current
          @node-click="(node: DeptNode) => { crud.query.deptId = node.id; crud.fetchList() }"
        />
      </aside>
      <section class="user-page__main scaffold-card">
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
          row-key="userId"
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
                  type="warning"
                  link
                  @click="handleResetPwd(row)"
                >
                  重置
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
      </section>
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
        label="用户名称"
        prop="userName"
      >
        <ElInput
          v-model="crud.form.userName"
          :disabled="!!crud.form.userId"
        />
      </ElFormItem>
      <ElFormItem
        label="用户昵称"
        prop="nickName"
      >
        <ElInput v-model="crud.form.nickName" />
      </ElFormItem>
      <ElFormItem
        v-if="!crud.form.userId"
        label="初始密码"
        prop="password"
      >
        <ElInput
          v-model="crud.form.password"
          type="password"
          show-password
        />
      </ElFormItem>
      <ElFormItem
        label="手机号"
        prop="phonenumber"
      >
        <ElInput
          v-model="crud.form.phonenumber"
          maxlength="11"
        />
      </ElFormItem>
      <ElFormItem
        label="邮箱"
        prop="email"
      >
        <ElInput v-model="crud.form.email" />
      </ElFormItem>
      <ElFormItem label="性别">
        <ElSelect
          v-model="crud.form.sex"
          style="width: 100%"
        >
          <ElOption
            v-for="d in dicts.sys_user_sex.value"
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

<style scoped lang="scss">
.user-page {
  &__layout {
    display: grid;
    grid-template-columns: 240px 1fr;
    gap: 16px;

    @media (max-width: 980px) {
      grid-template-columns: 1fr;
    }
  }

  &__tree {
    align-self: flex-start;
  }

  &__tree-title {
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 8px;
  }
}
</style>
