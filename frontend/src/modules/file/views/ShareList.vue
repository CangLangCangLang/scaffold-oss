<script setup lang="ts">
/**
 * 分享列表页（M-6）。
 *
 * <p>展示当前用户创建的全部分享链接，支持停用 / 删除 / 复制 URL。
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  disableShare,
  listMyShares,
  removeShare,
  shareAccessUrl,
  type SysFileShare
} from '../api'

const { t } = useI18n()
const rows = ref<SysFileShare[]>([])
const loading = ref(false)

async function fetchAll(): Promise<void> {
  loading.value = true
  try {
    const res = await listMyShares()
    rows.value = (res as unknown as { data: SysFileShare[] }).data ?? []
  } finally {
    loading.value = false
  }
}
onMounted(fetchAll)

async function disable(row: SysFileShare): Promise<void> {
  await disableShare(row.id)
  ElMessage.success(t('file.shareList.disableOk'))
  void fetchAll()
}

async function remove(row: SysFileShare): Promise<void> {
  try {
    await ElMessageBox.confirm(t('file.shareList.confirmDelete'), '', { type: 'warning' })
  } catch { return }
  await removeShare(row.id)
  ElMessage.success(t('file.shareList.deleteOk'))
  void fetchAll()
}

function statusType(s?: string): 'success' | 'info' | 'warning' {
  if (s === '0') return 'success'
  if (s === '1') return 'info'
  return 'warning'
}
function statusLabel(s?: string): string {
  if (s === '0') return t('file.shareList.statusActive')
  if (s === '1') return t('file.shareList.statusDisabled')
  return t('file.shareList.statusConsumed')
}

function copyLink(row: SysFileShare): void {
  const url = window.location.origin + shareAccessUrl(row.token)
  void navigator.clipboard.writeText(url).then(() => {
    ElMessage.success(t('file.share.copied'))
  })
}
</script>

<template>
  <div class="share-list">
    <el-table
      v-loading="loading"
      :data="rows"
      border
    >
      <el-table-column
        :label="t('file.shareList.colFile')"
        prop="fileId"
        width="100"
      />
      <el-table-column
        :label="t('file.shareList.colToken')"
        prop="token"
        width="240"
      />
      <el-table-column
        :label="t('file.shareList.colStatus')"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            :type="statusType(row.status)"
            size="small"
          >
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('file.shareList.colExpire')"
        prop="expireAt"
        width="170"
      >
        <template #default="{ row }">
          {{ row.expireAt || t('file.shareList.never') }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('file.shareList.colOneTime')"
        prop="oneTime"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          {{ row.oneTime === '1' ? t('file.shareList.yes') : t('file.shareList.no') }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('file.shareList.colVisits')"
        prop="visits"
        width="100"
        align="center"
      />
      <el-table-column
        :label="t('file.shareList.colCreate')"
        prop="createTime"
        width="170"
      />
      <el-table-column
        :label="t('file.shareList.colAction')"
        width="260"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            size="small"
            link
            @click="copyLink(row)"
          >
            {{ t('file.shareList.copy') }}
          </el-button>
          <el-button
            v-if="row.status === '0'"
            v-hasPermi="['file:share:disable']"
            size="small"
            type="warning"
            @click="disable(row)"
          >
            {{ t('file.shareList.disable') }}
          </el-button>
          <el-button
            v-hasPermi="['file:share:remove']"
            size="small"
            type="danger"
            @click="remove(row)"
          >
            {{ t('file.shareList.remove') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.share-list {
  padding: 16px;
}
</style>
