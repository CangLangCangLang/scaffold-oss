/**
 * 后端可插拔模块状态 Store。
 *
 * <p>启动时通过 {@code /actuator/scaffold-modules} 异步获取后端实际加载的模块清单，
 * 业务侧通过 {@link isBackendModuleEnabled} 做条件渲染（如附件 tab、合同 BPMN 入口等）。
 *
 * <p>设计原则：
 * - 探测失败（后端未起 / 端点鉴权失败）时返回 {@code true}，**不阻断**任何 UI；
 * - sidebar 不依赖本 store（菜单仍由后端 RBAC 驱动，关模块时菜单自然消失）；
 * - 仅服务于"前端有按钮 / tab，后端可能没启该桥模块"这类弱关联场景。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import { getToken } from '@/utils/auth'

interface BackendModule {
  name: string
  version: string
  enabled: boolean
}

interface ModulesHealthDetails {
  count: number
  modules: BackendModule[]
}

interface ModulesHealthResponse {
  status?: string
  details?: ModulesHealthDetails
}

interface ModulesEndpointResponse {
  count?: number
  modules?: BackendModule[]
}

function extractModules(data: ModulesEndpointResponse | ModulesHealthResponse): BackendModule[] | undefined {
  return (data as ModulesEndpointResponse).modules || (data as ModulesHealthResponse).details?.modules
}

export const useModulesStore = defineStore('scaffoldModules', () => {
  // null 表示尚未探测过 / 探测失败 → 此时 isBackendModuleEnabled() 返回 true 兜底
  const known = ref<Set<string> | null>(null)
  const ready = ref(false)

  async function probe(): Promise<void> {
    try {
      // 直接用裸 axios，绕开 service 的 {code, data} 业务拦截器（actuator 走 spring boot 原生格式）
      const baseURL = (import.meta.env.VITE_APP_BASE_API as string) || ''
      const token = getToken()
      const headers = token ? { Authorization: `Bearer ${token}` } : undefined
      const rsp = await axios.get<ModulesEndpointResponse | ModulesHealthResponse>(
        `${baseURL}/actuator/scaffold-modules`,
        { timeout: 3000, headers }
      )
      const modules = extractModules(rsp.data)
      if (modules?.length) {
        known.value = new Set(modules.filter((m) => m.enabled).map((m) => m.name))
      } else {
        known.value = new Set()
      }
    } catch {
      // 静默失败：探测不可用时退化为「全部启用」，不影响任何业务流
      known.value = null
    } finally {
      ready.value = true
    }
  }

  /**
   * 判断后端是否加载了某个模块。
   * 探测尚未完成 / 探测失败时返回 true（允许 UI 正常渲染，避免误隐藏）。
   *
   * @param name 模块短名（与后端 ScaffoldModule.name 对齐：cms / cms-workflow / oa-inbox / ...）
   */
  function isBackendModuleEnabled(name: string): boolean {
    if (known.value === null) return true
    return known.value.has(name)
  }

  return { known, ready, probe, isBackendModuleEnabled }
})
