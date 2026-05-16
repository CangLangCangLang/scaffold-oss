<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue'
import {
  ElMessage,
  ElInput,
  ElInputNumber,
  ElSelect,
  ElOption,
  ElFormItem,
  ElTree,
  type FormRules
} from 'element-plus'
import {
  listRole,
  getRole,
  addRole,
  updateRole,
  delRole,
  changeRoleStatus,
  dataScope as authDataScope,
  deptTreeSelect,
  type RoleRecord,
  type RoleQuery,
  type DeptTreeNode
} from '@/api/system/role'
import { useCrud } from '@/composables/useCrud'
import { useDict } from '@/composables/useDict'
import SearchForm from '@/components/SearchForm.vue'
import PageToolbar from '@/components/PageToolbar.vue'
import DataTable from '@/components/DataTable.vue'
import FormDialog from '@/components/FormDialog.vue'
import Pagination from '@/components/Pagination.vue'
import type { TableColumn, SearchField } from '@/types/table'

const dicts = useDict('sys_normal_disable')

/** 与 DataScopeAspect 五种范围严格对齐 */
const DATA_SCOPE_OPTIONS = [
  { value: '1', label: '全部数据权限' },
  { value: '2', label: '自定义数据权限' },
  { value: '3', label: '本部门数据权限' },
  { value: '4', label: '本部门及以下数据权限' },
  { value: '5', label: '仅本人数据权限' }
] as const

function dataScopeText(value?: string): string {
  return DATA_SCOPE_OPTIONS.find((o) => o.value === value)?.label ?? '-'
}

const crud = useCrud<RoleQuery & Record<string, unknown>, RoleRecord>({
  defaultQuery: () => ({ pageNum: 1, pageSize: 10, roleName: '', roleKey: '', status: '' }),
  defaultForm: () => ({
    status: '0',
    roleSort: 0,
    dataScope: '1',
    menuCheckStrictly: true,
    deptCheckStrictly: true
  }),
  fetchList: listRole,
  rowKey: 'roleId',
  getOne: (id) => getRole(id as number),
  create: addRole,
  update: updateRole,
  remove: delRole
})

const searchFields: SearchField[] = [
  { prop: 'roleName', label: '角色名称', type: 'input' },
  { prop: 'roleKey', label: '权限字符', type: 'input' },
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

async function handleStatusToggle(row: RoleRecord, value: string) {
  try {
    await changeRoleStatus(row.roleId!, value)
    row.status = value
    ElMessage.success('状态已更新')
  } catch {
    row.status = row.status === '0' ? '1' : '0'
  }
}

const columns: TableColumn<RoleRecord>[] = [
  { prop: 'roleId', label: '编号', width: 80, align: 'center' },
  { prop: 'roleName', label: '角色名称', minWidth: 120 },
  { prop: 'roleKey', label: '权限字符', minWidth: 120 },
  { prop: 'roleSort', label: '排序', width: 90, align: 'center' },
  {
    prop: 'dataScope',
    label: '数据范围',
    width: 180,
    formatter: (row: RoleRecord) => dataScopeText(row.dataScope)
  },
  { prop: 'status', label: '状态', render: 'switch', width: 100, align: 'center', onSwitchChange: handleStatusToggle },
  { prop: 'createTime', label: '创建时间', render: 'date', width: 180 }
]

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }],
  roleSort: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

// ---------------- 数据权限分配对话框 ----------------

const dsVisible = ref(false)
const dsLoading = ref(false)
const dsSubmitting = ref(false)
const dsForm = ref<RoleRecord>({})
const dsDeptTree = ref<DeptTreeNode[]>([])
const deptTreeRef = ref<InstanceType<typeof ElTree> | null>(null)

async function openDataScope(row: RoleRecord) {
  dsForm.value = { ...row }
  dsDeptTree.value = []
  dsVisible.value = true
  dsLoading.value = true
  try {
    const res = await deptTreeSelect(row.roleId!)
    dsDeptTree.value = res.depts ?? []
    // 自定义部门时勾选回显；其他范围下不展示部门树，但仍要回填，避免提交时丢失
    dsForm.value.deptIds = res.checkedKeys ?? []
    await nextTick()
    if (dsForm.value.dataScope === '2') {
      // strictly 模式下只勾叶子；这里依据 deptCheckStrictly 决定 leafOnly
      const leafOnly = dsForm.value.deptCheckStrictly !== false
      deptTreeRef.value?.setCheckedKeys(dsForm.value.deptIds ?? [], leafOnly)
    }
  } finally {
    dsLoading.value = false
  }
}

function onDsScopeChange(value: string) {
  dsForm.value.dataScope = value
  if (value !== '2') {
    // 切到非自定义范围，清掉部门树勾选
    deptTreeRef.value?.setCheckedKeys([], false)
  }
}

async function submitDataScope() {
  if (!dsForm.value.roleId) return
  dsSubmitting.value = true
  try {
    if (dsForm.value.dataScope === '2') {
      const halfChecked = deptTreeRef.value?.getHalfCheckedKeys?.() ?? []
      const checked = deptTreeRef.value?.getCheckedKeys?.() ?? []
      dsForm.value.deptIds = [...checked, ...halfChecked] as number[]
    } else {
      dsForm.value.deptIds = []
    }
    await authDataScope(dsForm.value)
    ElMessage.success('数据权限已更新')
    dsVisible.value = false
    await crud.fetchList()
  } finally {
    dsSubmitting.value = false
  }
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
        row-key="roleId"
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
                @click="openDataScope(row)"
              >
                数据权限
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
        label="角色名称"
        prop="roleName"
      >
        <ElInput v-model="crud.form.roleName" />
      </ElFormItem>
      <ElFormItem
        label="权限字符"
        prop="roleKey"
      >
        <ElInput v-model="crud.form.roleKey" />
      </ElFormItem>
      <ElFormItem
        label="角色排序"
        prop="roleSort"
      >
        <ElInputNumber
          v-model="crud.form.roleSort"
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
      <ElFormItem
        label="数据范围"
        prop="dataScope"
      >
        <ElSelect
          v-model="crud.form.dataScope"
          style="width: 100%"
          placeholder="新建后可在列表「数据权限」按钮里改自定义部门"
        >
          <ElOption
            v-for="o in DATA_SCOPE_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
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

    <el-dialog
      v-model="dsVisible"
      title="分配数据权限"
      width="560px"
    >
      <el-form
        v-loading="dsLoading"
        label-width="100px"
      >
        <el-form-item label="角色">
          {{ dsForm.roleName }}（{{ dsForm.roleKey }}）
        </el-form-item>
        <el-form-item label="数据范围">
          <ElSelect
            :model-value="dsForm.dataScope"
            style="width: 100%"
            @change="onDsScopeChange"
          >
            <ElOption
              v-for="o in DATA_SCOPE_OPTIONS"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </ElSelect>
        </el-form-item>
        <el-form-item
          v-if="dsForm.dataScope === '2'"
          label="父子联动"
        >
          <el-switch
            v-model="dsForm.deptCheckStrictly"
            inline-prompt
            active-text="联动"
            inactive-text="独立"
          />
        </el-form-item>
        <el-form-item
          v-if="dsForm.dataScope === '2'"
          label="授权部门"
        >
          <div class="dept-tree-wrap">
            <ElTree
              ref="deptTreeRef"
              :data="dsDeptTree"
              :props="{ children: 'children', label: 'label' }"
              :check-strictly="!dsForm.deptCheckStrictly"
              show-checkbox
              node-key="id"
              empty-text="无可分配部门"
            />
          </div>
        </el-form-item>
        <el-form-item
          v-else
          label="说明"
        >
          <span class="ds-hint">{{ dataScopeText(dsForm.dataScope) }}（无需选部门）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dsVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="dsSubmitting"
          @click="submitDataScope"
        >
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.dept-tree-wrap {
  width: 100%;
  max-height: 320px;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 8px;
}

.ds-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
