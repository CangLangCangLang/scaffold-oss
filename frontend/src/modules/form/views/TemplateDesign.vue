<script setup lang="ts">
/**
 * 表单模板设计器（M-10）。
 *
 * <p>关键点：
 * <ul>
 *   <li>FcDesigner 走 defineAsyncComponent 懒加载（vendor-form-create-designer ~1MB chunk
 *       仅在打开此路由时下载，TemplateList / FormFill / SubmissionList 不受影响）</li>
 *   <li>编辑模式：拉模板详情 → setRule(schemaJson)</li>
 *   <li>新增模式：进入空 designer，校验 formKey 必填且未冲突</li>
 *   <li>保存草稿：调 add / edit；后端按 status 智能派生新版本</li>
 *   <li>发布：先 save 再调 publish 端点</li>
 * </ul>
 */
import { defineAsyncComponent, markRaw, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  addTemplate,
  editTemplate,
  getTemplate,
  publishTemplate,
  type FormTemplate
} from '../api'

const FcDesigner = defineAsyncComponent(async () => {
  const mod = await import('@form-create/designer')
  return markRaw(mod.default)
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const isNew = ref(true)
const id = ref<number | null>(null)
const original = ref<FormTemplate | null>(null)
const formKey = ref('')
const name = ref('')
const category = ref('')
const description = ref('')
const loading = ref(false)
const saving = ref(false)

const designerRef = ref<{
  setRule: (r: unknown[]) => void
  getRule: () => unknown[]
} | null>(null)

async function load(): Promise<void> {
  const rid = route.params.id
  if (!rid) {
    isNew.value = true
    return
  }
  isNew.value = false
  id.value = Number(rid)
  loading.value = true
  try {
    const res = await getTemplate(id.value)
    const t = (res as unknown as { data?: FormTemplate }).data ?? (res as unknown as FormTemplate)
    original.value = t
    formKey.value = t.formKey
    name.value = t.name
    category.value = t.category ?? ''
    description.value = t.description ?? ''
    // 等 designer 异步加载完才能 setRule —— Vue 渲染下一个 tick 再调
    setTimeout(() => {
      try {
        const rule = JSON.parse(t.schemaJson || '[]')
        designerRef.value?.setRule(rule)
      } catch (e) {
        ElMessage.error('schemaJson 解析失败：' + (e as Error).message)
      }
    }, 300)
  } finally {
    loading.value = false
  }
}

onMounted(load)

function collect(): { schemaJson: string } | null {
  if (!designerRef.value) return null
  try {
    const rule = designerRef.value.getRule()
    return { schemaJson: JSON.stringify(rule) }
  } catch (e) {
    ElMessage.error((e as Error).message)
    return null
  }
}

function validateMeta(): boolean {
  if (!name.value) {
    ElMessage.warning(t('form.template.name'))
    return false
  }
  if (isNew.value && !formKey.value) {
    ElMessage.warning(t('form.template.formKey'))
    return false
  }
  return true
}

async function saveDraft(): Promise<void> {
  if (!validateMeta()) return
  const data = collect()
  if (!data) return
  saving.value = true
  try {
    if (isNew.value) {
      const res = await addTemplate({
        formKey: formKey.value,
        name: name.value,
        category: category.value || undefined,
        description: description.value || undefined,
        schemaJson: data.schemaJson
      })
      const created = (res as unknown as { data?: FormTemplate }).data ?? (res as unknown as FormTemplate)
      ElMessage.success(t('form.template.designSaveDraft'))
      router.replace({ name: 'FormTemplateDesign', params: { id: String(created.id) } })
    } else {
      await editTemplate({
        id: id.value!,
        name: name.value,
        category: category.value || undefined,
        description: description.value || undefined,
        schemaJson: data.schemaJson
      })
      ElMessage.success(t('form.template.designSaveDraft'))
      // 后端可能派生新版本：刷新拿到当前最新 id
      void load()
    }
  } finally {
    saving.value = false
  }
}

async function publish(): Promise<void> {
  if (!id.value && isNew.value) {
    try {
      await ElMessageBox.confirm(
        t('form.template.designSaveDraft') + ' → ' + t('form.template.designPublish'),
        '',
        { type: 'info' }
      )
    } catch {
      return
    }
    await saveDraft()
  }
  if (!id.value) return
  await publishTemplate(id.value)
  ElMessage.success(t('form.common.publishOk'))
  router.push({ name: 'FormTemplateList' })
}
</script>

<template>
  <div
    v-loading="loading"
    class="form-template-design"
  >
    <div class="meta-card">
      <el-form
        inline
        label-width="100px"
      >
        <el-form-item :label="t('form.template.formKey')">
          <el-input
            v-model="formKey"
            :disabled="!isNew"
            :placeholder="t('form.template.formKey')"
            style="width: 220px"
          />
          <el-tooltip
            v-if="isNew"
            :content="t('form.template.formKeyTip')"
            placement="top"
          >
            <el-icon class="ml-4">
              <InfoFilled />
            </el-icon>
          </el-tooltip>
        </el-form-item>
        <el-form-item :label="t('form.template.name')">
          <el-input
            v-model="name"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item :label="t('form.template.category')">
          <el-input
            v-model="category"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item :label="t('form.template.description')">
          <el-input
            v-model="description"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="saving"
            @click="saveDraft"
          >
            {{ t('form.template.designSaveDraft') }}
          </el-button>
          <el-button
            type="success"
            :loading="saving"
            @click="publish"
          >
            {{ t('form.template.designPublish') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="designer-wrap">
      <FcDesigner ref="designerRef" />
    </div>
  </div>
</template>

<script lang="ts">
import { InfoFilled } from '@element-plus/icons-vue'
export default { components: { InfoFilled } }
</script>

<style scoped lang="scss">
.form-template-design {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;

  .meta-card {
    background: #fff;
    border-radius: 6px;
    padding: 12px 16px;
  }

  .designer-wrap {
    flex: 1 1 auto;
    height: calc(100vh - 240px);
    min-height: 540px;
    background: #fff;
    border-radius: 6px;
  }
  .ml-4 {
    margin-left: 4px;
    color: var(--el-text-color-secondary);
  }
}
</style>
