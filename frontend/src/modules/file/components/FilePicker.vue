<script setup lang="ts">
/**
 * FilePicker：文件中心的复用组件。
 *
 * <p>用法（CMS / form 等模块）：
 * <pre>
 *   &lt;FilePicker v-model="fileId" :bucket="'cms/article'" /&gt;
 * </pre>
 *
 * <p>组件行为：
 * <ul>
 *   <li>展示当前选中文件名与下载链接</li>
 *   <li>点 选择文件 弹出文件中心列表抽屉，按 bucket 默认过滤</li>
 *   <li>点 上传 直接走 framework 通用 /system/upload/file（不依赖 file 模块本身）</li>
 *   <li>v-model 暴露 fileId（数字）；额外暴露 url / name 给父组件展示用</li>
 * </ul>
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { listFiles, type SysFile } from '../api'
import request from '@/utils/request'

const props = defineProps<{
  bucket?: string
  multiple?: boolean
  pageSize?: number
}>()
const fileId = defineModel<number | undefined>({ default: undefined })

const { t } = useI18n()
const drawerOpen = ref(false)
const loading = ref(false)
const rows = ref<SysFile[]>([])
const total = ref(0)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(props.pageSize ?? 10)
const currentName = ref('')
const currentUrl = ref('')

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await listFiles({
      name: keyword.value || undefined,
      bucket: props.bucket,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    rows.value = res?.rows ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

watch(drawerOpen, (open) => {
  if (open) {
    pageNum.value = 1
    void fetchList()
  }
})

function pick(row: SysFile): void {
  fileId.value = row.id
  currentName.value = row.name
  currentUrl.value = row.storagePath
  drawerOpen.value = false
  ElMessage.success(t('file.picker.picked', { name: row.name }))
}

const uploading = ref(false)
async function uploadDirect(file: File): Promise<void> {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    if (props.bucket) fd.append('bucket', props.bucket)
    const res = await request.post<{ data: { url: string; originalFilename: string } }>(
      '/system/upload/file',
      fd,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    const data = (res as unknown as { data: { url: string; originalFilename: string } }).data
    currentUrl.value = data.url
    currentName.value = data.originalFilename
    ElMessage.success(t('file.picker.uploaded'))
  } finally {
    uploading.value = false
  }
}
function onUpload(opts: { file: File }): Promise<void> {
  return uploadDirect(opts.file)
}

function clearPicked(): void {
  fileId.value = undefined
  currentName.value = ''
  currentUrl.value = ''
}
</script>

<template>
  <div class="file-picker">
    <template v-if="fileId || currentUrl">
      <a
        v-if="currentUrl"
        :href="currentUrl"
        target="_blank"
        rel="noopener"
      >{{ currentName || $t('file.picker.unknown') }}</a>
      <span v-else>{{ currentName || `#${fileId}` }}</span>
      <el-button
        size="small"
        link
        type="danger"
        @click="clearPicked"
      >
        {{ t('file.picker.clear') }}
      </el-button>
    </template>
    <el-button
      size="small"
      type="primary"
      :loading="uploading"
      @click="drawerOpen = true"
    >
      {{ t('file.picker.pick') }}
    </el-button>
    <el-upload
      style="display: inline-block; margin-left: 8px"
      :show-file-list="false"
      :auto-upload="true"
      :http-request="onUpload"
    >
      <el-button
        size="small"
        :loading="uploading"
      >
        {{ t('file.picker.upload') }}
      </el-button>
    </el-upload>

    <el-drawer
      v-model="drawerOpen"
      :title="t('file.picker.drawerTitle')"
      size="60%"
    >
      <div class="picker-toolbar">
        <el-input
          v-model="keyword"
          :placeholder="t('file.list.search.name')"
          clearable
          style="width: 220px"
          @keyup.enter="fetchList()"
        />
        <el-button
          type="primary"
          @click="fetchList()"
        >
          {{ $t('common.search') }}
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="rows"
        border
        height="60vh"
      >
        <el-table-column
          :label="t('file.list.colName')"
          prop="name"
          min-width="220"
        />
        <el-table-column
          :label="t('file.list.colExt')"
          prop="ext"
          width="80"
        />
        <el-table-column
          :label="t('file.list.colSize')"
          width="120"
        >
          <template #default="{ row }">
            {{ ((row.sizeBytes || 0) / 1024).toFixed(1) }} KB
          </template>
        </el-table-column>
        <el-table-column
          :label="t('file.list.colCreator')"
          prop="createByName"
          width="120"
        />
        <el-table-column
          :label="t('file.list.colCreateTime')"
          prop="createTime"
          width="170"
        />
        <el-table-column
          :label="t('file.list.colAction')"
          width="100"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              @click="pick(row)"
            >
              {{ t('file.picker.choose') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchList()"
        @size-change="fetchList()"
      />
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.file-picker {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}
.picker-toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}
</style>
