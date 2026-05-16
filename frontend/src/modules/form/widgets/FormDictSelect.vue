<script setup lang="ts">
/**
 * 字典下拉 widget。
 *
 * <p>对接 {@code /system/dict/data/type/{dictType}}；
 * 提供 dictType prop（字典编码，如 sys_normal_disable / sys_yes_no）；
 * 自动按 dict_sort 升序展示；返回值是 dict_value（string）。
 */
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'

const { t } = useI18n()

interface DictData {
  dictCode: number
  dictLabel: string
  dictValue: string
  cssClass?: string
  listClass?: string
  dictSort?: number
}

const props = defineProps<{
  modelValue?: string | string[]
  /** 字典编码，必填 */
  dictType: string
  multiple?: boolean
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: string | string[] | undefined): void }>()

const items = ref<DictData[]>([])
const loading = ref(false)
const selected = ref<string | string[] | undefined>(props.modelValue)

watch(
  () => props.modelValue,
  (v) => (selected.value = v)
)
watch(selected, (v) => emit('update:modelValue', v))

watch(
  () => props.dictType,
  () => void load(),
  { immediate: false }
)

interface DictResp {
  data?: DictData[]
}

async function load(): Promise<void> {
  if (!props.dictType) {
    items.value = []
    return
  }
  loading.value = true
  try {
    const res = (await request.get<DictResp, DictResp>(
      `/system/dict/data/type/${props.dictType}`
    )) as DictResp
    items.value = (res.data ?? [])
      .slice()
      .sort((a: DictData, b: DictData) => (a.dictSort ?? 0) - (b.dictSort ?? 0))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-select
    v-model="selected"
    :multiple="multiple"
    :placeholder="placeholder ?? t('form.widget.dictSelect.placeholder')"
    :disabled="disabled"
    :loading="loading"
    filterable
    clearable
    style="width: 100%"
  >
    <el-option
      v-for="d in items"
      :key="d.dictCode"
      :value="d.dictValue"
      :label="d.dictLabel"
    />
  </el-select>
</template>
