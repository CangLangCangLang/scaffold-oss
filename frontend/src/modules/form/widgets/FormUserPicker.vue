<script setup lang="ts">
/**
 * 人员选择 widget。
 *
 * <p>对接现有 {@code /system/user/list} 端点，按 nickName / userName 模糊远程搜索；
 * 单选 / 多选两种模式（multiple prop）；返回值是 username 字符串（或 string[]）。
 *
 * <p>为什么不复用 user/index.vue 弹窗：
 * <ul>
 *   <li>表单内联控件需要轻量，不能用全屏选择器</li>
 *   <li>v-model 直接出 username，下游业务直接拿来当审批人 / 通知对象传给 backend</li>
 * </ul>
 */
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/utils/request'

const { t } = useI18n()

interface UserOption {
  userId: number
  userName: string
  nickName: string
  dept?: { deptName?: string }
}

const props = defineProps<{
  modelValue?: string | string[]
  multiple?: boolean
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: string | string[] | undefined): void }>()

const options = ref<UserOption[]>([])
const loading = ref(false)
const selected = ref<string | string[] | undefined>(props.modelValue)

watch(
  () => props.modelValue,
  (v) => {
    selected.value = v
    if (v) void loadInitial(v)
  },
  { immediate: true }
)

watch(selected, (v) => emit('update:modelValue', v))

interface UserListResp {
  rows?: UserOption[]
}

async function remoteSearch(query: string): Promise<void> {
  if (!query) {
    options.value = []
    return
  }
  loading.value = true
  try {
    const res = (await request.get<UserListResp, UserListResp>('/system/user/list', {
      params: { nickName: query, pageNum: 1, pageSize: 20 }
    })) as UserListResp
    options.value = res.rows ?? []
  } finally {
    loading.value = false
  }
}

async function loadInitial(v: string | string[]): Promise<void> {
  const usernames = Array.isArray(v) ? v : [v]
  if (!usernames.length) return
  loading.value = true
  try {
    const res = (await request.get<UserListResp, UserListResp>('/system/user/list', {
      params: { userName: usernames.join(','), pageNum: 1, pageSize: 50 }
    })) as UserListResp
    options.value = res.rows ?? []
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-select
    v-model="selected"
    :multiple="multiple"
    :placeholder="placeholder ?? t('form.widget.userPicker.placeholder')"
    :disabled="disabled"
    filterable
    remote
    :remote-method="remoteSearch"
    :loading="loading"
    style="width: 100%"
  >
    <el-option
      v-for="u in options"
      :key="u.userId"
      :value="u.userName"
      :label="`${u.nickName} (${u.userName})`"
    >
      <span class="user-row">
        <span class="user-name">{{ u.nickName }}</span>
        <span class="user-meta">@{{ u.userName }}<span v-if="u.dept?.deptName"> · {{ u.dept.deptName }}</span></span>
      </span>
    </el-option>
  </el-select>
</template>

<style scoped lang="scss">
.user-row {
  display: flex;
  align-items: center;
  gap: 8px;
  .user-name {
    font-weight: 500;
  }
  .user-meta {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
