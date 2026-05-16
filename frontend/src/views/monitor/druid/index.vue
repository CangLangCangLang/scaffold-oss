<script setup lang="ts">
const druidUrl = `${import.meta.env.VITE_APP_BASE_API}/druid/login.html`
</script>

<template>
  <div class="iframe-page">
    <div class="iframe-page__hint">
      Druid 监控由后端原生 Web 控制台提供，使用 <code>monitor / monitor</code> 默认账号（生产环境请通过环境变量重置）。
    </div>
    <iframe
      :src="druidUrl"
      class="iframe-page__frame"
    />
  </div>
</template>

<style scoped lang="scss">
.iframe-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px - 38px);

  &__hint {
    background: #fff7ed;
    color: #b45309;
    padding: 8px 16px;
    font-size: 12px;
    border-bottom: 1px solid #fed7aa;
  }

  &__frame {
    flex: 1;
    width: 100%;
    border: 0;
  }
}
</style>
