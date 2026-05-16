<script setup lang="ts">
/**
 * FilePreview：通用图片 / pdf 预览弹窗。
 *
 * <p>策略：
 * <ul>
 *   <li>image/* → 直接 img</li>
 *   <li>application/pdf → iframe 嵌入</li>
 *   <li>其它 → 引导下载</li>
 * </ul>
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { downloadUrl, type SysFile } from '../api'

const props = defineProps<{ file: SysFile | null }>()
const visible = defineModel<boolean>({ default: false })
const { t } = useI18n()

const kind = computed<'image' | 'pdf' | 'other'>(() => {
  const m = (props.file?.mime || '').toLowerCase()
  if (m.startsWith('image/')) return 'image'
  if (m === 'application/pdf') return 'pdf'
  return 'other'
})
const url = computed<string>(() => downloadUrl(props.file?.id || 0))
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="file?.name || ''"
    width="80%"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="preview-body">
      <img
        v-if="kind === 'image' && file"
        :src="url"
        :alt="file?.name"
        style="max-width: 100%; max-height: 70vh"
      >
      <iframe
        v-else-if="kind === 'pdf' && file"
        :src="url"
        style="width: 100%; height: 70vh; border: 0"
      />
      <div
        v-else
        class="empty-tip"
      >
        <p>{{ t('file.preview.unsupported', { mime: file?.mime || '?' }) }}</p>
        <a
          :href="url"
          target="_blank"
          rel="noopener"
        >{{ t('file.preview.downloadInstead') }}</a>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.preview-body {
  display: flex;
  justify-content: center;
  align-items: center;
}
.empty-tip {
  text-align: center;
  padding: 40px;
}
</style>
