<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElDescriptions,
  ElDescriptionsItem,
  ElInput,
  ElMessage,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTabPane
} from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import { getGenTable, updateGenTable, type GenTableRecord } from '@/api/tool/gen'

interface GenColumn {
  columnId?: number
  columnName?: string
  columnComment?: string
  javaType?: string
  javaField?: string
  isPk?: string
  isInsert?: string
  isEdit?: string
  isList?: string
  isQuery?: string
  htmlType?: string
}

const route = useRoute()
const router = useRouter()
const tableInfo = ref<GenTableRecord>({})
const columns = ref<GenColumn[]>([])
const tabKey = ref('basic')

async function loadTable() {
  const id = Number(route.params.tableId)
  if (!Number.isFinite(id)) {
    ElMessage.error('无效的表 ID')
    router.replace('/tool/gen')
    return
  }
  const res = (await getGenTable(id)) as { data?: GenTableRecord; info?: GenTableRecord; rows?: GenColumn[] }
  tableInfo.value = (res.info ?? res.data ?? {}) as GenTableRecord
  columns.value = (res.rows ?? []) as GenColumn[]
}

async function handleSave() {
  await updateGenTable({ ...tableInfo.value, ...{ columns: columns.value } } as GenTableRecord)
  ElMessage.success('已保存')
}

onMounted(loadTable)
</script>

<template>
  <div class="scaffold-page">
    <div class="scaffold-card">
      <div class="scaffold-toolbar">
        <el-button
          :icon="Back"
          @click="router.push('/tool/gen')"
        >
          返回
        </el-button>
        <el-button
          type="primary"
          @click="handleSave"
        >
          保存
        </el-button>
      </div>
      <ElTabs v-model="tabKey">
        <ElTabPane
          label="基本信息"
          name="basic"
        >
          <ElDescriptions
            :column="2"
            border
          >
            <ElDescriptionsItem label="表名称">
              {{ tableInfo.tableName }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="表描述">
              {{ tableInfo.tableComment }}
            </ElDescriptionsItem>
            <ElDescriptionsItem label="实体名">
              <ElInput v-model="tableInfo.className" />
            </ElDescriptionsItem>
            <ElDescriptionsItem label="作者">
              <ElInput v-model="tableInfo.functionAuthor" />
            </ElDescriptionsItem>
            <ElDescriptionsItem label="包名">
              <ElInput v-model="tableInfo.packageName" />
            </ElDescriptionsItem>
            <ElDescriptionsItem label="模块">
              <ElInput v-model="tableInfo.moduleName" />
            </ElDescriptionsItem>
            <ElDescriptionsItem label="业务名">
              <ElInput v-model="tableInfo.businessName" />
            </ElDescriptionsItem>
            <ElDescriptionsItem label="功能名">
              <ElInput v-model="tableInfo.functionName" />
            </ElDescriptionsItem>
            <ElDescriptionsItem
              label="备注"
              :span="2"
            >
              <ElInput v-model="tableInfo.remark" />
            </ElDescriptionsItem>
          </ElDescriptions>
        </ElTabPane>
        <ElTabPane
          label="字段信息"
          name="columns"
        >
          <ElTable
            :data="columns"
            border
            :max-height="500"
          >
            <ElTableColumn
              prop="columnName"
              label="字段"
              min-width="160"
            />
            <ElTableColumn
              prop="columnComment"
              label="描述"
              min-width="180"
            >
              <template #default="{ row }">
                <ElInput
                  v-model="row.columnComment"
                  size="small"
                />
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="javaType"
              label="Java 类型"
              min-width="140"
            >
              <template #default="{ row }">
                <ElInput
                  v-model="row.javaType"
                  size="small"
                />
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="javaField"
              label="Java 字段"
              min-width="160"
            >
              <template #default="{ row }">
                <ElInput
                  v-model="row.javaField"
                  size="small"
                />
              </template>
            </ElTableColumn>
          </ElTable>
        </ElTabPane>
      </ElTabs>
    </div>
  </div>
</template>
