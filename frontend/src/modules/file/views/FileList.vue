<script setup lang="ts">
/**
 * 文件中心列表（M-6）。
 *
 * <p>左侧文件夹树（用户级隔离）+ 右侧文件列表 + 顶部过滤 + 操作（上传 / 重命名 / 移动 / 软删 / 分享 / 预览）。
 *
 * <p>权限：路由由 file:list（看全量）或 file:list:mine（看自己）守卫；按钮均通过 v-hasPermi 控制。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  addFolder,
  batchRemoveFiles,
  downloadUrl,
  editFile,
  listFileRefs,
  listFiles,
  listFolders,
  removeFile,
  removeFolder,
  type FileQuery,
  type SysFile,
  type SysFileFolder
} from '../api'
import FilePreview from '../components/FilePreview.vue'
import ShareDialog from '../components/ShareDialog.vue'
import request from '@/utils/request'

const { t } = useI18n()

const folders = ref<SysFileFolder[]>([])
const rows = ref<SysFile[]>([])
const total = ref(0)
const loading = ref(false)
const selected = ref<SysFile[]>([])
const previewing = ref<SysFile | null>(null)
const previewVisible = ref(false)
const sharing = ref<SysFile | null>(null)
const shareVisible = ref(false)
const editing = ref<SysFile | null>(null)
const editVisible = ref(false)
const refsOf = ref<SysFile | null>(null)
const refsList = ref<{ refModule: string; refType: string; refId: string }[]>([])
const refsVisible = ref(false)

const query = reactive<FileQuery>({
  name: '',
  bucket: '',
  ext: '',
  category: '',
  folderId: undefined,
  delFlag: '0',
  pageNum: 1,
  pageSize: 20
})

interface FolderTreeNode extends SysFileFolder {
  children: FolderTreeNode[]
}

const folderTree = computed<FolderTreeNode[]>(() => {
  const map = new Map<number, FolderTreeNode>()
  for (const f of folders.value) map.set(f.id, { ...f, children: [] })
  const roots: FolderTreeNode[] = []
  for (const f of map.values()) {
    if (f.parentId === 0 || !map.has(f.parentId)) roots.push(f)
    else map.get(f.parentId)!.children.push(f)
  }
  return roots
})

async function fetchFolders(): Promise<void> {
  const res = await listFolders()
  folders.value = (res as unknown as { data: SysFileFolder[] }).data ?? []
}

async function fetchFiles(): Promise<void> {
  loading.value = true
  try {
    const res = await listFiles(query)
    rows.value = res?.rows ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchFolders()
  void fetchFiles()
})

function search(): void {
  query.pageNum = 1
  void fetchFiles()
}
function reset(): void {
  query.name = ''
  query.bucket = ''
  query.ext = ''
  query.category = ''
  query.folderId = undefined
  query.delFlag = '0'
  query.pageNum = 1
  void fetchFiles()
}

function pickFolder(node: FolderTreeNode | null): void {
  query.folderId = node ? node.id : undefined
  query.pageNum = 1
  void fetchFiles()
}

const newFolderName = ref('')
const creatingFolder = ref(false)
async function createFolder(parentId = 0): Promise<void> {
  const name = newFolderName.value.trim()
  if (!name) return
  creatingFolder.value = true
  try {
    await addFolder({ parentId, name })
    ElMessage.success(t('file.folder.createOk'))
    newFolderName.value = ''
    await fetchFolders()
  } finally {
    creatingFolder.value = false
  }
}

async function deleteFolder(node: FolderTreeNode): Promise<void> {
  try {
    await ElMessageBox.confirm(t('file.folder.confirmDelete', { name: node.name }), '', { type: 'warning' })
  } catch { return }
  await removeFolder(node.id)
  ElMessage.success(t('file.folder.deleteOk'))
  await fetchFolders()
  if (query.folderId === node.id) query.folderId = undefined
  void fetchFiles()
}

const uploading = ref(false)
async function onUpload(opts: { file: File }): Promise<void> {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', opts.file)
    if (query.bucket) fd.append('bucket', query.bucket)
    if (query.folderId) fd.append('folderId', String(query.folderId))
    await request.post('/file/file/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success(t('file.list.uploadOk'))
    void fetchFiles()
  } finally {
    uploading.value = false
  }
}

function preview(row: SysFile): void {
  previewing.value = row
  previewVisible.value = true
}

function startShare(row: SysFile): void {
  sharing.value = row
  shareVisible.value = true
}

function startEdit(row: SysFile): void {
  editing.value = { ...row }
  editVisible.value = true
}
async function saveEdit(): Promise<void> {
  if (!editing.value) return
  await editFile({
    id: editing.value.id,
    name: editing.value.name,
    folderId: editing.value.folderId === null ? undefined : editing.value.folderId,
    category: editing.value.category,
    tags: editing.value.tags,
    remark: editing.value.remark
  })
  ElMessage.success(t('file.list.editOk'))
  editVisible.value = false
  void fetchFiles()
}

async function softDelete(row: SysFile): Promise<void> {
  if (row.refCount && row.refCount > 0) {
    ElMessage.warning(t('file.list.refBlock', { count: row.refCount }))
    return
  }
  try {
    await ElMessageBox.confirm(t('file.list.confirmSoft', { name: row.name }), '', { type: 'warning' })
  } catch { return }
  await removeFile(row.id)
  ElMessage.success(t('file.list.deleteOk'))
  void fetchFiles()
}

async function batchSoftDelete(): Promise<void> {
  if (!selected.value.length) return
  try {
    await ElMessageBox.confirm(t('file.list.confirmBatch', { count: selected.value.length }), '', { type: 'warning' })
  } catch { return }
  await batchRemoveFiles(selected.value.map((r) => r.id))
  ElMessage.success(t('file.list.deleteOk'))
  selected.value = []
  void fetchFiles()
}

async function viewRefs(row: SysFile): Promise<void> {
  refsOf.value = row
  const res = await listFileRefs(row.id)
  refsList.value = (res as unknown as { data: typeof refsList.value }).data || []
  refsVisible.value = true
}

function downloadOf(row: SysFile): string {
  return downloadUrl(row.id)
}
function fmtSize(bytes?: number): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
function isPreviewable(row: SysFile): boolean {
  const m = (row.mime || '').toLowerCase()
  return m.startsWith('image/') || m === 'application/pdf'
}
</script>

<template>
  <div class="file-list-page">
    <aside class="folder-side">
      <div class="folder-header">
        <span>{{ t('file.folder.title') }}</span>
        <el-popover
          :title="t('file.folder.create')"
          width="240"
          trigger="click"
        >
          <template #reference>
            <el-button
              v-hasPermi="['file:folder:add']"
              size="small"
              link
            >
              +
            </el-button>
          </template>
          <el-input
            v-model="newFolderName"
            :placeholder="t('file.folder.namePh')"
            @keyup.enter="createFolder(0)"
          />
          <div style="margin-top: 8px; text-align: right">
            <el-button
              size="small"
              :loading="creatingFolder"
              type="primary"
              @click="createFolder(0)"
            >
              {{ t('file.folder.create') }}
            </el-button>
          </div>
        </el-popover>
      </div>
      <el-tree
        :data="folderTree"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        :expand-on-click-node="false"
        @node-click="(n: FolderTreeNode) => pickFolder(n)"
      >
        <template #default="{ node, data }">
          <div class="folder-node">
            <span>{{ node.label }}</span>
            <el-button
              v-hasPermi="['file:folder:remove']"
              size="small"
              link
              type="danger"
              @click.stop="deleteFolder(data)"
            >
              ×
            </el-button>
          </div>
        </template>
      </el-tree>
      <el-button
        link
        size="small"
        @click="pickFolder(null)"
      >
        {{ t('file.folder.allFiles') }}
      </el-button>
    </aside>

    <main class="file-main">
      <div class="filters">
        <el-input
          v-model="query.name"
          :placeholder="t('file.list.search.name')"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-input
          v-model="query.ext"
          :placeholder="t('file.list.search.ext')"
          clearable
          style="width: 120px"
        />
        <el-input
          v-model="query.bucket"
          :placeholder="t('file.list.search.bucket')"
          clearable
          style="width: 160px"
        />
        <el-select
          v-model="query.delFlag"
          :placeholder="t('file.list.search.delFlag')"
          style="width: 140px"
        >
          <el-option
            :label="t('file.list.search.delFlag0')"
            value="0"
          />
          <el-option
            :label="t('file.list.search.delFlag2')"
            value="2"
          />
        </el-select>
        <el-button
          type="primary"
          @click="search"
        >
          {{ $t('common.search') }}
        </el-button>
        <el-button @click="reset">
          {{ $t('common.reset') }}
        </el-button>
        <div style="flex: 1" />
        <el-upload
          v-hasPermi="['file:file:upload']"
          :show-file-list="false"
          :auto-upload="true"
          :http-request="onUpload"
        >
          <el-button
            type="success"
            :loading="uploading"
          >
            {{ t('file.list.upload') }}
          </el-button>
        </el-upload>
        <el-button
          v-hasPermi="['file:file:batch-remove']"
          type="danger"
          :disabled="!selected.length"
          @click="batchSoftDelete"
        >
          {{ t('file.list.batchDelete', { count: selected.length }) }}
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        border
        @selection-change="(s: SysFile[]) => (selected = s)"
      >
        <el-table-column
          type="selection"
          width="50"
        />
        <el-table-column
          :label="t('file.list.colName')"
          min-width="220"
        >
          <template #default="{ row }">
            <a
              v-if="isPreviewable(row)"
              href="#"
              @click.prevent="preview(row)"
            >{{ row.name }}</a>
            <span v-else>{{ row.name }}</span>
          </template>
        </el-table-column>
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
            {{ fmtSize(row.sizeBytes) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('file.list.colBucket')"
          prop="bucket"
          width="140"
        />
        <el-table-column
          :label="t('file.list.colRef')"
          prop="refCount"
          width="80"
          align="center"
        />
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
          width="380"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-hasPermi="['file:file:download']"
              size="small"
              type="primary"
              tag="a"
              :href="downloadOf(row)"
              target="_blank"
            >
              {{ t('file.list.actionDownload') }}
            </el-button>
            <el-button
              v-hasPermi="['file:share:add']"
              size="small"
              @click="startShare(row)"
            >
              {{ t('file.list.actionShare') }}
            </el-button>
            <el-button
              v-hasPermi="['file:file:edit']"
              size="small"
              @click="startEdit(row)"
            >
              {{ t('file.list.actionEdit') }}
            </el-button>
            <el-button
              size="small"
              link
              @click="viewRefs(row)"
            >
              {{ t('file.list.actionRefs') }}
            </el-button>
            <el-button
              v-if="row.delFlag === '0'"
              v-hasPermi="['file:file:remove']"
              size="small"
              type="danger"
              :disabled="(row.refCount || 0) > 0"
              @click="softDelete(row)"
            >
              {{ t('file.list.actionRemove') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pager"
        @current-change="fetchFiles"
        @size-change="fetchFiles"
      />
    </main>

    <FilePreview
      v-model="previewVisible"
      :file="previewing"
    />
    <ShareDialog
      v-model="shareVisible"
      :file="sharing"
    />

    <el-dialog
      v-model="editVisible"
      :title="t('file.list.editTitle')"
      width="520px"
    >
      <el-form
        v-if="editing"
        label-width="100px"
      >
        <el-form-item :label="t('file.list.colName')">
          <el-input v-model="editing.name" />
        </el-form-item>
        <el-form-item :label="t('file.list.editFolder')">
          <el-tree-select
            v-model="editing.folderId"
            :data="folderTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            clearable
            :placeholder="t('file.list.editFolderPh')"
          />
        </el-form-item>
        <el-form-item :label="t('file.list.colCategory')">
          <el-input
            v-model="editing.category"
            :placeholder="t('file.list.search.category')"
          />
        </el-form-item>
        <el-form-item :label="t('file.list.colTags')">
          <el-input
            v-model="editing.tags"
            :placeholder="t('file.list.tagsPh')"
          />
        </el-form-item>
        <el-form-item :label="t('file.list.colRemark')">
          <el-input
            v-model="editing.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">
          {{ t('file.list.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="saveEdit"
        >
          {{ t('file.list.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="refsVisible"
      :title="t('file.refs.title', { name: refsOf?.name || '' })"
      width="600px"
    >
      <p
        v-if="!refsList.length"
        class="empty"
      >
        {{ t('file.refs.none') }}
      </p>
      <el-table
        v-else
        :data="refsList"
        border
      >
        <el-table-column
          :label="t('file.refs.module')"
          prop="refModule"
        />
        <el-table-column
          :label="t('file.refs.type')"
          prop="refType"
        />
        <el-table-column
          :label="t('file.refs.id')"
          prop="refId"
        />
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.file-list-page {
  display: flex;
  height: 100%;
  gap: 12px;
  padding: 16px;
}
.folder-side {
  width: 240px;
  background: #fafafa;
  border-radius: 4px;
  padding: 12px;
  overflow: auto;
  .folder-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-weight: 600;
  }
  .folder-node {
    display: flex;
    justify-content: space-between;
    width: 100%;
  }
}
.file-main {
  flex: 1;
  min-width: 0;
  .filters {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
    align-items: center;
    flex-wrap: wrap;
  }
}
.empty {
  text-align: center;
  color: #999;
  padding: 24px;
}
.pager {
  margin-top: 12px;
}
</style>
