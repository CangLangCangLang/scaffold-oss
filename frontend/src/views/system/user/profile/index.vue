<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElTabs,
  ElTabPane,
  ElButton,
  ElMessage,
  type FormRules
} from 'element-plus'
import { getUserProfile, updateUserProfile, updateUserPwd } from '@/api/system/profile'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const profile = reactive({
  userName: '',
  nickName: '',
  email: '',
  phonenumber: '',
  sex: '0',
  remark: ''
})

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const profileLoading = ref(false)
const pwdLoading = ref(false)

const profileRules: FormRules = {
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phonenumber: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 5, max: 20, message: '密码长度 5~20', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, cb) => {
        if (value !== pwdForm.newPassword) cb(new Error('两次密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

async function loadProfile() {
  const res = (await getUserProfile()) as { data?: typeof profile }
  Object.assign(profile, res.data ?? {})
}

async function handleSave() {
  profileLoading.value = true
  try {
    await updateUserProfile(profile)
    ElMessage.success('保存成功')
    await userStore.fetchProfile()
  } finally {
    profileLoading.value = false
  }
}

async function handleChangePwd() {
  pwdLoading.value = true
  try {
    await updateUserPwd(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功，请妥善保管')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    pwdLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="scaffold-page profile-page">
    <ElCard>
      <ElTabs>
        <ElTabPane label="基本资料">
          <ElForm
            :model="profile"
            :rules="profileRules"
            label-width="100px"
            style="max-width: 480px"
          >
            <ElFormItem label="账号">
              <ElInput
                :model-value="profile.userName"
                disabled
              />
            </ElFormItem>
            <ElFormItem
              label="昵称"
              prop="nickName"
            >
              <ElInput v-model="profile.nickName" />
            </ElFormItem>
            <ElFormItem
              label="邮箱"
              prop="email"
            >
              <ElInput v-model="profile.email" />
            </ElFormItem>
            <ElFormItem
              label="手机号"
              prop="phonenumber"
            >
              <ElInput v-model="profile.phonenumber" />
            </ElFormItem>
            <ElFormItem label="备注">
              <ElInput
                v-model="profile.remark"
                type="textarea"
                :rows="2"
              />
            </ElFormItem>
            <ElFormItem>
              <ElButton
                type="primary"
                :loading="profileLoading"
                @click="handleSave"
              >
                保存
              </ElButton>
            </ElFormItem>
          </ElForm>
        </ElTabPane>
        <ElTabPane label="修改密码">
          <ElForm
            :model="pwdForm"
            :rules="pwdRules"
            label-width="100px"
            style="max-width: 480px"
          >
            <ElFormItem
              label="原密码"
              prop="oldPassword"
            >
              <ElInput
                v-model="pwdForm.oldPassword"
                type="password"
                show-password
              />
            </ElFormItem>
            <ElFormItem
              label="新密码"
              prop="newPassword"
            >
              <ElInput
                v-model="pwdForm.newPassword"
                type="password"
                show-password
              />
            </ElFormItem>
            <ElFormItem
              label="确认密码"
              prop="confirmPassword"
            >
              <ElInput
                v-model="pwdForm.confirmPassword"
                type="password"
                show-password
              />
            </ElFormItem>
            <ElFormItem>
              <ElButton
                type="primary"
                :loading="pwdLoading"
                @click="handleChangePwd"
              >
                修改密码
              </ElButton>
            </ElFormItem>
          </ElForm>
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </div>
</template>

<style scoped lang="scss">
.profile-page :deep(.el-card__body) {
  padding: 24px;
}
</style>
