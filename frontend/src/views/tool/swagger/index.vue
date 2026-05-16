<script setup lang="ts">
const swaggerUrl = `${import.meta.env.VITE_APP_BASE_API}/swagger-ui/index.html`
</script>

<template>
  <div class="iframe-page">
    <div class="iframe-page__hint">
      接口文档由 SpringDoc OpenAPI 生成，便于联调与排查。
    </div>
    <iframe
      :src="swaggerUrl"
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
    background: #ecfeff;
    color: #0e7490;
    padding: 8px 16px;
    font-size: 12px;
    border-bottom: 1px solid #a5f3fc;
  }

  &__frame {
    flex: 1;
    width: 100%;
    border: 0;
  }
}
</style>
