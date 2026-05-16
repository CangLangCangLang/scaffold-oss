<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

interface VisitedTag {
  path: string
  fullPath: string
  title: string
  affix: boolean
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const tags = ref<VisitedTag[]>([])

const activePath = computed(() => route.path)

function addTag(target: RouteLocationNormalized) {
  if (!target.meta?.title) return
  if (target.meta?.hidden) return
  const existing = tags.value.find((tag) => tag.path === target.path)
  if (existing) {
    existing.fullPath = target.fullPath
    return
  }
  tags.value.push({
    path: target.path,
    fullPath: target.fullPath,
    title: displayTitle(target.meta?.title as string | undefined, target.path),
    affix: Boolean(target.meta?.affix)
  })
}

function displayTitle(title: string | undefined, fallback: string): string {
  if (!title) return fallback
  const translated = t(title)
  return translated === title ? title : translated
}

function closeTag(tag: VisitedTag) {
  if (tag.affix) return
  const index = tags.value.findIndex((t) => t.path === tag.path)
  if (index === -1) return
  tags.value.splice(index, 1)
  if (route.path === tag.path) {
    const fallback = tags.value[index - 1] || tags.value[index] || tags.value[0]
    router.push(fallback?.fullPath || '/')
  }
}

function closeOthers() {
  tags.value = tags.value.filter((tag) => tag.affix || tag.path === route.path)
}

function closeAll() {
  tags.value = tags.value.filter((tag) => tag.affix)
  if (!tags.value.some((tag) => tag.path === route.path)) {
    router.push(tags.value[0]?.fullPath || '/')
  }
}

watch(
  () => route.fullPath,
  () => addTag(route),
  { immediate: true }
)
</script>

<template>
  <div class="tags-view">
    <div class="tags-view__list">
      <router-link
        v-for="tag in tags"
        :key="tag.path"
        :to="tag.fullPath"
        class="tags-view__item"
        :class="{ 'tags-view__item--active': tag.path === activePath }"
      >
        <span>{{ tag.title }}</span>
        <el-icon
          v-if="!tag.affix"
          class="tags-view__close"
          @click.prevent="closeTag(tag)"
        >
          <Close />
        </el-icon>
      </router-link>
    </div>
    <div class="tags-view__actions">
      <el-button
        size="small"
        link
        @click="closeOthers"
      >
        关闭其它
      </el-button>
      <el-button
        size="small"
        link
        @click="closeAll"
      >
        全部关闭
      </el-button>
    </div>
  </div>
</template>

<script lang="ts">
import { Close } from '@element-plus/icons-vue'
export default { components: { Close } }
</script>

<style scoped lang="scss">
.tags-view {
  display: flex;
  align-items: center;
  height: 38px;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  gap: 12px;

  &__list {
    flex: 1;
    display: flex;
    gap: 6px;
    overflow-x: auto;
  }

  &__item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 0 10px;
    height: 26px;
    border: 1px solid #e5e7eb;
    border-radius: 4px;
    background: #f9fafb;
    color: #4b5563;
    font-size: 12px;
    text-decoration: none;
    white-space: nowrap;

    &--active {
      color: #fff;
      background: #2563eb;
      border-color: #1d4ed8;
    }
  }

  &__close {
    font-size: 12px;
    cursor: pointer;
  }

  &__actions {
    flex-shrink: 0;
  }
}
</style>
