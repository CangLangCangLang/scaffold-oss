<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  title: string
  width?: string | number
  rules?: FormRules
  model?: Record<string, unknown>
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit'): void
  (e: 'cancel'): void
}>()

const formRef = ref<FormInstance>()

async function onSubmit() {
  if (!formRef.value) {
    emit('submit')
    return
  }
  try {
    await formRef.value.validate()
    emit('submit')
  } catch {
    // validation error - silent
  }
}

function onCancel() {
  emit('update:modelValue', false)
  emit('cancel')
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      formRef.value?.clearValidate?.()
    }
  }
)

defineExpose({
  formRef
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width || '640px'"
    destroy-on-close
    align-center
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <el-form
      ref="formRef"
      :model="model || {}"
      :rules="rules"
      label-width="100px"
      class="form-dialog__body"
    >
      <slot />
    </el-form>
    <template #footer>
      <el-button @click="onCancel">
        取消
      </el-button>
      <el-button
        type="primary"
        :loading="loading"
        @click="onSubmit"
      >
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.form-dialog__body {
  padding: 4px 8px 0;
}
</style>
