<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  deleteArticle,
  getChannelTree,
  listArticles,
  listTags,
  transitArticle,
  type ArticleAction,
  type ArticleStatus,
  type CmsArticleSummary,
  type CmsChannelTreeNode,
  type CmsTag
} from '../api'
import StatusTag from '../components/StatusTag.vue'
import { ArrowDown } from '@element-plus/icons-vue'

const { t } = useI18n()

const router = useRouter()
const list = ref<CmsArticleSummary[]>([])
const total = ref(0)
const loading = ref(false)

const channels = ref<{ id: number; name: string }[]>([])
const tags = ref<CmsTag[]>([])

const query = reactive({
  channelId: undefined as number | undefined,
  status: undefined as ArticleStatus | undefined,
  keyword: '',
  tagId: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20
})

const statusOptions = computed<{ value: ArticleStatus; label: string }[]>(() => [
  { value: 'DRAFT', label: t('cms.status.DRAFT') },
  { value: 'PENDING', label: t('cms.status.PENDING') },
  { value: 'PUBLISHED', label: t('cms.status.PUBLISHED') },
  { value: 'UNPUBLISHED', label: t('cms.status.UNPUBLISHED') }
])

function flattenChannels(nodes: CmsChannelTreeNode[], depth: number): { id: number; name: string }[] {
  const out: { id: number; name: string }[] = []
  for (const n of nodes) {
    out.push({ id: n.id, name: '— '.repeat(depth) + n.name })
    if (n.children?.length) out.push(...flattenChannels(n.children, depth + 1))
  }
  return out
}

async function fetchAux() {
  const [chTree, tagRes] = await Promise.all([getChannelTree(false), listTags()])
  channels.value = flattenChannels((chTree?.data || []) as CmsChannelTreeNode[], 0)
  tags.value = (tagRes?.data || []) as CmsTag[]
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listArticles({ ...query })
    list.value = (res?.rows || []) as CmsArticleSummary[]
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

function reset() {
  query.channelId = undefined
  query.status = undefined
  query.keyword = ''
  query.tagId = undefined
  query.pageNum = 1
  fetchList()
}

function goCreate() {
  router.push({ name: 'CmsArticleEdit', params: { id: undefined as unknown as string } })
}

function goEdit(row: CmsArticleSummary) {
  router.push({ name: 'CmsArticleEdit', params: { id: String(row.id) } })
}

async function remove(row: CmsArticleSummary) {
  try {
    await ElMessageBox.confirm(
      t('cms.confirm.deleteArticle', { title: row.title }),
      t('cms.article.deleteSoftTitle'),
      { type: 'warning' }
    )
    await deleteArticle(row.id)
    ElMessage.success(t('common.deleted'))
    fetchList()
  } catch (e) {
    if (e === 'cancel') return
  }
}

interface QuickAction { value: ArticleAction; label: string; perm: string }

function quickActionsForRow(row: CmsArticleSummary): QuickAction[] {
  const out: QuickAction[] = []
  switch (row.status) {
    case 'DRAFT':
      out.push({ value: 'submit', label: t('cms.action.submit'), perm: 'cms:article:submit' })
      break
    case 'PENDING':
      out.push({ value: 'approve', label: t('cms.action.approve'), perm: 'cms:article:approve' })
      out.push({ value: 'reject', label: t('cms.action.reject'), perm: 'cms:article:approve' })
      break
    case 'PUBLISHED':
      out.push({ value: 'unpublish', label: t('cms.action.unpublish'), perm: 'cms:article:unpublish' })
      break
    case 'UNPUBLISHED':
      out.push({ value: 'publish', label: t('cms.action.publish'), perm: 'cms:article:publish' })
      out.push({ value: 'back-to-draft', label: t('cms.action.backToDraft'), perm: 'cms:article:edit' })
      break
  }
  return out
}

async function quick(row: CmsArticleSummary, action: ArticleAction) {
  try {
    await transitArticle(row.id, action)
    ElMessage.success(t('common.success'))
    fetchList()
  } catch {
    /* swallow */
  }
}

onMounted(async () => {
  await fetchAux()
  await fetchList()
})
</script>

<template>
  <div class="cms-article">
    <div class="filter">
      <el-select
        v-model="query.channelId"
        :placeholder="t('cms.article.channelAll')"
        clearable
        style="width: 200px"
      >
        <el-option
          v-for="c in channels"
          :key="c.id"
          :value="c.id"
          :label="c.name"
        />
      </el-select>
      <el-select
        v-model="query.status"
        :placeholder="t('cms.article.statusAll')"
        clearable
        style="width: 140px"
      >
        <el-option
          v-for="s in statusOptions"
          :key="s.value"
          :value="s.value"
          :label="s.label"
        />
      </el-select>
      <el-select
        v-model="query.tagId"
        :placeholder="t('cms.article.tagsAll')"
        clearable
        style="width: 160px"
      >
        <el-option
          v-for="tag in tags"
          :key="tag.id"
          :value="tag.id"
          :label="tag.name"
        />
      </el-select>
      <el-input
        v-model="query.keyword"
        :placeholder="t('cms.article.searchPlaceholder')"
        clearable
        style="width: 240px"
        @keyup.enter="fetchList"
      />
      <el-button
        type="primary"
        @click="fetchList"
      >
        {{ t('common.search') }}
      </el-button>
      <el-button @click="reset">
        {{ t('common.reset') }}
      </el-button>
      <el-button
        v-permission="['cms:article:add']"
        type="success"
        @click="goCreate"
      >
        {{ t('cms.articleCreate') }}
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="list"
      border
    >
      <el-table-column
        prop="id"
        label="ID"
        width="70"
        align="center"
      />
      <el-table-column
        :label="t('cms.article.title')"
        min-width="280"
      >
        <template #default="{ row }">
          <a
            class="title-link"
            @click="goEdit(row)"
          >{{ row.title }}</a>
          <el-tag
            v-if="row.processInstanceId"
            size="small"
            type="info"
            effect="plain"
            class="wf-chip"
            :title="t('cms.article.inWorkflowTitle', { id: row.processInstanceId })"
          >
            {{ t('cms.article.inWorkflow') }}
          </el-tag>
          <div class="meta">
            <span class="slug">/{{ row.slug }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('cms.article.channel')"
        width="120"
      >
        <template #default="{ row }">
          {{ channels.find((c) => c.id === row.channelId)?.name?.replace(/^[— ]+/, '') ?? row.channelId }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('cms.article.tagsLabel')"
        width="200"
      >
        <template #default="{ row }">
          <el-tag
            v-for="tag in row.tags || []"
            :key="tag.id"
            size="small"
            class="tag-chip"
            :color="tag.color || ''"
            :style="tag.color ? { color: '#fff', borderColor: tag.color } : {}"
            effect="plain"
          >
            {{ tag.name }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('common.status')"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column
        prop="viewCount"
        :label="t('cms.article.viewCount')"
        width="80"
        align="center"
      />
      <el-table-column
        prop="publishedAt"
        :label="t('common.publishedAt')"
        width="170"
      />
      <el-table-column
        prop="createBy"
        :label="t('cms.article.author')"
        width="100"
      />
      <el-table-column
        prop="updateTime"
        :label="t('common.updateTime')"
        width="170"
      />
      <el-table-column
        :label="t('common.operation')"
        width="240"
        align="center"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            v-permission="['cms:article:edit']"
            link
            type="primary"
            @click="goEdit(row)"
          >
            {{ t('common.edit') }}
          </el-button>
          <el-dropdown
            v-if="quickActionsForRow(row).length"
            trigger="click"
            @command="(act: ArticleAction) => quick(row, act)"
          >
            <el-button
              link
              type="primary"
            >
              {{ t('cms.action.flow') }}<el-icon class="el-icon--right">
                <ArrowDown />
              </el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="a in quickActionsForRow(row)"
                  :key="a.value"
                  v-permission="[a.perm]"
                  :command="a.value"
                >
                  {{ a.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button
            v-permission="['cms:article:remove']"
            link
            type="danger"
            @click="remove(row)"
          >
            {{ t('common.delete') }}
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
      @current-change="fetchList"
      @size-change="fetchList"
    />
  </div>
</template>

<style scoped>
.cms-article {
  padding: 16px;
}
.filter {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.title-link {
  color: var(--el-color-primary);
  cursor: pointer;
}
.meta .slug {
  color: #999;
  font-size: 12px;
}
.tag-chip {
  margin-right: 4px;
  margin-bottom: 2px;
}
.wf-chip {
  margin-left: 8px;
  vertical-align: middle;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
  display: flex;
}
</style>
