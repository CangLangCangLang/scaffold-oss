<script setup lang="ts">
/**
 * 动态表格 widget：用户可以在表单内添加 / 删除任意条数据行。
 *
 * <p>列定义来自 schema 里的 columns prop（设计器配进去）：
 * <pre>
 * columns: [
 *   { prop: 'name',  label: '姓名',     type: 'text' },
 *   { prop: 'age',   label: '年龄',     type: 'number', min: 0, max: 150 },
 *   { prop: 'role',  label: '角色',     type: 'select', options: [{label:'A',value:'A'}] },
 *   { prop: 'memo',  label: '备注',     type: 'text' }
 * ]
 * </pre>
 * 返回值结构：{@code Array<Record<string, unknown>>}。
 */
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElButton } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

const { t } = useI18n()

interface ColumnDef {
  prop: string
  label: string
  type?: 'text' | 'number' | 'select' | 'switch'
  width?: number | string
  options?: Array<{ label: string; value: string | number }>
  min?: number
  max?: number
  placeholder?: string
}

const props = defineProps<{
  modelValue?: Array<Record<string, unknown>>
  columns: ColumnDef[]
  /** 行数上限，默认 50，避免恶意大量输入 */
  maxRows?: number
  disabled?: boolean
  /** 自定义行新增初值 */
  defaultRow?: Record<string, unknown>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: Array<Record<string, unknown>>): void
}>()

const rows = ref<Array<Record<string, unknown>>>([])

watch(
  () => props.modelValue,
  (v) => {
    rows.value = Array.isArray(v) ? v.map((r) => ({ ...r })) : []
  },
  { immediate: true, deep: true }
)

watch(
  rows,
  (v) => emit('update:modelValue', v.map((r) => ({ ...r }))),
  { deep: true }
)

const limit = computed(() => props.maxRows ?? 50)

function addRow(): void {
  if (rows.value.length >= limit.value) return
  const empty: Record<string, unknown> = {}
  for (const c of props.columns) empty[c.prop] = props.defaultRow?.[c.prop] ?? ''
  rows.value = [...rows.value, empty]
}

function removeRow(idx: number): void {
  rows.value = rows.value.filter((_, i) => i !== idx)
}
</script>

<template>
  <div class="form-dyn-table">
    <el-table
      :data="rows"
      border
      size="small"
    >
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :label="col.label"
        :width="col.width"
        min-width="120"
      >
        <template #default="{ row, $index }">
          <template v-if="col.type === 'number'">
            <el-input-number
              :model-value="(row[col.prop] as number)"
              :min="col.min"
              :max="col.max"
              :disabled="disabled"
              size="small"
              :placeholder="col.placeholder"
              style="width: 100%"
              @update:model-value="(v) => (rows[$index][col.prop] = v as number)"
            />
          </template>
          <template v-else-if="col.type === 'select'">
            <el-select
              :model-value="row[col.prop]"
              :disabled="disabled"
              size="small"
              :placeholder="col.placeholder"
              filterable
              clearable
              style="width: 100%"
              @update:model-value="(v) => (rows[$index][col.prop] = v)"
            >
              <el-option
                v-for="o in col.options"
                :key="o.value"
                :value="o.value"
                :label="o.label"
              />
            </el-select>
          </template>
          <template v-else-if="col.type === 'switch'">
            <el-switch
              :model-value="!!row[col.prop]"
              :disabled="disabled"
              @update:model-value="(v) => (rows[$index][col.prop] = v as boolean)"
            />
          </template>
          <template v-else>
            <el-input
              :model-value="(row[col.prop] as string)"
              :disabled="disabled"
              size="small"
              :placeholder="col.placeholder"
              @update:model-value="(v) => (rows[$index][col.prop] = v)"
            />
          </template>
        </template>
      </el-table-column>

      <el-table-column
        :label="t('form.widget.dynamicTable.colAction')"
        width="80"
        fixed="right"
      >
        <template #default="{ $index }">
          <el-button
            link
            type="danger"
            :icon="Delete"
            :disabled="disabled"
            @click="removeRow($index)"
          />
        </template>
      </el-table-column>

      <template #empty>
        <span class="empty-tip">{{ t('form.widget.dynamicTable.empty') }}</span>
      </template>
    </el-table>

    <div class="actions">
      <el-button
        type="primary"
        plain
        size="small"
        :icon="Plus"
        :disabled="disabled || rows.length >= limit"
        @click="addRow"
      >
        {{
          rows.length >= limit
            ? t('form.widget.dynamicTable.maxReached', { max: limit })
            : t('form.widget.dynamicTable.addRow')
        }}
      </el-button>
      <span class="count-tip">{{ t('form.widget.dynamicTable.countTip', { count: rows.length, max: limit }) }}</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.form-dyn-table {
  .actions {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .count-tip,
  .empty-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
