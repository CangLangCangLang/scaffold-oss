import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
  AxiosHeaders
} from 'axios'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { saveAs } from 'file-saver'
import { getToken, removeToken } from '@/utils/auth'
import { resolveErrorMessage } from '@/utils/errorCode'
import type { ApiResult } from '@/types/api'

interface ScaffoldRequestConfig extends InternalAxiosRequestConfig {
  /** 是否携带 token，默认 true */
  isToken?: boolean
  /** 是否启用同步重复提交检测，默认 true */
  repeatSubmit?: boolean
  /** 重复提交判定窗口（毫秒），默认 1000 */
  interval?: number
}

declare module 'axios' {
  // eslint-disable-next-line @typescript-eslint/no-empty-interface
  interface AxiosRequestConfig {
    isToken?: boolean
    repeatSubmit?: boolean
    interval?: number
  }
}

const DEFAULT_TIMEOUT = 15_000

let isRelogin = false
let lastRequest: { url: string; data: string; time: number } | undefined

function tansParams(params: Record<string, unknown>): string {
  let result = ''
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue
    if (typeof value === 'object') {
      for (const [k, v] of Object.entries(value as Record<string, unknown>)) {
        if (v === undefined || v === null || v === '') continue
        result += `${encodeURIComponent(`${key}[${k}]`)}=${encodeURIComponent(String(v))}&`
      }
    } else {
      result += `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}&`
    }
  }
  return result
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: DEFAULT_TIMEOUT
})

service.interceptors.request.use(
  (config: ScaffoldRequestConfig) => {
    const headers = (config.headers ||= new AxiosHeaders()) as AxiosHeaders
    const isToken = config.isToken !== false
    if (getToken() && isToken) {
      headers.set('Authorization', `Bearer ${getToken()}`)
    }
    if (!headers.get('Content-Type')) {
      headers.set('Content-Type', 'application/json;charset=utf-8')
    }
    if (config.method?.toLowerCase() === 'get' && config.params) {
      const query = tansParams(config.params as Record<string, unknown>)
      if (query) {
        config.url = `${config.url}?${query.slice(0, -1)}`
        config.params = {}
      }
    }
    if (
      config.repeatSubmit !== false &&
      ['post', 'put'].includes(config.method?.toLowerCase() || '')
    ) {
      const dataStr = typeof config.data === 'object' ? JSON.stringify(config.data) : String(config.data ?? '')
      const interval = config.interval ?? 1000
      const now = Date.now()
      if (
        lastRequest &&
        lastRequest.url === config.url &&
        lastRequest.data === dataStr &&
        now - lastRequest.time < interval
      ) {
        return Promise.reject(new Error('请勿重复提交，请稍候再试'))
      }
      lastRequest = { url: config.url || '', data: dataStr, time: now }
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { request, data, headers } = response
    const traceId = (headers['x-trace-id'] || headers['X-Trace-Id']) as string | undefined
    if (traceId && response.config.headers) {
      (response.config.headers as AxiosHeaders).set('X-Trace-Id', traceId)
    }
    if (request.responseType === 'blob' || request.responseType === 'arraybuffer') {
      return data
    }
    const body = data as ApiResult
    const code = body?.code ?? 200
    const msg = resolveErrorMessage(code, body?.msg, body?.errorKey)
    if (code === 401) {
      if (!isRelogin) {
        isRelogin = true
        ElMessageBox.confirm('登录状态已过期，您可以继续留在当前页，或重新登录', '系统提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        })
          .then(() => {
            isRelogin = false
            removeToken()
            window.location.href = '/login'
          })
          .catch(() => {
            isRelogin = false
          })
      }
      return Promise.reject(new Error('无效的会话或会话已过期'))
    }
    if (code === 500) {
      ElMessage({ message: msg, type: 'error' })
      return Promise.reject(new Error(msg))
    }
    if (code === 601) {
      ElMessage({ message: msg, type: 'warning' })
      return Promise.reject(new Error(msg))
    }
    if (code !== 200) {
      ElNotification.error({ title: msg, message: traceId ? `traceId: ${traceId}` : '' })
      return Promise.reject(new Error(msg))
    }
    return body
  },
  (error: AxiosError) => {
    let message = error.message
    if (message === 'Network Error') message = '后端接口连接异常'
    else if (message.includes('timeout')) message = '系统接口请求超时'
    else if (message.startsWith('Request failed with status code')) {
      message = `系统接口 ${message.slice(-3)} 异常`
    }
    ElMessage({ message, type: 'error', duration: 5000 })
    return Promise.reject(error)
  }
)

export async function downloadFile(
  url: string,
  params: Record<string, unknown>,
  filename: string
): Promise<void> {
  try {
    const blob = (await service.post(url, params, {
      transformRequest: [(p) => tansParams(p as Record<string, unknown>)],
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      responseType: 'blob',
      repeatSubmit: false
    })) as unknown as Blob
    saveAs(new Blob([blob]), filename)
  } catch (error) {
    ElMessage.error('下载文件出现错误，请联系管理员')
    throw error
  }
}

export default service
