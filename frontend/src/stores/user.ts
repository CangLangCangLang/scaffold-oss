import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getInfo, login, logout, type LoginPayload } from '@/api/login'
import { getToken, removeToken, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userId = ref<number | undefined>(undefined)
  const username = ref('')
  const nickName = ref('')
  const avatar = ref('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const isDefaultModifyPwd = ref(false)
  const isPasswordExpired = ref(false)

  function bootstrapToken() {
    const stored = getToken()
    if (stored) token.value = stored
  }

  async function performLogin(payload: LoginPayload) {
    const res = await login(payload)
    const fresh = (res.data?.token || (res as unknown as { token?: string }).token) as string
    if (!fresh) throw new Error('登录响应缺少 token')
    setToken(fresh)
    token.value = fresh
  }

  async function fetchProfile() {
    const res = await getInfo()
    const payload = res.data || (res as unknown as Record<string, unknown>)
    const user = payload.user as Record<string, unknown> | undefined
    userId.value = (user?.userId as number | undefined) ?? undefined
    username.value = (user?.userName as string | undefined) ?? ''
    nickName.value = (user?.nickName as string | undefined) ?? ''
    avatar.value = (user?.avatar as string | undefined) ?? ''
    roles.value = (payload.roles as string[]) ?? []
    permissions.value = (payload.permissions as string[]) ?? []
    isDefaultModifyPwd.value = Boolean(payload.isDefaultModifyPwd)
    isPasswordExpired.value = Boolean(payload.isPasswordExpired)
    return payload
  }

  async function performLogout() {
    try {
      await logout()
    } catch {
      // 忽略：即便后端登出失败，本地也要清理
    }
    reset()
  }

  function reset() {
    token.value = ''
    userId.value = undefined
    username.value = ''
    nickName.value = ''
    avatar.value = ''
    roles.value = []
    permissions.value = []
    removeToken()
  }

  return {
    token,
    userId,
    username,
    nickName,
    avatar,
    roles,
    permissions,
    isDefaultModifyPwd,
    isPasswordExpired,
    bootstrapToken,
    performLogin,
    fetchProfile,
    logout: performLogout,
    reset
  }
})
