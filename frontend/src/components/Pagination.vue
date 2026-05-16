<script setup lang="ts">
defineProps<{
  total: number
  pageNum?: number
  pageSize?: number
  pageSizes?: number[]
}>()

const emit = defineEmits<{
  (e: 'update:pageNum', value: number): void
  (e: 'update:pageSize', value: number): void
  (e: 'change'): void
}>()

function onSizeChange(size: number) {
  emit('update:pageSize', size)
  emit('update:pageNum', 1)
  emit('change')
}

function onCurrentChange(current: number) {
  emit('update:pageNum', current)
  emit('change')
}
</script>

<template>
  <div
    class="pagination"
    :class="{ 'is-empty': total === 0 }"
  >
    <el-pagination
      v-if="total > 0"
      :total="total"
      :current-page="pageNum ?? 1"
      :page-size="pageSize ?? 10"
      :page-sizes="pageSizes || [10, 20, 50, 100]"
      :background="true"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="onSizeChange"
      @current-change="onCurrentChange"
    />
  </div>
</template>

<style scoped lang="scss">
.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 0 6px;

  &.is-empty {
    height: 0;
    padding: 0;
  }
}
</style>
