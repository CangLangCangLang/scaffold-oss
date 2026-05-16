<script setup lang="ts">
import { Search, Refresh } from '@element-plus/icons-vue'
import type { SearchField } from '@/types/table'

type FormState = Record<string, any>

const props = defineProps<{
  modelValue: FormState
  fields: SearchField[]
  inline?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: FormState): void
  (e: 'search'): void
  (e: 'reset'): void
}>()

function readField(prop: string): unknown {
  return props.modelValue[prop]
}

function handleInput(prop: string, value: unknown) {
  emit('update:modelValue', { ...props.modelValue, [prop]: value })
}
</script>

<template>
  <el-form
    :model="modelValue"
    :inline="inline ?? true"
    class="search-form"
    size="default"
  >
    <el-form-item
      v-for="field in fields"
      :key="field.prop"
      :label="field.label"
    >
      <el-input
        v-if="!field.type || field.type === 'input'"
        :model-value="(readField(field.prop) as string) ?? ''"
        :placeholder="field.placeholder || `请输入${field.label}`"
        clearable
        style="width: 200px"
        @update:model-value="(v) => handleInput(field.prop, v)"
        @keyup.enter="emit('search')"
      />
      <el-select
        v-else-if="field.type === 'select'"
        :model-value="(readField(field.prop) as any)"
        :placeholder="field.placeholder || `请选择${field.label}`"
        clearable
        style="width: 200px"
        @update:model-value="(v) => handleInput(field.prop, v)"
      >
        <el-option
          v-for="opt in field.options || []"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-date-picker
        v-else-if="field.type === 'date-range'"
        :model-value="(readField(field.prop) as any)"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @update:model-value="(v) => handleInput(field.prop, v)"
      />
      <el-date-picker
        v-else-if="field.type === 'date'"
        :model-value="(readField(field.prop) as any)"
        type="date"
        value-format="YYYY-MM-DD"
        :placeholder="field.placeholder"
        @update:model-value="(v) => handleInput(field.prop, v)"
      />
    </el-form-item>
    <el-form-item>
      <el-button
        type="primary"
        :icon="Search"
        @click="emit('search')"
      >
        搜索
      </el-button>
      <el-button
        :icon="Refresh"
        @click="emit('reset')"
      >
        重置
      </el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped lang="scss">
.search-form {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px 0;
  margin-bottom: 12px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}
</style>
