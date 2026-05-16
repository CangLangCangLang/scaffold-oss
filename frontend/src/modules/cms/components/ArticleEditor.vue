<script setup lang="ts">
import {
  defineAsyncComponent,
  markRaw,
  onBeforeUnmount,
  ref,
  shallowRef,
  watch
} from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'
import '@wangeditor/editor/dist/css/style.css'

const { t } = useI18n()

/**
 * wangEditor 整体打包 700KB+，与 form-create-designer 一个量级，所以走
 * defineAsyncComponent 懒加载，配合 vite manualChunks 中的 vendor-wangeditor
 * 一一对应：仅 ArticleEdit 路由打开时才下载本 chunk，主 bundle 不受影响。
 */
const Editor = defineAsyncComponent(async () => {
  const mod = await import('@wangeditor/editor-for-vue')
  return markRaw(mod.Editor)
})
const Toolbar = defineAsyncComponent(async () => {
  const mod = await import('@wangeditor/editor-for-vue')
  return markRaw(mod.Toolbar)
})

const props = defineProps<{
  modelValue?: string
}>()
const emit = defineEmits<{ (e: 'update:modelValue', val: string): void }>()

type WangEditorInstance = {
  destroy: () => void
}

const editorRef = shallowRef<WangEditorInstance | null>(null)
const html = ref<string>(props.modelValue ?? '')
const mode: 'default' | 'simple' = 'default'

watch(
  () => props.modelValue,
  (v) => {
    if (v !== html.value) html.value = v ?? ''
  }
)
watch(html, (v) => emit('update:modelValue', v))

const toolbarConfig = {
  excludeKeys: [
    'fullScreen',
    'group-video',
    'insertVideo',
    'uploadVideo'
  ]
}

interface UploadResult {
  data?: { url?: string }
  url?: string
}

const editorConfig = {
  placeholder: t('cms.editor.placeholder'),
  MENU_CONF: {
    uploadImage: {
      maxFileSize: 10 * 1024 * 1024,
      timeout: 60 * 1000,
      async customUpload(file: File, insertFn: (url: string, alt?: string, href?: string) => void) {
        const fd = new FormData()
        fd.append('file', file)
        try {
          const res = (await request.post<UploadResult, UploadResult>(
            '/cms/upload/image',
            fd,
            { headers: { 'Content-Type': 'multipart/form-data' } }
          )) as UploadResult
          const url = res?.data?.url ?? res?.url ?? ''
          if (!url) throw new Error(t('cms.editor.uploadNoUrl'))
          insertFn(url, file.name)
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e)
          ElMessage.error(t('cms.editor.uploadFailed', { msg }))
        }
      }
    }
  }
}

function onCreated(editor: WangEditorInstance) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  if (editorRef.value) {
    editorRef.value.destroy()
    editorRef.value = null
  }
})
</script>

<template>
  <div class="wang-wrap">
    <Toolbar
      class="wang-toolbar"
      :editor="editorRef"
      :default-config="toolbarConfig"
      :mode="mode"
    />
    <Editor
      v-model="html"
      class="wang-editor"
      :default-config="editorConfig"
      :mode="mode"
      @on-created="onCreated"
    />
  </div>
</template>

<style scoped>
.wang-wrap {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
}
.wang-toolbar {
  border-bottom: 1px solid var(--el-border-color-light);
}
.wang-editor {
  height: 480px;
  overflow-y: auto;
}
</style>
