<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import VChart from 'vue-echarts'
import { useUserStore } from '@/stores/user'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
])

const userStore = useUserStore()
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '上午好'
  if (hour < 13) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const stats = ref([
  { label: '今日活跃用户', value: 1280, delta: '+8.3%', tone: 'positive' },
  { label: '本周接口调用', value: 51_240, delta: '+12.4%', tone: 'positive' },
  { label: '错误率', value: 0.42, suffix: '%', delta: '-0.18%', tone: 'positive' },
  { label: '在线告警', value: 3, delta: '关注中', tone: 'warning' }
])

const trendOption = shallowRef<EChartsOption>()
const distributionOption = shallowRef<EChartsOption>()

onMounted(() => {
  trendOption.value = {
    grid: { left: 30, right: 16, top: 30, bottom: 24 },
    legend: { data: ['访问量', '业务请求'] },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '访问量',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.18 },
        data: [820, 932, 901, 934, 1290, 1330, 1320]
      },
      {
        name: '业务请求',
        type: 'line',
        smooth: true,
        data: [620, 720, 700, 780, 880, 920, 880]
      }
    ]
  }

  distributionOption.value = {
    legend: { bottom: 0 },
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '调用占比',
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        data: [
          { name: '系统管理', value: 1048 },
          { name: '业务接口', value: 2735 },
          { name: '监控接口', value: 580 },
          { name: '工具接口', value: 484 }
        ]
      }
    ]
  }
})
</script>

<template>
  <div class="dashboard scaffold-page">
    <div class="dashboard__welcome scaffold-card">
      <div>
        <div class="dashboard__greeting">
          {{ greeting }}，{{ userStore.nickName || userStore.username || 'admin' }}
        </div>
        <div class="dashboard__hint">
          这里是 Vue 3 + Vite + TypeScript 重写的脚手架仪表盘，业务页面将在后续 Stage 接入。
        </div>
      </div>
      <div class="dashboard__roles">
        <el-tag
          v-for="role in userStore.roles"
          :key="role"
          type="primary"
        >
          {{ role }}
        </el-tag>
      </div>
    </div>

    <div class="dashboard__stats">
      <div
        v-for="item in stats"
        :key="item.label"
        class="scaffold-card dashboard__stat"
      >
        <div class="dashboard__stat-label">
          {{ item.label }}
        </div>
        <div class="dashboard__stat-value">
          <span>{{ item.value }}</span>
          <span
            v-if="item.suffix"
            class="dashboard__stat-unit"
          >{{ item.suffix }}</span>
        </div>
        <div :class="['dashboard__stat-delta', `dashboard__stat-delta--${item.tone}`]">
          {{ item.delta }}
        </div>
      </div>
    </div>

    <div class="dashboard__charts">
      <div class="scaffold-card dashboard__chart-card">
        <div class="dashboard__chart-title">
          近 7 天调用趋势
        </div>
        <VChart
          v-if="trendOption"
          class="dashboard__chart"
          :option="trendOption"
          autoresize
        />
      </div>
      <div class="scaffold-card dashboard__chart-card">
        <div class="dashboard__chart-title">
          业务模块分布
        </div>
        <VChart
          v-if="distributionOption"
          class="dashboard__chart"
          :option="distributionOption"
          autoresize
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__welcome {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    background: linear-gradient(135deg, #2563eb, #0ea5e9);
    color: #fff;
  }

  &__greeting {
    font-size: 18px;
    font-weight: 600;
  }

  &__hint {
    margin-top: 4px;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.8);
  }

  &__roles {
    display: flex;
    gap: 6px;
  }

  &__stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;

    @media (max-width: 1100px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  &__stat-label {
    color: #6b7280;
    font-size: 13px;
  }

  &__stat-value {
    margin: 6px 0;
    font-size: 24px;
    font-weight: 600;
    color: #1f2937;
  }

  &__stat-unit {
    margin-left: 4px;
    font-size: 14px;
    color: #6b7280;
  }

  &__stat-delta {
    font-size: 12px;
    color: #6b7280;
    &--positive {
      color: #10b981;
    }
    &--warning {
      color: #f59e0b;
    }
  }

  &__charts {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 16px;

    @media (max-width: 1100px) {
      grid-template-columns: 1fr;
    }
  }

  &__chart-card {
    display: flex;
    flex-direction: column;
  }

  &__chart-title {
    font-size: 14px;
    color: #1f2937;
    margin-bottom: 8px;
    font-weight: 600;
  }

  &__chart {
    height: 300px;
  }
}
</style>
