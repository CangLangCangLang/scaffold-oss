<script setup lang="ts">
/**
 * 表单模板列表页（M-10）。
 *
 * <p>页面动作：
 * <ul>
 *   <li>新增模板：跳设计器（id 为空走新建分支）</li>
 *   <li>编辑模板：跳设计器（带 id；后端：草稿原地改 / 已发布派生 version+1）</li>
 *   <li>填报：草稿不可填报；只有 PUBLISHED 可跳填报页</li>
 *   <li>发布 / 归档 / 删除：调对应端点 + 弹确认 + Toast</li>
 * </ul>
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  archiveTemplate,
  listTemplates,
  publishTemplate,
  removeTemplate,
  type FormTemplate,
  type FormTemplateQuery
} from '../api'

const { t } = useI18n()
const router = useRouter()

const rows = ref<FormTemplate[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive<FormTemplateQuery>({
  keyword: '',
  status: undefined,
  category: '',
  pageNum: 1,
  pageSize: 20
})

async function fetch(): Promise<void> {
  loading.value = true
  try {
    const res = await listTemplates(query)
    rows.value = res?.rows ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

onMounted(fetch)

function search(): void {
  query.pageNum = 1
  void fetch()
}

function reset(): void {
  query.keyword = ''
  query.status = undefined
  query.category = ''
  query.pageNum = 1
  void fetch()
}

function openDesigner(id?: number): void {
  router.push({
    name: 'FormTemplateDesign',
    params: id !== undefined ? { id: String(id) } : {}
  })
}

function openFill(row: FormTemplate): void {
  if (row.status !== 'PUBLISHED') {
    ElMessage.warning(t('form.fill.notPublished'))
    return
  }
  router.push({ name: 'FormFill', params: { id: String(row.id) } })
}

async function publish(row: FormTemplate): Promise<void> {
  await publishTemplate(row.id)
  ElMessage.success(t('form.common.publishOk'))
  void fetch()
}

async function archive(row: FormTemplate): Promise<void> {
  await archiveTemplate(row.id)
  ElMessage.success(t('form.common.archiveOk'))
  void fetch()
}

async function remove(row: FormTemplate): Promise<void> {
  try {
    await ElMessageBox.confirm(t('form.common.confirmDelete'), '', { type: 'warning' })
  } catch {
    return
  }
  await removeTemplate(row.id)
  ElMessage.success(t('form.common.deleteOk'))
  void fetch()
}

function statusTagType(s: FormTemplate['status']): 'info' | 'success' | 'warning' {
  if (s === 'PUBLISHED') return 'success'
  if (s === 'ARCHIVED') return 'info'
  return 'warning'
}
</script>

<template>
  <div class="form-template-list">
    <div class="filters">
      <el-input
        v-model="query.keyword"
        :placeholder="t('form.template.search.keyword')"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      />
      <el-select
        v-model="query.status"
        :placeholder="t('form.template.search.status')"
        clearable
        style="width: 150px"
      >
        <el-option
          :label="t('form.template.status.DRAFT')"
          value="DRAFT"
        />
        <el-option
          :label="t('form.template.status.PUBLISHED')"
          value="PUBLISHED"
        />
        <el-option
          :label="t('form.template.status.ARCHIVED')"
          value="ARCHIVED"
        />
      </el-select>
      <el-input
        v-model="query.category"
        :placeholder="t('form.template.search.category')"
        clearable
        style="width: 180px"
      />
      <el-button
        type="primary"
        @click="search"
      >
        {{ $t('common.search') }}
      </el-button>
      <el-button @click="reset">
        {{ $t('common.reset') }}
      </el-button>
      <div style="flex: 1" />
      <el-button
        v-hasPermi="['form:template:add']"
        type="success"
        @click="openDesigner()"
      >
        {{ t('form.template.actionAdd') }}
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      border
    >
      <el-table-column
        :label="t('form.template.colKey')"
        prop="formKey"
        width="200"
      />
      <el-table-column
        :label="t('form.template.colName')"
        prop="name"
        min-width="180"
      />
      <el-table-column
        :label="t('form.template.colCategory')"
        prop="category"
        width="120"
      />
      <el-table-column
        :label="t('form.template.colVersion')"
        prop="version"
        width="80"
        align="center"
      />
      <el-table-column
        :label="t('form.template.colStatus')"
        width="120"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            :type="statusTagType(row.status)"
            size="small"
          >
            {{ t(`form.template.status.${row.status}`) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('form.template.colCreate')"
        prop="createTime"
        width="170"
      />
      <el-table-column
        :label="t('form.template.colAction')"
        width="320"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            v-hasPermi="['form:submission:add']"
            size="small"
            type="primary"
            :disabled="row.status !== 'PUBLISHED'"
            @click="openFill(row)"
          >
            {{ t('form.template.actionFill') }}
          </el-button>
          <el-button
            v-hasPermi="['form:template:edit']"
            size="small"
            @click="openDesigner(row.id)"
          >
            {{ t('form.template.actionEdit') }}
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            v-hasPermi="['form:template:publish']"
            size="small"
            type="success"
            @click="publish(row)"
          >
            {{ t('form.template.actionPublish') }}
          </el-button>
          <el-button
            v-if="row.status === 'PUBLISHED'"
            v-hasPermi="['form:template:publish']"
            size="small"
            type="warning"
            @click="archive(row)"
          >
            {{ t('form.template.actionArchive') }}
          </el-button>
          <el-button
            v-if="row.status !== 'PUBLISHED'"
            v-hasPermi="['form:template:remove']"
            size="small"
            type="danger"
            @click="remove(row)"
          >
            {{ t('form.template.actionRemove') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pager"
      @current-change="fetch"
      @size-change="fetch"
    />
  </div>
</template>

<style scoped lang="scss">
.form-template-list {
  padding: 16px;
  .filters {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
    align-items: center;
  }
}
</style>
