<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElInput,
  ElInputNumber,
  ElSelect,
  ElOption,
  ElRadioGroup,
  ElRadio,
  ElFormItem,
  ElMessage,
  ElMessageBox,
  ElTreeSelect,
  type FormRules
} from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { listMenu, getMenu, addMenu, updateMenu, delMenu, type MenuRecord } from '@/api/system/menu'
import FormDialog from '@/components/FormDialog.vue'

interface MenuTreeNode extends MenuRecord {
  children?: MenuTreeNode[]
}

const loading = ref(false)
const list = ref<MenuTreeNode[]>([])
const treeSelect = ref<{ id?: number; label: string; children?: MenuTreeNode[] }[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<MenuRecord>({
  menuType: 'M',
  visible: '0',
  status: '0',
  isFrame: '1',
  isCache: '0'
})

function toTree(rows: MenuRecord[]): MenuTreeNode[] {
  const map = new Map<number, MenuTreeNode>()
  const tree: MenuTreeNode[] = []
  rows.forEach((r) => map.set(r.menuId!, { ...r, children: [] }))
  rows.forEach((r) => {
    const node = map.get(r.menuId!)!
    if (r.parentId && map.has(r.parentId)) {
      map.get(r.parentId)!.children!.push(node)
    } else {
      tree.push(node)
    }
  })
  return tree
}

async function fetchList() {
  loading.value = true
  try {
    const res = (await listMenu()) as { data?: MenuRecord[] }
    const flat = res.data ?? []
    list.value = toTree(flat)
    treeSelect.value = [
      {
        id: 0,
        label: '主类目',
        children: list.value.map((n) => ({ ...n, label: n.menuName ?? '' }))
      }
    ]
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.keys(form).forEach((key) => delete (form as Record<string, unknown>)[key])
  Object.assign(form, { menuType: 'M', visible: '0', status: '0', isFrame: '1', isCache: '0', orderNum: 0 })
}

function handleAdd(parent?: MenuRecord) {
  resetForm()
  form.parentId = parent?.menuId ?? 0
  dialogTitle.value = '新增菜单'
  dialogVisible.value = true
}

async function handleEdit(row: MenuRecord) {
  resetForm()
  const res = (await getMenu(row.menuId!)) as { data?: MenuRecord }
  Object.assign(form, res.data ?? row)
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

async function handleDelete(row: MenuRecord) {
  try {
    await ElMessageBox.confirm(`确认删除菜单：${row.menuName}？`, '提示', { type: 'warning' })
    await delMenu(row.menuId!)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // cancel
  }
}

async function handleSubmit() {
  if (form.menuId) {
    await updateMenu(form)
    ElMessage.success('修改成功')
  } else {
    await addMenu(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchList()
}

const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入显示排序', trigger: 'blur' }]
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
        row-key="menuId"
        default-expand-all
        :tree-props="{ children: 'children' }"
        style="width: 100%"
      >
        <el-table-column
          prop="menuName"
          label="菜单名称"
          min-width="200"
        />
        <el-table-column
          prop="icon"
          label="图标"
          width="80"
          align="center"
        />
        <el-table-column
          prop="orderNum"
          label="排序"
          width="80"
          align="center"
        />
        <el-table-column
          prop="perms"
          label="权限标识"
          width="200"
        />
        <el-table-column
          prop="component"
          label="组件路径"
          width="220"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          label="状态"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
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
      width="720px"
      @submit="handleSubmit"
    >
      <ElFormItem label="上级菜单">
        <ElTreeSelect
          v-model="form.parentId"
          :data="treeSelect"
          node-key="menuId"
          :props="({ label: 'label', children: 'children' } as any)"
          check-strictly
          placeholder="选择上级菜单"
          style="width: 100%"
        />
      </ElFormItem>
      <ElFormItem label="菜单类型">
        <ElRadioGroup v-model="form.menuType">
          <ElRadio label="M">
            目录
          </ElRadio>
          <ElRadio label="C">
            菜单
          </ElRadio>
          <ElRadio label="F">
            按钮
          </ElRadio>
        </ElRadioGroup>
      </ElFormItem>
      <ElFormItem
        label="菜单名称"
        prop="menuName"
      >
        <ElInput v-model="form.menuName" />
      </ElFormItem>
      <ElFormItem
        v-if="form.menuType !== 'F'"
        label="路由地址"
      >
        <ElInput v-model="form.path" />
      </ElFormItem>
      <ElFormItem
        v-if="form.menuType === 'C'"
        label="组件路径"
      >
        <ElInput v-model="form.component" />
      </ElFormItem>
      <ElFormItem
        v-if="form.menuType !== 'M'"
        label="权限标识"
      >
        <ElInput v-model="form.perms" />
      </ElFormItem>
      <ElFormItem label="图标">
        <ElInput v-model="form.icon" />
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
      <ElFormItem label="显示状态">
        <ElRadioGroup v-model="form.visible">
          <ElRadio label="0">
            显示
          </ElRadio>
          <ElRadio label="1">
            隐藏
          </ElRadio>
        </ElRadioGroup>
      </ElFormItem>
      <ElFormItem label="菜单状态">
        <ElSelect
          v-model="form.status"
          style="width: 100%"
        >
          <ElOption
            label="正常"
            value="0"
          />
          <ElOption
            label="停用"
            value="1"
          />
        </ElSelect>
      </ElFormItem>
    </FormDialog>
  </div>
</template>
