<script setup lang="ts">
/**
 * 表单提交详情（M-10）。
 *
 * <p>动作：拉提交记录 + 拉对应模板（按 templateId）→ 用 FormRenderer 只读渲染 data
 *  + 显示 raw JSON / 元数据卡片。
 */
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { getSubmission, getTemplate, type FormSubmission, type FormTemplate } from '../api'
import FormRenderer from '../components/FormRenderer.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const submission = ref<FormSubmission | null>(null)
const template = ref<FormTemplate | null>(null)
const data = ref<Record<string, unknown>>({})
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const sRes = await getSubmission(id)
    const s = (sRes as unknown as { data?: FormSubmission }).data ?? (sRes as unknown as FormSubmission)
    if (!s || !s.id) {
      ElMessage.error(t('form.fill.notFound'))
      router.back()
      return
    }
    submission.value = s
    try {
      data.value = JSON.parse(s.data || '{}')
    } catch {
      data.value = {}
    }
    const tRes = await getTemplate(s.templateId)
    const tpl =
      (tRes as unknown as { data?: FormTemplate }).data ?? (tRes as unknown as FormTemplate)
    template.value = tpl
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div
    v-loading="loading"
    class="form-submission-detail"
  >
    <div class="header">
      <el-button @click="router.back()">
        {{ t('form.submission.backToList') }}
      </el-button>
      <h3 v-if="submission">
        {{ t('form.submission.detailTitle', { id: submission.id }) }}
      </h3>
    </div>

    <el-descriptions
      v-if="submission"
      :column="2"
      border
    >
      <el-descriptions-item :label="t('form.submission.colTpl')">
        {{ submission.templateKey }} (v{{ submission.templateVersion }})
      </el-descriptions-item>
      <el-descriptions-item :label="t('form.submission.colSubmitter')">
        {{ submission.submitterName ?? submission.submitter }}
        <span class="username-meta">@{{ submission.submitter }}</span>
      </el-descriptions-item>
      <el-descriptions-item :label="t('form.submission.colTime')">
        {{ submission.createTime }}
      </el-descriptions-item>
      <el-descriptions-item label="ID">
        #{{ submission.id }}
      </el-descriptions-item>
    </el-descriptions>

    <el-card
      v-if="template"
      class="renderer-card"
    >
      <FormRenderer
        v-model="data"
        :schema-json="template.schemaJson"
        readonly
      />
    </el-card>

    <el-card
      v-if="submission"
      class="json-card"
    >
      <template #header>
        <span>data (raw JSON)</span>
      </template>
      <pre class="raw-json">{{ JSON.stringify(JSON.parse(submission.data || '{}'), null, 2) }}</pre>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.form-submission-detail {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  .header {
    display: flex;
    align-items: center;
    gap: 12px;
    h3 {
      margin: 0;
    }
  }
  .renderer-card,
  .json-card {
    margin-top: 4px;
  }
  .username-meta {
    margin-left: 6px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
  .raw-json {
    margin: 0;
    max-height: 300px;
    overflow: auto;
    font-family: ui-monospace, Menlo, Consolas, monospace;
    font-size: 12px;
    background: var(--el-fill-color-lighter);
    padding: 8px;
    border-radius: 4px;
  }
}
</style>
