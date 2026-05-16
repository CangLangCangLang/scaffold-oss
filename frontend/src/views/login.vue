<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User, Picture } from '@element-plus/icons-vue'
import { getCaptcha, listSsoProviders, type SsoProvider } from '@/api/login'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const appTitle = import.meta.env.VITE_APP_TITLE || 'Fullstack Scaffold'

const formRef = ref<FormInstance>()
const submitting = ref(false)
const captchaImg = ref('')
const captchaUuid = ref('')
const captchaEnabled = ref(true)
const ssoProviders = ref<SsoProvider[]>([])
const backendOrigin = (import.meta.env.VITE_BACKEND_ORIGIN as string | undefined) || ''

const form = reactive({
  username: 'admin',
  password: 'admin123',
  code: '',
  uuid: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    const data = res.data || (res as Record<string, unknown>)
    captchaEnabled.value = (data.captchaEnabled as boolean | undefined) ?? true
    captchaImg.value = `data:image/gif;base64,${data.img}`
    captchaUuid.value = (data.uuid as string) ?? ''
    form.uuid = captchaUuid.value
  } catch (e) {
    console.warn('验证码加载失败', e)
  }
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await userStore.performLogin({
        username: form.username,
        password: form.password,
        code: form.code,
        uuid: form.uuid
      })
      permissionStore.reset()
      const target = (route.query.redirect as string) || '/'
      await router.replace(target)
      ElMessage.success('登录成功')
    } catch (error) {
      console.warn('登录失败', error)
      await refreshCaptcha()
    } finally {
      submitting.value = false
    }
  })
}

async function loadSsoProviders() {
  try {
    const res = await listSsoProviders()
    ssoProviders.value = res.data || []
  } catch (e) {
    console.warn('加载 SSO 入口失败', e)
  }
}

function startSsoLogin(provider: SsoProvider) {
  // /oauth2/authorization/<id> 由 Spring Security 处理后会 302 到 IDP，
  // 登录成功 / 失败回到 SsoAuthenticationHandlers 配置的回调路径。
  const target = (backendOrigin || '') + provider.authorizationUri
  window.location.assign(target)
}

onMounted(() => {
  const ssoError = route.query.ssoError as string | undefined
  if (ssoError) ElMessage.error(`第三方登录失败：${ssoError}`)
  refreshCaptcha()
  loadSsoProviders()
})
</script>

<template>
  <div class="login">
    <div class="login__panel">
      <div class="login__brand">
        <div class="login__logo" />
        <h1 class="login__title">
          {{ appTitle }}
        </h1>
        <p class="login__subtitle">
          Vue 3 · Vite · TypeScript · Element Plus
        </p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        class="login__form"
        @keyup.enter="onSubmit"
      >
        <el-form-item
          label="账号"
          prop="username"
        >
          <el-input
            v-model="form.username"
            :prefix-icon="User"
            placeholder="请输入账号"
          />
        </el-form-item>
        <el-form-item
          label="密码"
          prop="password"
        >
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item
          v-if="captchaEnabled"
          label="验证码"
          prop="code"
        >
          <div class="login__captcha">
            <el-input
              v-model="form.code"
              :prefix-icon="Picture"
              placeholder="请输入验证码"
              maxlength="6"
            />
            <img
              v-if="captchaImg"
              :src="captchaImg"
              class="login__captcha-img"
              alt="验证码"
              @click="refreshCaptcha"
            >
          </div>
        </el-form-item>
        <el-button
          type="primary"
          :loading="submitting"
          class="login__submit"
          @click="onSubmit"
        >
          登 录
        </el-button>
      </el-form>
      <div
        v-if="ssoProviders.length"
        class="login__sso"
      >
        <div class="login__sso-divider">
          <span>或使用第三方账号登录</span>
        </div>
        <div class="login__sso-buttons">
          <el-button
            v-for="provider in ssoProviders"
            :key="provider.id"
            class="login__sso-btn"
            @click="startSsoLogin(provider)"
          >
            {{ provider.label }}
          </el-button>
        </div>
      </div>
      <div class="login__footer">
        默认账号：<strong>admin / admin123</strong>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #312e81 50%, #4338ca 100%);
  padding: 24px;

  &__panel {
    width: 100%;
    max-width: 420px;
    background: #ffffffeb;
    backdrop-filter: blur(8px);
    border-radius: 14px;
    padding: 36px 32px 28px;
    box-shadow: 0 30px 60px rgba(15, 23, 42, 0.25);
  }

  &__brand {
    text-align: center;
    margin-bottom: 28px;
  }

  &__logo {
    width: 56px;
    height: 56px;
    margin: 0 auto 12px;
    border-radius: 14px;
    background: linear-gradient(135deg, #3b82f6, #0ea5e9);
    box-shadow: 0 12px 24px rgba(59, 130, 246, 0.3);
  }

  &__title {
    margin: 0 0 6px;
    font-size: 20px;
    color: #1e293b;
  }

  &__subtitle {
    margin: 0;
    font-size: 12px;
    color: #64748b;
    letter-spacing: 0.05em;
  }

  &__form {
    :deep(.el-form-item) {
      margin-bottom: 18px;
    }
  }

  &__captcha {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
  }

  &__captcha-img {
    height: 38px;
    width: 110px;
    border-radius: 6px;
    cursor: pointer;
    background: #f1f5f9;
  }

  &__submit {
    width: 100%;
    margin-top: 4px;
  }

  &__sso {
    margin-top: 18px;
  }

  &__sso-divider {
    text-align: center;
    color: #94a3b8;
    font-size: 12px;
    position: relative;
    margin: 8px 0 12px;
    span {
      background: transparent;
      padding: 0 12px;
    }
    &::before, &::after {
      content: '';
      position: absolute;
      top: 50%;
      width: 30%;
      height: 1px;
      background: #cbd5e1;
    }
    &::before { left: 0; }
    &::after { right: 0; }
  }

  &__sso-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
  }

  &__sso-btn {
    flex: 1 1 calc(50% - 8px);
    min-width: 120px;
  }

  &__footer {
    margin-top: 18px;
    text-align: center;
    font-size: 12px;
    color: #6b7280;
  }
}
</style>
