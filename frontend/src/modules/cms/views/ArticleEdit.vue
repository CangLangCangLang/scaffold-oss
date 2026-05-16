<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  getArticle,
  getChannelTree,
  listTags,
  saveArticle,
  type CmsArticleDetail,
  type CmsArticleSaveRequest,
  type CmsChannelTreeNode,
  type CmsTag
} from '../api'
import StatusTag from '../components/StatusTag.vue'
import ArticleReviewBar from '../components/ArticleReviewBar.vue'
import ArticleEditor from '../components/ArticleEditor.vue'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const articleId = computed(() => {
  const v = route.params.id
  return v && v !== 'undefined' ? Number(v) : undefined
})

const loading = ref(false)
const saving = ref(false)
const detail = ref<CmsArticleDetail | null>(null)
const channels = ref<{ id: number; name: string }[]>([])
const tags = ref<CmsTag[]>([])

const form = reactive<CmsArticleSaveRequest>({
  channelId: 0,
  title: '',
  slug: '',
  summary: '',
  coverUrl: '',
  contentHtml: '',
  source: '',
  author: '',
  metaTitle: '',
  metaDescription: '',
  metaKeywords: '',
  canonicalUrl: '',
  sortOrder: 0,
  tagIds: []
})

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

async function fetchDetail() {
  if (!articleId.value) {
    detail.value = null
    return
  }
  loading.value = true
  try {
    const res = await getArticle(articleId.value)
    const d = res?.data as CmsArticleDetail
    detail.value = d
    Object.assign(form, {
      id: d.id,
      channelId: d.channelId,
      title: d.title,
      slug: d.slug,
      summary: d.summary ?? '',
      coverUrl: d.coverUrl ?? '',
      contentHtml: d.contentHtml ?? '',
      source: d.source ?? '',
      author: d.author ?? '',
      metaTitle: d.metaTitle ?? '',
      metaDescription: d.metaDescription ?? '',
      metaKeywords: d.metaKeywords ?? '',
      canonicalUrl: d.canonicalUrl ?? '',
      sortOrder: d.sortOrder ?? 0,
      tagIds: d.tagIds ?? []
    })
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.title.trim()) return ElMessage.warning(t('cms.article.tipTitleRequired'))
  if (!form.channelId) return ElMessage.warning(t('cms.article.tipChannelRequired'))
  saving.value = true
  try {
    const res = await saveArticle({ ...form })
    ElMessage.success(form.id ? t('common.saved') : t('common.created'))
    const saved = res?.data as CmsArticleDetail
    if (saved?.id) {
      router.replace({ name: 'CmsArticleEdit', params: { id: String(saved.id) } })
    }
  } finally {
    saving.value = false
  }
}

function back() {
  router.push({ name: 'CmsArticleList' })
}

watch(articleId, fetchDetail)

onMounted(async () => {
  await fetchAux()
  await fetchDetail()
})
</script>

<template>
  <div
    v-loading="loading"
    class="cms-edit"
  >
    <div class="head">
      <div class="head-left">
        <el-button
          :icon="undefined"
          @click="back"
        >
          {{ t('common.back') }}
        </el-button>
        <span class="title">{{ form.id ? t('cms.edit.editTitle') : t('cms.edit.newTitle') }}</span>
        <StatusTag
          v-if="detail"
          :status="detail.status"
        />
      </div>
      <div class="head-right">
        <ArticleReviewBar
          v-if="detail"
          :article="detail"
          :loading="saving"
          @changed="(d) => { detail = d }"
        />
        <el-button
          type="primary"
          :loading="saving"
          @click="submit"
        >
          {{ t('common.save') }}
        </el-button>
      </div>
    </div>

    <el-form
      :model="form"
      label-width="100px"
      class="form-area"
    >
      <el-row :gutter="16">
        <el-col :span="14">
          <el-form-item :label="t('cms.article.title')">
            <el-input
              v-model="form.title"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>
          <el-form-item :label="t('cms.article.content')">
            <ArticleEditor v-model="form.contentHtml" />
          </el-form-item>
          <el-form-item :label="t('cms.article.summary')">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-col>

        <el-col :span="10">
          <el-form-item :label="t('cms.article.channel')">
            <el-select
              v-model="form.channelId"
              :placeholder="t('cms.article.channelPick')"
              style="width: 100%"
            >
              <el-option
                v-for="c in channels"
                :key="c.id"
                :value="c.id"
                :label="c.name"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('cms.article.tagsLabel')">
            <el-select
              v-model="form.tagIds"
              multiple
              filterable
              :placeholder="t('cms.article.tagsPick')"
              style="width: 100%"
            >
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :value="tag.id"
                :label="tag.name"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('cms.article.coverUrl')">
            <el-input
              v-model="form.coverUrl"
              :placeholder="t('cms.article.coverHint')"
            />
          </el-form-item>
          <el-form-item :label="t('cms.article.author')">
            <el-input v-model="form.author" />
          </el-form-item>
          <el-form-item :label="t('cms.article.source')">
            <el-input v-model="form.source" />
          </el-form-item>
          <el-form-item :label="t('cms.article.slug')">
            <el-input
              v-model="form.slug"
              :placeholder="t('cms.article.slugHint')"
            />
          </el-form-item>
          <el-form-item :label="t('common.sortOrder')">
            <el-input-number
              v-model="form.sortOrder"
              :min="0"
              :max="9999"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        {{ t('cms.edit.seo') }}
      </el-divider>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="meta title">
            <el-input
              v-model="form.metaTitle"
              maxlength="255"
            />
          </el-form-item>
          <el-form-item label="meta keywords">
            <el-input v-model="form.metaKeywords" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="meta description">
            <el-input
              v-model="form.metaDescription"
              type="textarea"
              :rows="2"
              maxlength="500"
            />
          </el-form-item>
          <el-form-item label="canonical url">
            <el-input v-model="form.canonicalUrl" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<style scoped>
.cms-edit {
  padding: 16px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.head-left {
  display: flex;
  gap: 12px;
  align-items: center;
}
.head-right {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.form-area {
  background: var(--el-bg-color);
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}
</style>
