<script setup lang="ts">
import { Plus, Delete, Refresh } from '@element-plus/icons-vue'

defineProps<{
  selectedCount?: number
  hideAdd?: boolean
  hideDelete?: boolean
  hideRefresh?: boolean
}>()

const emit = defineEmits<{
  (e: 'add'): void
  (e: 'delete'): void
  (e: 'refresh'): void
}>()
</script>

<template>
  <div class="page-toolbar">
    <div class="page-toolbar__left">
      <el-button
        v-if="!hideAdd"
        type="primary"
        :icon="Plus"
        @click="emit('add')"
      >
        新增
      </el-button>
      <el-button
        v-if="!hideDelete"
        type="danger"
        :icon="Delete"
        :disabled="(selectedCount ?? 0) === 0"
        @click="emit('delete')"
      >
        批量删除
      </el-button>
      <slot name="left" />
    </div>
    <div class="page-toolbar__right">
      <slot name="right" />
      <el-button
        v-if="!hideRefresh"
        :icon="Refresh"
        circle
        @click="emit('refresh')"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0 12px;
  gap: 12px;

  &__left,
  &__right {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
</style>
