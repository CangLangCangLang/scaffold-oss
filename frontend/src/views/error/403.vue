<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <div class="error-page__code">
      403
    </div>
    <div class="error-page__msg">
      没有权限访问该页面，请联系管理员授权
    </div>
    <el-button
      type="primary"
      @click="router.replace('/')"
    >
      返回首页
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #f9fafb;

  &__code {
    font-size: 96px;
    font-weight: 700;
    color: #d97706;
    line-height: 1;
  }

  &__msg {
    color: #6b7280;
  }
}
</style>
