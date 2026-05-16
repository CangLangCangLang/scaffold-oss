<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { setToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

onMounted(async () => {
  const token = route.query.token as string | undefined
  const ssoError = route.query.ssoError as string | undefined
  if (ssoError) {
    ElMessage.error(`第三方登录失败：${ssoError}`)
    await router.replace('/login')
    return
  }
  if (!token) {
    ElMessage.error('未在回调中拿到 token')
    await router.replace('/login')
    return
  }
  setToken(token)
  userStore.bootstrapToken()
  permissionStore.reset()
  ElMessage.success('SSO 登录成功')
  await router.replace('/')
})
</script>

<template>
  <div class="sso-callback">
    <div class="sso-callback__panel">
      <div class="sso-callback__spinner" />
      <p>正在完成第三方登录…</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.sso-callback {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #312e81 50%, #4338ca 100%);

  &__panel {
    background: #ffffffeb;
    backdrop-filter: blur(8px);
    padding: 32px 48px;
    border-radius: 12px;
    text-align: center;
    color: #1e293b;
  }

  &__spinner {
    margin: 0 auto 16px;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    border: 3px solid #c7d2fe;
    border-top-color: #4338ca;
    animation: spin 0.8s linear infinite;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
