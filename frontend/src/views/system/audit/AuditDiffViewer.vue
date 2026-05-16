<script setup lang="ts">
/**
 * 审计 diff 三模式查看器：字段级表格 / 原始 JSON Patch / 变更前后 JSON 对比。
 *
 * - 字段级表格：把 RFC 6902 patch + before/after 拼成「字段中文名 / 变更前 / 变更后」三列；
 *   字段名翻译走 fieldDict.ts（按 resourceType 选取）；找不到字典词条时 fallback 到 JSON Pointer。
 * - 原始 JSON Patch：把 patch 数组直接列出，给熟悉 RFC 6902 的开发者看。
 * - 变更前后对比：左右两栏 `<pre>` 显示 before / after 完整 JSON。
 *
 * 设计思路：
 * - 不引第三方 diff 库（如 react-diff-viewer / jsondiffpatch）：现有 patch 已经精确指出变化，自己渲染即可。
 * - JSON Pointer 取值用本文件内的 getByPointer（RFC 6901，~15 行），避免再加一个依赖。
 * - replace 时旧值从 before 拿、新值就是 op.value；add/remove 各自只显示一侧。
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { translatePath, translateValue } from './fieldDict'
import type { JsonPatchOp } from '@/api/system/audit'
import { parseDiff } from '@/api/system/audit'

const props = defineProps<{
  /** 后端 sys_audit_log.before_value（JSON 字符串） */
  before?: string
  /** 后端 sys_audit_log.after_value（JSON 字符串） */
  after?: string
  /** 后端 sys_audit_log.diff（JSON 字符串，RFC 6902 patch 数组） */
  diff?: string
  /** 当前审计记录的 resourceType（如 user / role / article），用于选取字段字典 */
  resourceType?: string
}>()

const { t } = useI18n()

const mode = defineModel<'fields' | 'patch' | 'snapshot'>('mode', { default: 'fields' })

const ops = computed<JsonPatchOp[]>(() => parseDiff(props.diff))

const beforeObj = computed<unknown>(() => safeParse(props.before))
const afterObj = computed<unknown>(() => safeParse(props.after))

interface Row {
  op: JsonPatchOp['op']
  path: string
  fieldLabel: string
  beforeDisplay: string
  afterDisplay: string
}

const rows = computed<Row[]>(() =>
  ops.value.map((p) => {
    const fieldLabel = translatePath(props.resourceType, p.path, t)
    let beforeDisplay = '—'
    let afterDisplay = '—'
    switch (p.op) {
      case 'add':
        afterDisplay = renderValue(props.resourceType, p.path, p.value)
        break
      case 'remove':
        beforeDisplay = renderValue(props.resourceType, p.path, getByPointer(beforeObj.value, p.path))
        break
      case 'replace':
        beforeDisplay = renderValue(props.resourceType, p.path, getByPointer(beforeObj.value, p.path))
        afterDisplay = renderValue(props.resourceType, p.path, p.value)
        break
      case 'move':
      case 'copy':
        afterDisplay = `from ${p.from ?? '?'}`
        break
      default:
        afterDisplay = renderValue(props.resourceType, p.path, p.value)
    }
    return { op: p.op, path: p.path, fieldLabel, beforeDisplay, afterDisplay }
  })
)

const opMeta = computed<Record<JsonPatchOp['op'], { label: string; type: 'success' | 'warning' | 'danger' | 'info' }>>(() => ({
  add: { label: t('audit.viewer.opAdd'), type: 'success' },
  replace: { label: t('audit.viewer.opReplace'), type: 'warning' },
  remove: { label: t('audit.viewer.opRemove'), type: 'danger' },
  move: { label: t('audit.viewer.opMove'), type: 'info' },
  copy: { label: t('audit.viewer.opCopy'), type: 'info' },
  test: { label: t('audit.viewer.opTest'), type: 'info' }
}))

const beforePretty = computed<string>(() => prettyJson(props.before))
const afterPretty = computed<string>(() => prettyJson(props.after))

function safeParse(s: string | undefined): unknown {
  if (!s) return undefined
  try {
    return JSON.parse(s)
  } catch {
    return undefined
  }
}

function prettyJson(s: string | undefined): string {
  if (!s) return '—'
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch {
    return s
  }
}

/**
 * RFC 6901 JSON Pointer 取值。如 `/userName` → obj.userName；`/tags/0/name` → obj.tags[0].name。
 * 失败返回 undefined（不抛错）。
 */
function getByPointer(obj: unknown, pointer: string): unknown {
  if (obj === null || obj === undefined || !pointer || pointer === '/') return obj
  const parts = pointer
    .replace(/^\//, '')
    .split('/')
    .map((p) => p.replace(/~1/g, '/').replace(/~0/g, '~'))
  let cur: unknown = obj
  for (const seg of parts) {
    if (cur === null || cur === undefined) return undefined
    if (Array.isArray(cur)) {
      const idx = Number(seg)
      cur = Number.isInteger(idx) ? cur[idx] : undefined
    } else if (typeof cur === 'object') {
      cur = (cur as Record<string, unknown>)[seg]
    } else {
      return undefined
    }
  }
  return cur
}

/** 把字段值渲染成"对人友好"的字符串：先走字典 formatter，再 stringify 对象，最后 String()。 */
function renderValue(resourceType: string | undefined, path: string, raw: unknown): string {
  if (raw === null || raw === undefined) return '—'
  const translated = translateValue(resourceType, path, raw, t)
  if (translated !== undefined) return translated
  if (typeof raw === 'object') {
    try {
      return JSON.stringify(raw)
    } catch {
      return String(raw)
    }
  }
  return String(raw)
}
</script>

<template>
  <div class="audit-diff-viewer">
    <el-segmented
      v-model="mode"
      :options="[
        { label: t('audit.viewer.modeFields'), value: 'fields' },
        { label: t('audit.viewer.modePatch'), value: 'patch' },
        { label: t('audit.viewer.modeSnapshot'), value: 'snapshot' }
      ]"
      size="small"
    />

    <el-empty
      v-if="ops.length === 0 && mode !== 'snapshot'"
      :description="t('audit.viewer.empty')"
      :image-size="56"
    />

    <el-table
      v-else-if="mode === 'fields'"
      :data="rows"
      size="small"
      border
      class="diff-table"
    >
      <el-table-column
        :label="t('audit.viewer.colAction')"
        width="76"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            size="small"
            :type="opMeta[row.op as JsonPatchOp['op']]?.type ?? 'info'"
          >
            {{ opMeta[row.op as JsonPatchOp['op']]?.label ?? row.op }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('audit.viewer.colField')">
        <template #default="{ row }">
          <span class="diff-field">{{ row.fieldLabel }}</span>
          <span
            v-if="row.fieldLabel !== row.path"
            class="diff-field-raw"
          >{{ row.path }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('audit.viewer.colBefore')">
        <template #default="{ row }">
          <span
            class="diff-cell diff-cell-before"
            :class="{ 'is-empty': row.beforeDisplay === '—' }"
          >{{ row.beforeDisplay }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('audit.viewer.colAfter')">
        <template #default="{ row }">
          <span
            class="diff-cell diff-cell-after"
            :class="{ 'is-empty': row.afterDisplay === '—' }"
          >{{ row.afterDisplay }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-table
      v-else-if="mode === 'patch'"
      :data="ops"
      size="small"
      border
      class="diff-table"
    >
      <el-table-column
        label="op"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            size="small"
            :type="opMeta[row.op as JsonPatchOp['op']]?.type ?? 'info'"
          >
            {{ row.op }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="path"
        label="path"
        min-width="180"
      />
      <el-table-column label="value / from">
        <template #default="{ row }">
          <span class="diff-cell">{{ row.value !== undefined ? JSON.stringify(row.value) : (row.from ?? '—') }}</span>
        </template>
      </el-table-column>
    </el-table>

    <div
      v-else-if="mode === 'snapshot'"
      class="diff-snapshot-grid"
    >
      <div class="diff-snapshot-col">
        <div class="diff-snapshot-title">
          {{ t('audit.viewer.snapshotBefore') }}
        </div>
        <pre class="diff-snapshot-body">{{ beforePretty }}</pre>
      </div>
      <div class="diff-snapshot-col">
        <div class="diff-snapshot-title">
          {{ t('audit.viewer.snapshotAfter') }}
        </div>
        <pre class="diff-snapshot-body">{{ afterPretty }}</pre>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.audit-diff-viewer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.diff-table {
  width: 100%;
}

.diff-field {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.diff-field-raw {
  margin-left: 6px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  font-family: 'Consolas', 'Monaco', monospace;
}

.diff-cell {
  word-break: break-all;
  white-space: pre-wrap;
  font-size: 12px;
  line-height: 1.5;

  &.is-empty {
    color: var(--el-text-color-placeholder);
  }
}

.diff-cell-before {
  color: var(--el-color-danger);
  text-decoration: line-through;
  text-decoration-color: var(--el-color-danger-light-5);
}

.diff-cell-after {
  color: var(--el-color-success);
}

.diff-cell-before.is-empty,
.diff-cell-after.is-empty {
  color: var(--el-text-color-placeholder);
  text-decoration: none;
}

.diff-snapshot-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.diff-snapshot-col {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.diff-snapshot-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
  color: var(--el-text-color-primary);
}

.diff-snapshot-body {
  background: var(--el-fill-color-light);
  border-radius: 4px;
  padding: 8px 12px;
  max-height: 320px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
