<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ArticleStatus } from '../api'

type ElTagType = 'success' | 'warning' | 'info' | 'danger'

const props = defineProps<{ status?: ArticleStatus | string }>()

const { t } = useI18n()

const cfg = computed<{ type: ElTagType; label: string }>(() => {
  switch (props.status) {
    case 'DRAFT':
      return { type: 'info', label: t('cms.status.DRAFT') }
    case 'PENDING':
      return { type: 'warning', label: t('cms.status.PENDING') }
    case 'PUBLISHED':
      return { type: 'success', label: t('cms.status.PUBLISHED') }
    case 'UNPUBLISHED':
      return { type: 'danger', label: t('cms.status.UNPUBLISHED') }
    default:
      return { type: 'info', label: props.status ?? '-' }
  }
})
</script>

<template>
  <el-tag
    :type="cfg.type"
    size="small"
  >
    {{ cfg.label }}
  </el-tag>
</template>
