<script setup lang="ts">
import { computed } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { usePermissionStore } from '@/stores/permission'
import { useAppStore } from '@/stores/app'

const permissionStore = usePermissionStore()
const appStore = useAppStore()
const route = useRoute()
const { t } = useI18n()

interface SidebarRoute extends Omit<RouteRecordRaw, 'children'> {
  meta?: { title?: string; icon?: string; hidden?: boolean; alwaysShow?: boolean }
  hidden?: boolean
  children?: SidebarRoute[]
  alwaysShow?: boolean
}

const visibleRoutes = computed<SidebarRoute[]>(() => {
  return permissionStore.sidebarRoutes
    .filter((r) => !(r as SidebarRoute).hidden && !r.meta?.hidden)
    .map(toSidebarRoute)
})

function toSidebarRoute(record: RouteRecordRaw): SidebarRoute {
  const sidebarRecord: SidebarRoute = { ...record } as SidebarRoute
  if (record.children?.length) {
    sidebarRecord.children = record.children
      .filter((c) => !(c as SidebarRoute).hidden && !c.meta?.hidden)
      .map(toSidebarRoute)
  }
  return sidebarRecord
}

const activeMenu = computed(() => route.meta?.activeMenu || route.path)

function resolvePath(parent: string, child: string): string {
  if (child.startsWith('http')) return child
  if (child.startsWith('/')) return child
  return `${parent.replace(/\/$/, '')}/${child}`
}

function displayTitle(title: string | undefined, fallback: string): string {
  if (!title) return fallback
  const translated = t(title)
  return translated === title ? title : translated
}
</script>

<template>
  <el-menu
    :default-active="activeMenu as string"
    :collapse="appStore.sidebarCollapsed"
    :background-color="'#1f2937'"
    :text-color="'#d1d5db'"
    :active-text-color="'#60a5fa'"
    router
    unique-opened
    class="scaffold-sidebar"
  >
    <template
      v-for="item in visibleRoutes"
      :key="item.path"
    >
      <template v-if="!item.children || item.children.length === 0">
        <el-menu-item :index="item.path">
          <span>{{ displayTitle(item.meta?.title, item.path) }}</span>
        </el-menu-item>
      </template>
      <template v-else>
        <el-sub-menu :index="item.path">
          <template #title>
            <span>{{ displayTitle(item.meta?.title, item.path) }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.path"
            :index="resolvePath(item.path, child.path)"
          >
            {{ displayTitle(child.meta?.title, child.path) }}
          </el-menu-item>
        </el-sub-menu>
      </template>
    </template>
  </el-menu>
</template>

<style scoped lang="scss">
.scaffold-sidebar {
  border-right: none;
  height: 100%;
  --el-menu-base-level-padding: 18px;
}
</style>
