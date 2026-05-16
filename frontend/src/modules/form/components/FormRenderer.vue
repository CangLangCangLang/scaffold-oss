<script setup lang="ts">
/**
 * 表单运行时渲染器。
 *
 * <p>用法：拿到 schemaJson（form-create rule[] 的 JSON 字符串）+ 初值，把它渲染成可填报表单；
 * 通过暴露的 ref 调用 validate() / formData() / setData() 与外部交互。
 *
 * <p>本组件**只用 form-create runtime**（@form-create/element-ui 已在 workflow 模块全局注册），
 * 不引入设计器代码 —— 设计器只在 TemplateDesign 视图懒加载，避免污染填报场景的 chunk 体积。
 *
 * <p>widgets：6 个高阶 widget（FormDynamicTable / FormDetailSubForm / FormUserPicker /
 * FormDeptPicker / FormDictSelect / FormCascaderSelect）已通过模块 install 钩子注册为
 * 全局组件，schema 里 {@code type: 'FormUserPicker'} 即可直接使用。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface Props {
  /** form-create rule[] JSON 字符串（来自后端 form_template.schemaJson） */
  schemaJson: string
  /** 初值；提交回填 / 编辑场景使用 */
  modelValue?: Record<string, unknown>
  /** 只读模式：rule 不变，但所有控件 disabled */
  readonly?: boolean
  /** 渲染失败时回调（场景：schema 格式坏 / 不可解析） */
  onError?: (err: Error) => void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => ({}),
  readonly: false,
  onError: undefined
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, unknown>): void
  (e: 'change', v: Record<string, unknown>): void
}>()

/** 解析后的 rule（form-create 数组） */
const rule = ref<unknown[]>([])
/** form-create option（global config） */
const option = ref<Record<string, unknown>>({
  submitBtn: false,
  resetBtn: false,
  global: { col: { span: 24 } }
})
/** form-create 实例 ref；需要外部调用 validate / formData 时用 */
const fApi = ref<{
  validate: (cb: (valid: boolean, fail?: unknown) => void) => void
  formData: () => Record<string, unknown>
  setValue: (data: Record<string, unknown>) => void
  disabled: (state: boolean) => void
} | null>(null)

const localData = ref<Record<string, unknown>>({ ...props.modelValue })

const parseError = computed(() => {
  try {
    const arr = JSON.parse(props.schemaJson)
    if (!Array.isArray(arr)) return new Error(t('form.renderer.errSchemaNotArray'))
    return null
  } catch (e) {
    return e as Error
  }
})

watch(
  () => props.schemaJson,
  () => {
    if (parseError.value) {
      props.onError?.(parseError.value)
      ElMessage.error(t('form.renderer.errSchemaParse', { msg: parseError.value.message }))
      rule.value = []
      return
    }
    rule.value = JSON.parse(props.schemaJson)
    // 触发 form-create 重新挂载（rule[] 变化通常即触发渲染重置）
  },
  { immediate: false }
)

watch(
  () => props.modelValue,
  (v) => {
    localData.value = { ...v }
    if (fApi.value) fApi.value.setValue(localData.value)
  },
  { deep: true }
)

watch(
  () => props.readonly,
  (v) => {
    if (fApi.value) fApi.value.disabled(v)
  }
)

watch(
  localData,
  (v) => {
    emit('update:modelValue', { ...v })
    emit('change', { ...v })
  },
  { deep: true }
)

onMounted(() => {
  if (parseError.value) {
    props.onError?.(parseError.value)
    ElMessage.error(t('form.renderer.errSchemaParse', { msg: parseError.value.message }))
    return
  }
  rule.value = JSON.parse(props.schemaJson)
})

/** 暴露给父组件的 API */
function validate(): Promise<{ valid: boolean; fail?: unknown }> {
  return new Promise((resolve) => {
    if (!fApi.value) return resolve({ valid: false, fail: 'form-not-ready' })
    fApi.value.validate((valid, fail) => resolve({ valid, fail }))
  })
}

function formData(): Record<string, unknown> {
  return fApi.value ? fApi.value.formData() : { ...localData.value }
}

function setData(d: Record<string, unknown>): void {
  localData.value = { ...d }
  if (fApi.value) fApi.value.setValue(localData.value)
}

defineExpose({ validate, formData, setData })
</script>

<template>
  <div
    class="form-renderer"
    :class="{ readonly }"
  >
    <el-alert
      v-if="parseError"
      type="error"
      :title="t('form.renderer.errSchemaTitle')"
      :description="parseError.message"
      :closable="false"
    />
    <form-create
      v-else
      v-model="localData"
      v-model:api="fApi"
      :rule="rule"
      :option="option"
    />
  </div>
</template>

<style scoped lang="scss">
.form-renderer {
  &.readonly :deep(.el-input__inner),
  &.readonly :deep(.el-textarea__inner) {
    background-color: var(--el-fill-color-lighter);
  }
}
</style>
