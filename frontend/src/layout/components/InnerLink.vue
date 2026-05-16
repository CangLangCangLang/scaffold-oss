<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
const url = (route.meta?.link as string | undefined) ?? ''
</script>

<template>
  <iframe
    v-if="url"
    :src="url"
    class="inner-link"
    frameborder="0"
  />
</template>

<style scoped lang="scss">
.inner-link {
  width: 100%;
  height: calc(100vh - 56px - 38px);
  border: 0;
}
</style>
