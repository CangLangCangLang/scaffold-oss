<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  createChannel,
  deleteChannel,
  getChannelTree,
  updateChannel,
  type CmsChannel,
  type CmsChannelTreeNode
} from '../api'

const { t } = useI18n()

const tree = ref<CmsChannelTreeNode[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const form = reactive<Partial<CmsChannel>>({
  parentId: 0,
  code: '',
  name: '',
  orderNum: 0,
  status: '0',
  keywords: '',
  description: '',
  template: ''
})

const flatChannels = ref<{ id: number; name: string; depth: number }[]>([])

async function fetchTree() {
  loading.value = true
  try {
    const res = await getChannelTree(false)
    tree.value = (res?.data || []) as CmsChannelTreeNode[]
    flatChannels.value = flatten(tree.value, 0)
  } catch {
    /* 已由 request 拦截器统一弹错 */
  } finally {
    loading.value = false
  }
}

function flatten(nodes: CmsChannelTreeNode[], depth: number): { id: number; name: string; depth: number }[] {
  const out: { id: number; name: string; depth: number }[] = []
  for (const n of nodes) {
    out.push({ id: n.id, name: '— '.repeat(depth) + n.name, depth })
    if (n.children?.length) {
      out.push(...flatten(n.children, depth + 1))
    }
  }
  return out
}

function openCreate(parent?: CmsChannelTreeNode) {
  dialogMode.value = 'create'
  Object.assign(form, {
    id: undefined,
    parentId: parent ? parent.id : 0,
    code: '',
    name: '',
    orderNum: 0,
    status: '0',
    keywords: '',
    description: '',
    template: ''
  })
  dialogVisible.value = true
}

function openEdit(node: CmsChannelTreeNode) {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: node.id,
    parentId: node.parentId,
    code: node.code,
    name: node.name,
    orderNum: node.orderNum,
    status: node.status,
    keywords: node.keywords ?? '',
    description: node.description ?? '',
    template: node.template ?? ''
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.code) return ElMessage.warning(t('cms.article.tipChannelCodeRequired'))
  if (!form.name) return ElMessage.warning(t('cms.article.tipChannelNameRequired'))
  try {
    if (dialogMode.value === 'create') {
      await createChannel(form)
      ElMessage.success(t('common.created'))
    } else {
      await updateChannel(form as CmsChannel)
      ElMessage.success(t('common.updated'))
    }
    dialogVisible.value = false
    fetchTree()
  } catch {
    /* swallow */
  }
}

async function removeNode(node: CmsChannelTreeNode) {
  try {
    await ElMessageBox.confirm(
      t('cms.confirm.deleteChannel', { name: node.name }),
      t('common.deleteTitle'),
      { type: 'warning' }
    )
    await deleteChannel(node.id)
    ElMessage.success(t('common.deleted'))
    fetchTree()
  } catch (e) {
    if (e === 'cancel') return
  }
}

onMounted(fetchTree)
</script>

<template>
  <div class="cms-channel">
    <div class="toolbar">
      <el-button
        type="primary"
        :icon="undefined"
        @click="openCreate()"
      >
        {{ t('cms.channel.createRoot') }}
      </el-button>
      <el-button
        :icon="undefined"
        @click="fetchTree"
      >
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="tree"
      row-key="id"
      :tree-props="{ children: 'children' }"
      default-expand-all
      border
    >
      <el-table-column
        prop="name"
        :label="t('cms.channel.name')"
        min-width="200"
      />
      <el-table-column
        prop="code"
        :label="t('cms.channel.code')"
        width="160"
      />
      <el-table-column
        prop="orderNum"
        :label="t('common.sortOrder')"
        width="80"
        align="center"
      />
      <el-table-column
        :label="t('common.status')"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            :type="row.status === '0' ? 'success' : 'info'"
            size="small"
          >
            {{ row.status === '0' ? t('common.enable') : t('common.disable') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="description"
        :label="t('common.description')"
        min-width="240"
        show-overflow-tooltip
      />
      <el-table-column
        :label="t('common.operation')"
        width="220"
        align="center"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openCreate(row)"
          >
            {{ t('cms.channel.addChild') }}
          </el-button>
          <el-button
            link
            type="primary"
            @click="openEdit(row)"
          >
            {{ t('common.edit') }}
          </el-button>
          <el-button
            link
            type="danger"
            @click="removeNode(row)"
          >
            {{ t('common.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('cms.channel.dialogCreate') : t('cms.channel.dialogEdit')"
      width="640px"
    >
      <el-form
        :model="form"
        label-width="100px"
      >
        <el-form-item :label="t('cms.channel.parent')">
          <el-select
            v-model="form.parentId"
            :placeholder="t('cms.channel.parentPick')"
            style="width: 100%"
          >
            <el-option
              :value="0"
              :label="t('cms.channel.parentRoot')"
            />
            <el-option
              v-for="c in flatChannels"
              :key="c.id"
              :value="c.id"
              :label="c.name"
              :disabled="dialogMode === 'edit' && c.id === form.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('cms.channel.code')">
          <el-input
            v-model="form.code"
            :placeholder="t('cms.channel.codeHint')"
          />
        </el-form-item>
        <el-form-item :label="t('common.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('common.sortOrder')">
          <el-input-number
            v-model="form.orderNum"
            :min="0"
            :max="999"
          />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-radio-group v-model="form.status">
            <el-radio value="0">
              {{ t('common.enable') }}
            </el-radio>
            <el-radio value="1">
              {{ t('common.disable') }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('cms.channel.seoKeywords')">
          <el-input
            v-model="form.keywords"
            :placeholder="t('cms.channel.seoKeywordsHint')"
          />
        </el-form-item>
        <el-form-item :label="t('cms.channel.seoDescription')">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-form-item :label="t('cms.channel.template')">
          <el-input
            v-model="form.template"
            :placeholder="t('cms.channel.templateHint')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="submitForm"
        >
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cms-channel {
  padding: 16px;
}
.toolbar {
  margin-bottom: 12px;
}
</style>
