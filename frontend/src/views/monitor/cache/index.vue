<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElButton, ElDescriptions, ElDescriptionsItem } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getCacheInfo, type CacheInfo } from '@/api/monitor/cache'

const loading = ref(false)
const info = ref<CacheInfo>({})

async function fetchInfo() {
  loading.value = true
  try {
    const res = (await getCacheInfo()) as { data?: CacheInfo }
    info.value = res.data ?? {}
  } finally {
    loading.value = false
  }
}

onMounted(fetchInfo)
</script>

<template>
  <div
    v-loading="loading"
    class="scaffold-page cache-page"
  >
    <div class="cache-page__header scaffold-card">
      <div>
        <div class="cache-page__title">
          缓存监控
        </div>
        <div class="cache-page__subtitle">
          Redis 实例运行状态与命令统计
        </div>
      </div>
      <ElButton
        type="primary"
        :icon="Refresh"
        @click="fetchInfo"
      >
        刷新
      </ElButton>
    </div>

    <div class="cache-page__grid">
      <div class="scaffold-card">
        <h3>基础信息</h3>
        <ElDescriptions
          :column="1"
          border
        >
          <ElDescriptionsItem label="键总数">
            {{ info.dbSize ?? 0 }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="Redis 版本">
            {{ info.info?.redis_version }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="运行模式">
            {{ info.info?.redis_mode }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="操作系统">
            {{ info.info?.os }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="运行时长">
            {{ info.info?.uptime_in_days }} 天
          </ElDescriptionsItem>
          <ElDescriptionsItem label="已用内存">
            {{ info.info?.used_memory_human }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="峰值内存">
            {{ info.info?.used_memory_peak_human }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="客户端数量">
            {{ info.info?.connected_clients }}
          </ElDescriptionsItem>
        </ElDescriptions>
      </div>

      <div class="scaffold-card">
        <h3>命令统计</h3>
        <el-table
          :data="info.commandStats ?? []"
          border
          style="width: 100%"
          :max-height="500"
        >
          <el-table-column
            prop="name"
            label="命令"
            min-width="160"
          />
          <el-table-column
            prop="value"
            label="调用次数"
            width="160"
            align="right"
          />
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.cache-page {
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
  }

  &__subtitle {
    margin-top: 4px;
    font-size: 13px;
    color: #6b7280;
  }

  &__grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;

    @media (max-width: 1100px) {
      grid-template-columns: 1fr;
    }
  }
}
</style>
