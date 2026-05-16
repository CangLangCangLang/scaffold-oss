<script setup lang="ts">
/**
 * 表单提交记录列表（M-10）。
 *
 * <p>页面行为：
 * <ul>
 *   <li>非 admin：后端强制按 submitter=current 过滤；表标题显示"我的提交记录"</li>
 *   <li>admin：可按 submitter / templateKey / 时间区间任意过滤；表标题显示"全部提交记录"</li>
 *   <li>查看行 → 详情页（/form/submission/:id）</li>
 * </ul>
 */
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { listSubmissions, type FormSubmission, type FormSubmissionQuery } from '../api'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const isAdmin = ref(false)

const rows = ref<FormSubmission[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive<FormSubmissionQuery>({
  templateKey: '',
  submitter: '',
  beginTime: '',
  endTime: '',
  pageNum: 1,
  pageSize: 20
})

const range = ref<[string, string] | null>(null)

async function fetch(): Promise<void> {
  loading.value = true
  try {
    if (range.value) {
      query.beginTime = range.value[0]
      query.endTime = range.value[1]
    } else {
      query.beginTime = ''
      query.endTime = ''
    }
    const res = await listSubmissions(query)
    rows.value = res?.rows ?? []
    total.value = res?.total ?? 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // admin 角色识别：roles 含 admin 或 perm key 含 *:*:*
  isAdmin.value = (userStore.roles ?? []).some((r) => r === 'admin')
  void fetch()
})

function search(): void {
  query.pageNum = 1
  void fetch()
}

function reset(): void {
  query.templateKey = ''
  query.submitter = ''
  range.value = null
  query.pageNum = 1
  void fetch()
}

function open(row: FormSubmission): void {
  router.push({ name: 'FormSubmissionDetail', params: { id: String(row.id) } })
}
</script>

<template>
  <div class="form-submission-list">
    <h3>{{ isAdmin ? t('form.submission.listTitleAll') : t('form.submission.listTitle') }}</h3>

    <div class="filters">
      <el-input
        v-model="query.templateKey"
        :placeholder="t('form.submission.search.tpl')"
        clearable
        style="width: 200px"
      />
      <el-input
        v-if="isAdmin"
        v-model="query.submitter"
        :placeholder="t('form.submission.search.submitter')"
        clearable
        style="width: 180px"
      />
      <el-date-picker
        v-model="range"
        type="datetimerange"
        :range-separator="' - '"
        :start-placeholder="t('form.submission.search.range')"
        :end-placeholder="t('form.submission.search.range')"
        format="YYYY-MM-DD HH:mm:ss"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 360px"
      />
      <el-button
        type="primary"
        @click="search"
      >
        {{ $t('common.search') }}
      </el-button>
      <el-button @click="reset">
        {{ $t('common.reset') }}
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      border
    >
      <el-table-column
        prop="id"
        label="ID"
        width="90"
        align="center"
      />
      <el-table-column
        :label="t('form.submission.colTpl')"
        min-width="200"
      >
        <template #default="{ row }">
          <span>{{ row.templateKey }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('form.submission.colSubmitter')"
        width="180"
      >
        <template #default="{ row }">
          <span>{{ row.submitterName ?? row.submitter }}</span>
          <span class="username-meta">@{{ row.submitter }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('form.submission.colVersion')"
        prop="templateVersion"
        width="100"
        align="center"
      />
      <el-table-column
        :label="t('form.submission.colTime')"
        prop="createTime"
        width="180"
      />
      <el-table-column
        :label="t('form.submission.colAction')"
        width="100"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            size="small"
            link
            type="primary"
            @click="open(row)"
          >
            {{ t('form.submission.actionView') }}
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
      @current-change="fetch"
      @size-change="fetch"
    />
  </div>
</template>

<style scoped lang="scss">
.form-submission-list {
  padding: 16px;
  h3 {
    margin: 0 0 12px;
  }
  .filters {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
    align-items: center;
  }
  .username-meta {
    margin-left: 6px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
  .pager {
    margin-top: 12px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
