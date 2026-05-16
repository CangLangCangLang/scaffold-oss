import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import nprogress from 'nprogress'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { ElMessage } from 'element-plus'

nprogress.configure({ showSpinner: false })

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/sso/callback',
    name: 'SsoCallback',
    component: () => import('@/views/sso/callback.vue'),
    meta: { title: 'SSO 登录中', hidden: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', hidden: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '403', hidden: true }
  },
  {
    path: '/',
    name: 'RootLayout',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'dashboard', affix: true }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理' }
      },
      {
        path: 'system/dept',
        name: 'SystemDept',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理' }
      },
      {
        path: 'system/post',
        name: 'SystemPost',
        component: () => import('@/views/system/post/index.vue'),
        meta: { title: '岗位管理' }
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理' }
      },
      {
        path: 'system/dict-data/:dictType',
        name: 'SystemDictData',
        component: () => import('@/views/system/dict/data.vue'),
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '参数设置' }
      },
      {
        path: 'system/notice',
        name: 'SystemNotice',
        component: () => import('@/views/system/notice/index.vue'),
        meta: { title: '通知公告' }
      },
      {
        path: 'system/profile',
        name: 'SystemProfile',
        component: () => import('@/views/system/user/profile/index.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'monitor/online',
        name: 'MonitorOnline',
        component: () => import('@/views/monitor/online/index.vue'),
        meta: { title: '在线用户' }
      },
      {
        path: 'monitor/logininfor',
        name: 'MonitorLogininfor',
        component: () => import('@/views/monitor/logininfor/index.vue'),
        meta: { title: '登录日志' }
      },
      {
        path: 'monitor/operlog',
        name: 'MonitorOperlog',
        component: () => import('@/views/monitor/operlog/index.vue'),
        meta: { title: '操作日志' }
      },
      {
        path: 'system/audit/log',
        name: 'SystemAuditLog',
        component: () => import('@/views/system/audit/index.vue'),
        meta: { title: '操作审计' }
      },
      {
        path: 'monitor/server',
        name: 'MonitorServer',
        component: () => import('@/views/monitor/server/index.vue'),
        meta: { title: '服务器监控' }
      },
      {
        path: 'monitor/cache',
        name: 'MonitorCache',
        component: () => import('@/views/monitor/cache/index.vue'),
        meta: { title: '缓存监控' }
      },
      {
        path: 'monitor/cache-list',
        name: 'MonitorCacheList',
        component: () => import('@/views/monitor/cache/list.vue'),
        meta: { title: '缓存列表' }
      },
      {
        path: 'monitor/job',
        name: 'MonitorJob',
        component: () => import('@/views/monitor/job/index.vue'),
        meta: { title: '定时任务' }
      },
      {
        path: 'monitor/job-log',
        name: 'MonitorJobLog',
        component: () => import('@/views/monitor/job/log.vue'),
        meta: { title: '调度日志', activeMenu: '/monitor/job' }
      },
      {
        path: 'monitor/druid',
        name: 'MonitorDruid',
        component: () => import('@/views/monitor/druid/index.vue'),
        meta: { title: 'Druid 监控' }
      },
      {
        path: 'monitor/observability',
        redirect: '/monitor/observability-slow',
        meta: { title: '可观测性' }
      },
      {
        path: 'monitor/observability-slow',
        name: 'MonitorObservabilitySlow',
        component: () => import('@/views/monitor/observability/SlowRequestList.vue'),
        meta: { title: '慢请求列表' }
      },
      {
        path: 'monitor/observability-metrics',
        name: 'MonitorObservabilityMetrics',
        component: () => import('@/views/monitor/observability/BusinessMetrics.vue'),
        meta: { title: '业务指标' }
      },
      {
        path: 'monitor/observability-health',
        name: 'MonitorObservabilityHealth',
        component: () => import('@/views/monitor/observability/HealthDashboard.vue'),
        meta: { title: '健康检查' }
      },
      {
        path: 'tool/swagger',
        name: 'ToolSwagger',
        component: () => import('@/views/tool/swagger/index.vue'),
        meta: { title: '系统接口' }
      },
      {
        path: 'tool/gen',
        name: 'ToolGen',
        component: () => import('@/views/tool/gen/index.vue'),
        meta: { title: '代码生成' }
      },
      {
        path: 'tool/gen/edit/:tableId',
        name: 'ToolGenEdit',
        component: () => import('@/views/tool/gen/edit.vue'),
        meta: { title: '编辑生成配置', activeMenu: '/tool/gen' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_APP_PUBLIC_PATH || '/'),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

const WHITELIST = ['/login', '/404', '/403', '/sso/callback']

router.beforeEach(async (to, _from, next) => {
  nprogress.start()
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (!userStore.token) {
    if (WHITELIST.includes(to.path)) {
      next()
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
    nprogress.done()
    return
  }

  if (to.path === '/login') {
    next({ path: '/' })
    nprogress.done()
    return
  }

  if (!permissionStore.loaded) {
    try {
      if (!userStore.roles.length) {
        await userStore.fetchProfile()
      }
      const dynamicRoutes = await permissionStore.generateRoutes()
      dynamicRoutes.forEach((r) => router.addRoute(r))
      router.addRoute({
        path: '/:pathMatch(.*)*',
        redirect: '/404',
        meta: { hidden: true }
      })
      next({ ...to, replace: true })
    } catch (error) {
      console.error('动态路由初始化失败', error)
      ElMessage.error((error as Error).message || '加载用户信息失败')
      await userStore.logout()
      permissionStore.reset()
      next({ path: '/login', query: { redirect: to.fullPath } })
    } finally {
      nprogress.done()
    }
    return
  }

  next()
})

router.afterEach(() => {
  nprogress.done()
})

export default router
