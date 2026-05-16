<script setup lang="ts">
import { computed, defineAsyncComponent, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  transitArticle,
  type ArticleAction,
  type ArticleStatus,
  type CmsArticleDetail
} from '../api'

/**
 * M-4 / M-5：当 article.processInstanceId 存在时，按钮文案与流程语义切换为"审批通过 / 审批驳回"，
 * 同时多出一个"查看审批进度"按钮——点击会懒加载 workflow 模块的 ProcessProgressDialog。
 *
 * 前端跨模块依赖处理：
 *   - 用 defineAsyncComponent + 动态 import 懒加载 workflow/components/ProcessProgressDialog.vue
 *   - 若用户删了 frontend/src/modules/workflow 目录，构建时 vite 会直接报错告知用户
 *     需要同步删除 ArticleReviewBar 这部分耦合；这与"删 jar 即下线"的后端约束方向一致
 *   - 桥模块未启用 / 文章未走 workflow 时，processInstanceId 为空，按钮不显示，组件仍然 100% 自包含
 */
const ProcessProgressDialog = defineAsyncComponent(
  () => import('@/modules/workflow/components/ProcessProgressDialog.vue')
)

const props = defineProps<{
  article: Pick<CmsArticleDetail, 'id' | 'status' | 'processInstanceId'> | null
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'changed', detail: CmsArticleDetail): void
}>()

const { t } = useI18n()

const status = computed<ArticleStatus | undefined>(() => props.article?.status as ArticleStatus | undefined)

/** 文章是否走过 workflow（M-4 桥启用且已 submit 过的文章）。 */
const inWorkflow = computed(() => !!props.article?.processInstanceId)

const allowSubmit = computed(() => status.value === 'DRAFT')
const allowApprove = computed(() => status.value === 'PENDING')
const allowReject = computed(() => status.value === 'PENDING')
const allowUnpublish = computed(() => status.value === 'PUBLISHED')
const allowRepublish = computed(() => status.value === 'UNPUBLISHED')
const allowBackToDraft = computed(() =>
  status.value === 'PENDING' || status.value === 'PUBLISHED' || status.value === 'UNPUBLISHED'
)

const progressVisible = ref(false)

function showProgress() {
  if (!props.article?.processInstanceId) {
    ElMessage.warning(t('cms.article.articleNoFlow'))
    return
  }
  progressVisible.value = true
}

async function trigger(action: ArticleAction, opts: { confirm?: string; askReason?: boolean } = {}) {
  if (!props.article) return
  try {
    if (opts.confirm) {
      await ElMessageBox.confirm(opts.confirm, t('common.confirmTitle'), { type: 'warning' })
    }
    let reason: string | undefined
    if (opts.askReason) {
      const promptRet = await ElMessageBox.prompt(
        t('cms.reason.prompt'),
        t('cms.reason.title'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          inputType: 'textarea'
        }
      )
      reason = promptRet.value || undefined
    }
    const res = await transitArticle(props.article.id, action, reason)
    ElMessage.success(t('common.success'))
    if (res?.data) emit('changed', res.data as CmsArticleDetail)
  } catch (e) {
    if (e === 'cancel') return
  }
}
</script>

<template>
  <div class="review-bar">
    <el-button
      v-if="allowSubmit"
      v-permission="['cms:article:submit']"
      type="primary"
      :loading="loading"
      @click="trigger('submit', { confirm: inWorkflow ? t('cms.confirm.submitWf') : t('cms.confirm.submit') })"
    >
      {{ inWorkflow ? t('cms.action.submitWf') : t('cms.action.submit') }}
    </el-button>
    <el-button
      v-if="allowApprove"
      v-permission="['cms:article:approve']"
      type="success"
      :loading="loading"
      @click="trigger('approve', { confirm: inWorkflow ? t('cms.confirm.approveWf') : t('cms.confirm.approve') })"
    >
      {{ inWorkflow ? t('cms.action.approveWf') : t('cms.action.approve') }}
    </el-button>
    <el-button
      v-if="allowReject"
      v-permission="['cms:article:approve']"
      type="warning"
      :loading="loading"
      @click="trigger('reject', { askReason: true })"
    >
      {{ inWorkflow ? t('cms.action.rejectWf') : t('cms.action.reject') }}
    </el-button>
    <el-button
      v-if="allowUnpublish"
      v-permission="['cms:article:unpublish']"
      type="danger"
      :loading="loading"
      @click="trigger('unpublish', { confirm: t('cms.confirm.unpublish'), askReason: true })"
    >
      {{ t('cms.action.unpublish') }}
    </el-button>
    <el-button
      v-if="allowRepublish"
      v-permission="['cms:article:publish']"
      type="success"
      :loading="loading"
      @click="trigger('publish', { confirm: t('cms.confirm.publish') })"
    >
      {{ t('cms.action.publish') }}
    </el-button>
    <el-button
      v-if="allowBackToDraft"
      v-permission="['cms:article:edit']"
      :loading="loading"
      @click="trigger('back-to-draft', { confirm: t('cms.confirm.backToDraft') })"
    >
      {{ t('cms.action.backToDraft') }}
    </el-button>

    <!-- M-4：当文章关联了 workflow 流程实例时，提供查看审批进度入口（懒加载，不在主 bundle） -->
    <el-button
      v-if="inWorkflow"
      type="info"
      plain
      :loading="loading"
      @click="showProgress"
    >
      {{ t('cms.action.progress') }}
    </el-button>
    <ProcessProgressDialog
      v-if="inWorkflow"
      v-model="progressVisible"
      :process-instance-id="article?.processInstanceId || undefined"
      :title="article?.id ? t('cms.article.progressTitle', { id: article.id }) : ''"
    />
  </div>
</template>

<style scoped>
.review-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
