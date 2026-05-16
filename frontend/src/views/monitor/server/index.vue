<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElButton, ElDescriptions, ElDescriptionsItem, ElProgress } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getServer, type ServerInfo } from '@/api/monitor/server'

const loading = ref(false)
const data = ref<ServerInfo>({})

async function fetchData() {
  loading.value = true
  try {
    const res = (await getServer()) as { data?: ServerInfo }
    data.value = res.data ?? {}
  } finally {
    loading.value = false
  }
}

function fmtMb(n?: number): string {
  if (n == null) return '-'
  return `${n.toFixed(2)} GB`
}

onMounted(fetchData)
</script>

<template>
  <div
    v-loading="loading"
    class="scaffold-page server-page"
  >
    <div class="server-page__header scaffold-card">
      <div>
        <div class="server-page__title">
          服务器监控
        </div>
        <div class="server-page__subtitle">
          实时查看后台运行环境
        </div>
      </div>
      <ElButton
        type="primary"
        :icon="Refresh"
        @click="fetchData"
      >
        刷新
      </ElButton>
    </div>

    <div class="server-page__grid">
      <div class="scaffold-card">
        <h3>CPU</h3>
        <ElDescriptions
          :column="2"
          border
        >
          <ElDescriptionsItem label="核心数">
            {{ data.cpu?.cpuNum }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="用户使用率">
            {{ data.cpu?.used }}%
          </ElDescriptionsItem>
          <ElDescriptionsItem label="系统使用率">
            {{ data.cpu?.sys }}%
          </ElDescriptionsItem>
          <ElDescriptionsItem label="当前空闲率">
            {{ data.cpu?.free }}%
          </ElDescriptionsItem>
        </ElDescriptions>
      </div>

      <div class="scaffold-card">
        <h3>内存</h3>
        <ElDescriptions
          :column="2"
          border
        >
          <ElDescriptionsItem label="总内存">
            {{ fmtMb(data.mem?.total) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="已使用">
            {{ fmtMb(data.mem?.used) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="剩余">
            {{ fmtMb(data.mem?.free) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="使用率">
            <ElProgress
              :percentage="Number(data.mem?.usage ?? 0)"
              status="success"
            />
          </ElDescriptionsItem>
        </ElDescriptions>
      </div>

      <div class="scaffold-card">
        <h3>JVM</h3>
        <ElDescriptions
          :column="2"
          border
        >
          <ElDescriptionsItem label="JDK 版本">
            {{ data.jvm?.version }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="启动时间">
            {{ data.jvm?.startTime }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="运行时长">
            {{ data.jvm?.runTime }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="使用率">
            <ElProgress
              :percentage="Number(data.jvm?.usage ?? 0)"
              status="warning"
            />
          </ElDescriptionsItem>
          <ElDescriptionsItem
            label="安装路径"
            :span="2"
          >
            {{ data.jvm?.home }}
          </ElDescriptionsItem>
        </ElDescriptions>
      </div>

      <div class="scaffold-card">
        <h3>系统信息</h3>
        <ElDescriptions
          :column="2"
          border
        >
          <ElDescriptionsItem label="主机名">
            {{ data.sys?.computerName }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="主机 IP">
            {{ data.sys?.computerIp }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="操作系统">
            {{ data.sys?.osName }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="系统架构">
            {{ data.sys?.osArch }}
          </ElDescriptionsItem>
          <ElDescriptionsItem
            label="项目路径"
            :span="2"
          >
            {{ data.sys?.userDir }}
          </ElDescriptionsItem>
        </ElDescriptions>
      </div>

      <div class="scaffold-card server-page__disk">
        <h3>磁盘</h3>
        <el-table
          :data="data.sysFiles ?? []"
          border
          style="width: 100%"
        >
          <el-table-column
            prop="dirName"
            label="盘符路径"
            min-width="160"
          />
          <el-table-column
            prop="sysTypeName"
            label="文件系统"
            width="140"
          />
          <el-table-column
            prop="typeName"
            label="盘符类型"
            width="120"
          />
          <el-table-column
            prop="total"
            label="总大小"
            width="120"
          />
          <el-table-column
            prop="free"
            label="可用空间"
            width="120"
          />
          <el-table-column
            prop="used"
            label="已使用"
            width="120"
          />
          <el-table-column
            prop="usage"
            label="使用率"
            width="180"
          >
            <template #default="{ row }">
              <ElProgress :percentage="Number(row.usage ?? 0)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.server-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
  }

  &__subtitle {
    margin-top: 4px;
    font-size: 13px;
    color: #6b7280;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;

    @media (max-width: 1100px) {
      grid-template-columns: 1fr;
    }
  }

  &__disk {
    grid-column: 1 / -1;
  }
}
</style>
