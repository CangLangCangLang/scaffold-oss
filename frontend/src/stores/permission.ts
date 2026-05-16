import { defineStore } from 'pinia'
import { ref, type Ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { getRouters } from '@/api/login'
import type { RouterRecord } from '@/types/api'

export interface PermissionStoreShape {
  sidebarRoutes: Ref<RouteRecordRaw[]>
  dynamicRoutes: Ref<RouteRecordRaw[]>
  loaded: Ref<boolean>
  generateRoutes: () => Promise<RouteRecordRaw[]>
  reset: () => void
}

const moduleViews = import.meta.glob('@/views/**/*.vue')
const businessModuleViews = import.meta.glob('@/modules/*/views/**/*.vue')
const enterpriseModuleViews = import.meta.glob('@/enterprise/*/views/**/*.vue')

const componentAliases: Record<string, string> = {
  'system/auditlog/index': 'system/audit/index'
}

const redirectAliases: Record<string, string> = {
  'tool/build/index': '/workflow/form-designer'
}

function loadView(component?: string) {
  if (!component) return undefined
  if (component === 'Layout') return () => import('@/layout/index.vue')
  if (component === 'ParentView') return () => import('@/components/ParentView.vue')
  if (component === 'InnerLink') return () => import('@/layout/components/InnerLink.vue')
  const normalized = componentAliases[component] || component
  const appTarget = `/src/views/${normalized}.vue`
  if (moduleViews[appTarget]) {
    return moduleViews[appTarget] as () => Promise<unknown>
  }
  const parts = normalized.split('/')
  if (parts.length >= 2) {
    const [moduleName, ...viewParts] = parts
    const moduleTarget = `/src/modules/${moduleName}/views/${viewParts.join('/')}.vue`
    if (businessModuleViews[moduleTarget]) {
      return businessModuleViews[moduleTarget] as () => Promise<unknown>
    }
    const enterpriseTarget = `/src/enterprise/${moduleName}/views/${viewParts.join('/')}.vue`
    if (enterpriseModuleViews[enterpriseTarget]) {
      return enterpriseModuleViews[enterpriseTarget] as () => Promise<unknown>
    }
  }
  return () => import('@/views/error/404.vue')
}

function normalizeRoutePath(record: RouterRecord, root: boolean): string | null {
  const raw = (record.path || '').trim()
  if (!raw || raw === '#') {
    return record.children?.length ? '' : null
  }
  if (!root) return raw
  return raw.startsWith('/') ? raw : `/${raw}`
}

function buildRoute(record: RouterRecord, root = true): RouteRecordRaw | null {
  const path = normalizeRoutePath(record, root)
  if (path === null) return null
  const routeName = `Dyn_${path}_${record.component || 'menu'}`.replace(/[^A-Za-z0-9_]+/g, '_')
  const route: RouteRecordRaw = {
    path,
    name: routeName,
    redirect: redirectAliases[record.component || ''] || record.redirect,
    meta: { ...record.meta },
    children: []
  } as unknown as RouteRecordRaw
  const componentLoader = loadView(record.component)
  if (componentLoader) {
    (route as RouteRecordRaw).component = componentLoader
  }
  if (record.children?.length) {
    route.children = record.children
      .map((child) => buildRoute(child, false))
      .filter((child): child is RouteRecordRaw => child !== null)
    const redirect = route.redirect
    if ((!redirect || redirect === 'noRedirect') && route.children.length) {
      route.redirect = root
        ? `${path}/${route.children[0].path}`.replace(/\/+/g, '/')
        : route.children[0].path
    }
  }
  return route
}

export const usePermissionStore = defineStore('permission', (): PermissionStoreShape => {
  const sidebarRoutes = ref<RouteRecordRaw[]>([])
  const dynamicRoutes = ref<RouteRecordRaw[]>([])
  const loaded = ref(false)

  async function generateRoutes(): Promise<RouteRecordRaw[]> {
    const res = await getRouters()
    const data = (res.data || []) as RouterRecord[]
    const built = data.map((record) => buildRoute(record)).filter((route): route is RouteRecordRaw => route !== null)
    sidebarRoutes.value = built
    dynamicRoutes.value = built
    loaded.value = true
    return built
  }

  function reset() {
    sidebarRoutes.value = []
    dynamicRoutes.value = []
    loaded.value = false
  }

  return { sidebarRoutes, dynamicRoutes, loaded, generateRoutes, reset }
})
