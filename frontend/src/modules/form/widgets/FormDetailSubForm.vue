<script setup lang="ts">
/**
 * 明细子表 widget。
 *
 * <p>与 {@link FormDynamicTable} 的区别：
 * <ul>
 *   <li>FormDynamicTable：表格内联编辑，每行一个 record，列固定</li>
 *   <li>FormDetailSubForm：每行展开为完整子表单（用 expand panel），适合字段多 / 行数少的场景</li>
 * </ul>
 *
 * <p>每行也用 form-create rule 渲染（subSchemaJson）—— 一个 form-create 嵌套实例，schema 来自模板设计器。
 *
 * <p>限制：subSchemaJson 必须使用 form-create 标准控件（不嵌套 FormDynamicTable / FormDetailSubForm
 * 自身，避免无限递归）。
 */
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElButton } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

const { t } = useI18n()

const props = defineProps<{
  modelValue?: Array<Record<string, unknown>>
  /** 子表单 schema —— form-create rule[] JSON 字符串 */
  subSchemaJson: string
  /** 行标题模板，{index} 占位符替换成行号；不传则用默认 */
  rowTitle?: string
  maxRows?: number
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: Array<Record<string, unknown>>): void
}>()

const rows = ref<Array<Record<string, unknown>>>([])
const expanded = ref<number[]>([0])

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

const subRule = ref<unknown[]>([])
try {
  subRule.value = JSON.parse(props.subSchemaJson)
} catch {
  subRule.value = []
}

watch(
  () => props.subSchemaJson,
  (v) => {
    try {
      subRule.value = JSON.parse(v)
    } catch {
      subRule.value = []
    }
  }
)

const limit = computeMaxRows()
function computeMaxRows(): number {
  return props.maxRows ?? 20
}

function addRow(): void {
  if (rows.value.length >= limit) return
  rows.value.push({})
  expanded.value = [rows.value.length - 1]
}

function removeRow(idx: number): void {
  rows.value = rows.value.filter((_, i) => i !== idx)
  expanded.value = expanded.value
    .map((e) => (e === idx ? -1 : e > idx ? e - 1 : e))
    .filter((e) => e >= 0)
}

function rowTitleFor(idx: number): string {
  const tpl = props.rowTitle ?? t('form.widget.detailSubForm.defaultRowTitle')
  return tpl.replace('{index}', String(idx + 1))
}
</script>

<template>
  <div class="form-detail-subform">
    <el-collapse
      v-model="expanded"
      accordion
    >
      <el-collapse-item
        v-for="(row, idx) in rows"
        :key="idx"
        :name="idx"
        :title="rowTitleFor(idx)"
      >
        <template #title>
          <span class="row-header">
            <span>{{ rowTitleFor(idx) }}</span>
            <el-button
              link
              type="danger"
              :icon="Delete"
              :disabled="disabled"
              size="small"
              @click.stop="removeRow(idx)"
            />
          </span>
        </template>
        <form-create
          v-model="rows[idx]"
          :rule="subRule"
          :option="{ submitBtn: false, resetBtn: false }"
        />
      </el-collapse-item>
    </el-collapse>

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
            ? t('form.widget.detailSubForm.maxReached', { max: limit })
            : t('form.widget.detailSubForm.addRow')
        }}
      </el-button>
      <span class="count-tip">{{ t('form.widget.detailSubForm.countTip', { count: rows.length, max: limit }) }}</span>
    </div>
    <el-empty
      v-if="rows.length === 0"
      :description="t('form.widget.detailSubForm.empty')"
      :image-size="56"
    />
  </div>
</template>

<style scoped lang="scss">
.form-detail-subform {
  .row-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    padding-right: 16px;
  }
  .actions {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .count-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
