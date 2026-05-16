<script setup lang="ts">
/**
 * ShareDialog：分享链接创建对话框。
 *
 * <p>用户填：过期天数（0/空 = 永久）/ 一次性 / 可选密码。
 * 提交后展示生成的 token + 拼好的访问 URL，可一键复制。
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { createShare, shareAccessUrl, type SysFile, type SysFileShare } from '../api'

const props = defineProps<{ file: SysFile | null }>()
const visible = defineModel<boolean>({ default: false })
const { t } = useI18n()

const form = reactive<{ expireDays?: number; oneTime: '0' | '1'; password: string }>({
  expireDays: 7,
  oneTime: '0',
  password: ''
})
const result = ref<SysFileShare | null>(null)
const loading = ref(false)

watch(visible, (open) => {
  if (open) {
    form.expireDays = 7
    form.oneTime = '0'
    form.password = ''
    result.value = null
  }
})

async function submit(): Promise<void> {
  if (!props.file) return
  loading.value = true
  try {
    const res = await createShare({
      fileId: props.file.id,
      expireDays: form.expireDays,
      oneTime: form.oneTime,
      password: form.password.trim() || undefined
    })
    result.value = (res as unknown as { data: SysFileShare }).data
    ElMessage.success(t('file.share.createOk'))
  } finally {
    loading.value = false
  }
}

const fullShareUrl = computed<string>(() => {
  if (!result.value) return ''
  return window.location.origin + shareAccessUrl(result.value.token, form.password || undefined)
})

function copyLink(): void {
  if (!fullShareUrl.value) return
  void navigator.clipboard.writeText(fullShareUrl.value).then(() => {
    ElMessage.success(t('file.share.copied'))
  })
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('file.share.title', { name: file?.name || '' })"
    width="520px"
    :close-on-click-modal="false"
  >
    <el-form
      v-if="!result"
      :model="form"
      label-width="100px"
    >
      <el-form-item :label="t('file.share.expireDays')">
        <el-input-number
          v-model="form.expireDays"
          :min="0"
          :max="365"
          style="width: 100%"
        />
        <div class="tip">
          {{ t('file.share.expireTip') }}
        </div>
      </el-form-item>
      <el-form-item :label="t('file.share.oneTime')">
        <el-switch
          :model-value="form.oneTime === '1'"
          @update:model-value="(v) => (form.oneTime = v ? '1' : '0')"
        />
        <div class="tip">
          {{ t('file.share.oneTimeTip') }}
        </div>
      </el-form-item>
      <el-form-item :label="t('file.share.password')">
        <el-input
          v-model="form.password"
          show-password
          :placeholder="t('file.share.passwordPh')"
        />
      </el-form-item>
    </el-form>

    <div
      v-else
      class="result"
    >
      <p class="meta">
        {{ t('file.share.createdAt', { time: result.createTime || '' }) }}
      </p>
      <el-input
        :model-value="fullShareUrl"
        readonly
      >
        <template #append>
          <el-button @click="copyLink">
            {{ t('file.share.copy') }}
          </el-button>
        </template>
      </el-input>
      <p
        v-if="form.password"
        class="meta"
      >
        {{ t('file.share.passwordReminder') }}
      </p>
    </div>

    <template #footer>
      <el-button @click="visible = false">
        {{ t('file.share.close') }}
      </el-button>
      <el-button
        v-if="!result"
        type="primary"
        :loading="loading"
        @click="submit"
      >
        {{ t('file.share.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.tip {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
}
.result .meta {
  font-size: 12px;
  color: #888;
  margin: 8px 0;
}
</style>
