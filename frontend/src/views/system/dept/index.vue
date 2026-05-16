<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElInput,
  ElInputNumber,
  ElSelect,
  ElOption,
  ElFormItem,
  ElMessage,
  ElMessageBox,
  ElTreeSelect,
  type FormRules
} from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { listDept, getDept, addDept, updateDept, delDept, type DeptRecord } from '@/api/system/dept'
import { useDict } from '@/composables/useDict'
import FormDialog from '@/components/FormDialog.vue'

interface DeptTreeNode extends DeptRecord {
  children?: DeptTreeNode[]
}

const dicts = useDict('sys_normal_disable')
const loading = ref(false)
const list = ref<DeptTreeNode[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<DeptRecord>({ status: '0', orderNum: 0 })
const treeSelect = ref<{ id?: number; label: string; children?: DeptTreeNode[] }[]>([])

function buildTreeOptions(rows: DeptRecord[]): typeof treeSelect.value {
  const map = new Map<number, DeptTreeNode>()
  const tree: DeptTreeNode[] = []
  rows.forEach((r) => map.set(r.deptId!, { ...r, children: [] }))
  rows.forEach((r) => {
    const node = map.get(r.deptId!)!
    if (r.parentId && map.has(r.parentId)) {
      map.get(r.parentId)!.children!.push(node)
    } else {
      tree.push(node)
    }
  })
  const wrap: typeof treeSelect.value = [
    { id: 0, label: '主类目', children: tree.map((t) => ({ ...t, label: t.deptName ?? '' })) }
  ]
  return wrap
}

async function fetchList() {
  loading.value = true
  try {
    const res = (await listDept()) as { data?: DeptRecord[] }
    const flat = res.data ?? []
    list.value = toTree(flat)
    treeSelect.value = buildTreeOptions(flat)
  } finally {
    loading.value = false
  }
}

function toTree(rows: DeptRecord[]): DeptTreeNode[] {
  const map = new Map<number, DeptTreeNode>()
  const tree: DeptTreeNode[] = []
  rows.forEach((r) => map.set(r.deptId!, { ...r, children: [] }))
  rows.forEach((r) => {
    const node = map.get(r.deptId!)!
    if (r.parentId && map.has(r.parentId)) {
      map.get(r.parentId)!.children!.push(node)
    } else {
      tree.push(node)
    }
  })
  return tree
}

function resetForm() {
  Object.keys(form).forEach((key) => delete (form as Record<string, unknown>)[key])
  Object.assign(form, { status: '0', orderNum: 0 })
}

function handleAdd(parent?: DeptRecord) {
  resetForm()
  if (parent) form.parentId = parent.deptId
  dialogTitle.value = '新增部门'
  dialogVisible.value = true
}

async function handleEdit(row: DeptRecord) {
  resetForm()
  const res = (await getDept(row.deptId!)) as { data?: DeptRecord }
  Object.assign(form, res.data ?? row)
  dialogTitle.value = '编辑部门'
  dialogVisible.value = true
}

async function handleDelete(row: DeptRecord) {
  try {
    await ElMessageBox.confirm(`确认删除部门：${row.deptName}？`, '提示', { type: 'warning' })
    await delDept(row.deptId!)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // cancel
  }
}

async function handleSubmit() {
  if (form.deptId) {
    await updateDept(form)
    ElMessage.success('修改成功')
  } else {
    await addDept(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchList()
}

const rules: FormRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

onMounted(fetchList)
</script>

<template>
  <div class="scaffold-page">
    <div class="scaffold-card">
      <div class="scaffold-toolbar">
        <ElButton
          type="primary"
          :icon="Plus"
          @click="handleAdd()"
        >
          新增
        </ElButton>
        <ElButton
          :icon="Refresh"
          circle
          @click="fetchList"
        />
      </div>
      <el-table
        v-loading="loading"
        :data="list"
        row-key="deptId"
        default-expand-all
        :tree-props="{ children: 'children' }"
        style="width: 100%"
      >
        <el-table-column
          prop="deptName"
          label="部门名称"
          min-width="180"
        />
        <el-table-column
          prop="orderNum"
          label="排序"
          width="100"
          align="center"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
        />
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
              @click="handleAdd(row)"
            >
              新增
            </el-button>
            <el-button
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <FormDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :model="form"
      :rules="rules"
      @submit="handleSubmit"
    >
      <ElFormItem label="上级部门">
        <ElTreeSelect
          v-model="form.parentId"
          :data="treeSelect"
          node-key="deptId"
          :props="({ label: 'label', children: 'children' } as any)"
          check-strictly
          placeholder="选择上级部门"
          style="width: 100%"
        />
      </ElFormItem>
      <ElFormItem
        label="部门名称"
        prop="deptName"
      >
        <ElInput v-model="form.deptName" />
      </ElFormItem>
      <ElFormItem
        label="显示排序"
        prop="orderNum"
      >
        <ElInputNumber
          v-model="form.orderNum"
          :min="0"
        />
      </ElFormItem>
      <ElFormItem label="负责人">
        <ElInput v-model="form.leader" />
      </ElFormItem>
      <ElFormItem label="联系电话">
        <ElInput v-model="form.phone" />
      </ElFormItem>
      <ElFormItem label="邮箱">
        <ElInput v-model="form.email" />
      </ElFormItem>
      <ElFormItem label="状态">
        <ElSelect
          v-model="form.status"
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
    </FormDialog>
  </div>
</template>
