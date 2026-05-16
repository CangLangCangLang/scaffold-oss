<script setup lang="ts">
/**
 * 部门选择 widget（树形下拉）。
 *
 * <p>对接 {@code /system/dept/treeselect}（脚手架自带）；返回值默认是 deptId（number）；
 * 多选 / 单选切换；树深度由后端决定。
 */
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'

const { t } = useI18n()

interface DeptNode {
  id: number
  label: string
  children?: DeptNode[]
}

const props = defineProps<{
  modelValue?: number | number[]
  multiple?: boolean
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: number | number[] | undefined): void }>()

const tree = ref<DeptNode[]>([])
const loading = ref(false)
const selected = ref<number | number[] | undefined>(props.modelValue)

watch(
  () => props.modelValue,
  (v) => (selected.value = v)
)
watch(selected, (v) => emit('update:modelValue', v))

interface DeptResp {
  data?: DeptNode[]
}

onMounted(async () => {
  loading.value = true
  try {
    const res = (await request.get<DeptResp, DeptResp>('/system/dept/treeselect')) as DeptResp
    tree.value = res.data ?? []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <el-tree-select
    v-model="selected"
    :data="tree"
    :multiple="multiple"
    :placeholder="placeholder ?? t('form.widget.deptPicker.placeholder')"
    :disabled="disabled"
    :loading="loading"
    check-strictly
    filterable
    style="width: 100%"
    node-key="id"
  />
</template>
