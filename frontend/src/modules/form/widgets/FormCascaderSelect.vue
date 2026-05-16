<script setup lang="ts">
/**
 * 级联选择 widget。
 *
 * <p>静态 options 模式：模板设计器把 [[label, value, children?], ...] 直接配进 schema；
 * 动态 url 模式：传 url prop 后远程拉取（Cascader 走 lazy-load 模式可在后续扩展）。
 *
 * <p>P2 阶段先做 静态 options 模式，覆盖 80% 的省 / 市 / 区类场景；
 * 远程 lazy-load 留给 P3 / 后续。
 */
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface CascaderOptionLocal {
  label: string
  value: string | number
  children?: CascaderOptionLocal[]
  [k: string]: unknown
}

const props = defineProps<{
  modelValue?: (string | number)[]
  options?: CascaderOptionLocal[]
  placeholder?: string
  disabled?: boolean
  /** Element-Plus cascader props 透传，如 { multiple: true, checkStrictly: false } */
  cascaderProps?: Record<string, unknown>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: (string | number)[] | undefined): void
}>()

const selected = ref<(string | number)[] | undefined>(props.modelValue)

watch(
  () => props.modelValue,
  (v) => (selected.value = v)
)
watch(selected, (v) => emit('update:modelValue', v))
</script>

<template>
  <el-cascader
    v-model="selected"
    :options="(options as any) ?? []"
    :placeholder="placeholder ?? t('form.widget.cascaderSelect.placeholder')"
    :disabled="disabled"
    :props="cascaderProps ?? { checkStrictly: false }"
    style="width: 100%"
    clearable
  />
</template>
