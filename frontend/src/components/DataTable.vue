<script setup lang="ts" generic="T extends object">
import { computed } from 'vue'
import { useDict, type DictDataItem } from '@/composables/useDict'
import type { TableColumn } from '@/types/table'

const props = defineProps<{
  data: T[]
  columns: TableColumn<T>[]
  loading?: boolean
  selectable?: boolean
  rowKey?: string
  border?: boolean
  height?: string | number
}>()

type SortPayload = { prop: string; order: string | null }

const emit = defineEmits<{
  (e: 'selection-change', rows: T[]): void
  (e: 'sort-change', value: SortPayload): void
}>()

function onSelectionChange(rows: T[]) {
  emit('selection-change', rows)
}

function onSortChange(value: SortPayload) {
  emit('sort-change', value)
}

const dictTypes = computed(() => {
  const set = new Set<string>()
  props.columns.forEach((c) => {
    if (c.dict) set.add(c.dict)
  })
  return Array.from(set)
})

const dictMaps = useDict(...dictTypes.value)

function dictLabel(type: string, value: unknown): DictDataItem | undefined {
  return dictMaps[type]?.value.find((item) => item.dictValue === String(value))
}

function valueOf(row: T, prop?: string): unknown {
  if (!prop) return undefined
  return (row as Record<string, unknown>)[prop]
}

function formatDate(value: unknown): string {
  if (!value) return ''
  if (typeof value === 'number') {
    return new Date(value).toLocaleString('zh-CN', { hour12: false })
  }
  return String(value)
}
</script>

<template>
  <el-table
    v-loading="loading ?? false"
    :data="data"
    :border="border"
    :row-key="rowKey"
    :height="height"
    stripe
    style="width: 100%"
    @selection-change="onSelectionChange"
    @sort-change="onSortChange"
  >
    <el-table-column
      v-if="selectable"
      type="selection"
      width="48"
      align="center"
      :reserve-selection="true"
    />
    <template
      v-for="col in columns"
      :key="col.prop || col.type"
    >
      <el-table-column
        v-if="col.type === 'index'"
        type="index"
        :label="col.label || '#'"
        :width="col.width || 60"
        align="center"
      />
      <el-table-column
        v-else-if="col.render === 'tag' && col.dict"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align"
        :fixed="col.fixed"
      >
        <template #default="{ row }">
          <el-tag
            v-if="dictLabel(col.dict!, valueOf(row, col.prop))"
            :type="(dictLabel(col.dict!, valueOf(row, col.prop))?.listClass as any) || 'info'"
          >
            {{ dictLabel(col.dict!, valueOf(row, col.prop))?.dictLabel }}
          </el-tag>
          <span v-else>{{ valueOf(row, col.prop) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        v-else-if="col.render === 'date'"
        :prop="col.prop"
        :label="col.label"
        :width="col.width || 180"
        :align="col.align || 'center'"
      >
        <template #default="{ row }">
          {{ formatDate(valueOf(row, col.prop)) }}
        </template>
      </el-table-column>
      <el-table-column
        v-else-if="col.render === 'switch'"
        :prop="col.prop"
        :label="col.label"
        :width="col.width || 90"
        :align="col.align || 'center'"
      >
        <template #default="{ row }">
          <el-switch
            :model-value="valueOf(row, col.prop) === '0'"
            active-text="启用"
            inactive-text="停用"
            inline-prompt
            @change="(v) => col.onSwitchChange?.(row, v ? '0' : '1')"
          />
        </template>
      </el-table-column>
      <el-table-column
        v-else-if="col.render === 'custom'"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align"
        :fixed="col.fixed"
      >
        <template #default="scope">
          <slot
            :name="col.slot || col.prop || 'cell'"
            v-bind="scope"
          />
        </template>
      </el-table-column>
      <el-table-column
        v-else
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align"
        :fixed="col.fixed"
        :show-overflow-tooltip="col.showOverflowTooltip ?? true"
        :sortable="col.sortable"
        :formatter="col.formatter ? (row, _col, value) => col.formatter!(row as T, value) : undefined"
      />
    </template>
    <slot name="action" />
  </el-table>
</template>
