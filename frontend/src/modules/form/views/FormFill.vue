<script setup lang="ts">
/**
 * 表单填报页（M-10）。
 *
 * <p>路径：/form/fill/:id（id 是 form_template.id）；
 * 拉模板详情 → 校验 status=PUBLISHED → FormRenderer 渲染 → 提交。
 */
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { getTemplate, submitForm, type FormTemplate } from '../api'
import FormRenderer from '../components/FormRenderer.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const template = ref<FormTemplate | null>(null)
const data = ref<Record<string, unknown>>({})
const loading = ref(false)
const submitting = ref(false)

const rendererRef = ref<{
  validate: () => Promise<{ valid: boolean; fail?: unknown }>
  formData: () => Record<string, unknown>
} | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    const id = Number(route.params.id)
    if (!Number.isFinite(id)) {
      ElMessage.error(t('form.fill.notFound'))
      router.back()
      return
    }
    const res = await getTemplate(id)
    const tpl =
      (res as unknown as { data?: FormTemplate }).data ?? (res as unknown as FormTemplate)
    if (!tpl || !tpl.id) {
      ElMessage.error(t('form.fill.notFound'))
      router.back()
      return
    }
    if (tpl.status === 'ARCHIVED') {
      ElMessage.warning(t('form.fill.archived'))
    }
    if (tpl.status !== 'PUBLISHED') {
      ElMessage.warning(t('form.fill.notPublished'))
    }
    template.value = tpl
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  if (!template.value || !rendererRef.value) return
  const v = await rendererRef.value.validate()
  if (!v.valid) {
    ElMessage.error(t('form.common.required'))
    return
  }
  submitting.value = true
  try {
    const formData = rendererRef.value.formData()
    await submitForm({
      templateId: template.value.id,
      data: JSON.stringify(formData)
    })
    ElMessage.success(t('form.common.submitOk'))
    router.push({ name: 'FormSubmissionList' })
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div
    v-loading="loading"
    class="form-fill-page"
  >
    <el-card v-if="template">
      <template #header>
        <div class="header">
          <span class="title">{{ t('form.fill.title', { name: template.name }) }}</span>
          <el-tag
            v-if="template.status === 'PUBLISHED'"
            type="success"
            size="small"
          >
            v{{ template.version }}
          </el-tag>
          <el-tag
            v-else
            type="info"
            size="small"
          >
            {{ t(`form.template.status.${template.status}`) }}
          </el-tag>
        </div>
      </template>
      <FormRenderer
        ref="rendererRef"
        v-model="data"
        :schema-json="template.schemaJson"
        :readonly="template.status !== 'PUBLISHED'"
      />
      <div class="actions">
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="template.status !== 'PUBLISHED'"
          @click="submit"
        >
          {{ t('form.fill.submit') }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.form-fill-page {
  padding: 16px;
  .header {
    display: flex;
    align-items: center;
    gap: 12px;
    .title {
      font-size: 16px;
      font-weight: 600;
    }
  }
  .actions {
    margin-top: 16px;
    text-align: right;
  }
}
</style>
