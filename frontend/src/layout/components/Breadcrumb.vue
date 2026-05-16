<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'

const route = useRoute()
const { t } = useI18n()

interface Crumb {
  title: string
  path?: string
}

const crumbs = computed<Crumb[]>(() => {
  const matched = route.matched.filter((r) => r.meta?.title)
  return matched.map((r) => ({
    title: displayTitle(r.meta?.title as string | undefined, r.path),
    path: r.path
  }))
})

function displayTitle(title: string | undefined, fallback: string): string {
  if (!title) return fallback
  const translated = t(title)
  return translated === title ? title : translated
}
</script>

<template>
  <el-breadcrumb
    separator="/"
    class="scaffold-breadcrumb"
  >
    <el-breadcrumb-item
      v-for="(c, idx) in crumbs"
      :key="c.path"
      :to="idx === crumbs.length - 1 ? undefined : c.path"
    >
      {{ c.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<style scoped lang="scss">
.scaffold-breadcrumb {
  font-size: 13px;
  line-height: 56px;
}
</style>
