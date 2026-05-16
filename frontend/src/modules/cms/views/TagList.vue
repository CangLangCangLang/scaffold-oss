<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  createTag,
  deleteTag,
  listTags,
  updateTag,
  type CmsTag
} from '../api'

const { t } = useI18n()

const tags = ref<CmsTag[]>([])
const keyword = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const form = reactive<Partial<CmsTag>>({ name: '', color: '' })

async function fetchTags() {
  loading.value = true
  try {
    const res = await listTags(keyword.value || undefined)
    tags.value = (res?.data || []) as CmsTag[]
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, { id: undefined, name: '', color: '' })
  dialogVisible.value = true
}

function openEdit(t: CmsTag) {
  dialogMode.value = 'edit'
  Object.assign(form, { id: t.id, name: t.name, color: t.color ?? '' })
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.name) return ElMessage.warning(t('cms.article.tipNameRequired'))
  if (dialogMode.value === 'create') {
    await createTag(form)
    ElMessage.success(t('common.created'))
  } else {
    await updateTag(form as CmsTag)
    ElMessage.success(t('common.updated'))
  }
  dialogVisible.value = false
  fetchTags()
}

async function removeTag(tag: CmsTag) {
  try {
    await ElMessageBox.confirm(
      t('cms.confirm.deleteTag', { name: tag.name }),
      t('common.deleteTitle'),
      { type: 'warning' }
    )
    await deleteTag(tag.id)
    ElMessage.success(t('common.deleted'))
    fetchTags()
  } catch (e) {
    if (e === 'cancel') return
  }
}

onMounted(fetchTags)
</script>

<template>
  <div class="cms-tag">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('cms.tag.searchPlaceholder')"
        clearable
        style="width: 240px"
        @keyup.enter="fetchTags"
      />
      <el-button
        type="primary"
        @click="fetchTags"
      >
        {{ t('common.search') }}
      </el-button>
      <el-button @click="openCreate">
        {{ t('cms.tag.createBtn') }}
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="tags"
      border
    >
      <el-table-column
        prop="id"
        label="ID"
        width="80"
        align="center"
      />
      <el-table-column
        :label="t('common.name')"
        min-width="160"
      >
        <template #default="{ row }">
          <el-tag
            :color="row.color || ''"
            :style="row.color ? { color: '#fff', borderColor: row.color } : {}"
            effect="plain"
          >
            {{ row.name }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="color"
        :label="t('cms.tag.color')"
        width="120"
      />
      <el-table-column
        prop="createBy"
        :label="t('common.creator')"
        width="120"
      />
      <el-table-column
        prop="createTime"
        :label="t('common.createTime')"
        width="180"
      />
      <el-table-column
        :label="t('common.operation')"
        width="180"
        align="center"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openEdit(row)"
          >
            {{ t('common.edit') }}
          </el-button>
          <el-button
            link
            type="danger"
            @click="removeTag(row)"
          >
            {{ t('common.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('cms.tag.dialogCreate') : t('cms.tag.dialogEdit')"
      width="480px"
    >
      <el-form
        :model="form"
        label-width="80px"
      >
        <el-form-item :label="t('common.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('cms.tag.color')">
          <el-color-picker
            v-model="form.color"
            show-alpha
          />
          <span
            v-if="form.color"
            style="margin-left: 8px; color: #999"
          >{{ form.color }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="submitForm"
        >
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cms-tag {
  padding: 16px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
