<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { ConnectionStatus } from '@/utils/websocket'
import { useNotificationStore } from '../store'

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const router = useRouter()
const { t } = useI18n()

const popoverVisible = ref(false)

function gotoFullPage() {
  popoverVisible.value = false
  router.push('/system/message/inbox')
}

const statusLabel = computed(() => {
  const labels: Record<ConnectionStatus, string> = {
    idle: t('notification.statusClosed'),
    connecting: t('notification.statusConnecting'),
    connected: t('notification.statusConnected'),
    reconnecting: t('notification.statusReconnecting'),
    closed: t('notification.statusClosed'),
    error: t('notification.statusError')
  }
  return labels[notificationStore.status]
})

const statusType = computed(() => {
  switch (notificationStore.status) {
    case 'connected':
      return 'success'
    case 'connecting':
    case 'reconnecting':
      return 'warning'
    case 'error':
      return 'danger'
    default:
      return 'info'
  }
})

watch(
  () => userStore.token,
  (token) => {
    if (token) notificationStore.connect()
    else notificationStore.disconnect()
  },
  { immediate: false }
)

onMounted(() => {
  if (userStore.token) notificationStore.connect()
})

onBeforeUnmount(() => {
  if (popoverVisible.value) popoverVisible.value = false
})

function formatTime(ts: number) {
  return new Date(ts).toLocaleTimeString()
}

function describePayload(payload: unknown) {
  if (payload == null) return ''
  if (typeof payload === 'string') return payload
  try {
    return JSON.stringify(payload)
  } catch {
    return String(payload)
  }
}
</script>

<template>
  <el-popover
    v-model:visible="popoverVisible"
    placement="bottom-end"
    :width="320"
    trigger="click"
    :title="t('notification.title')"
  >
    <template #reference>
      <el-badge
        :hidden="notificationStore.unreadCount === 0"
        :value="notificationStore.unreadCount"
        :max="99"
      >
        <el-icon
          class="topbar__action"
          :title="t('notification.title')"
        >
          <Bell />
        </el-icon>
      </el-badge>
    </template>
    <div class="notice-toolbar">
      <el-tag
        :type="statusType"
        size="small"
      >
        {{ statusLabel }}
      </el-tag>
      <div>
        <el-button
          size="small"
          link
          :disabled="notificationStore.unreadCount === 0"
          @click="notificationStore.markAllRead()"
        >
          {{ t('notification.markAllRead') }}
        </el-button>
        <el-button
          size="small"
          link
          :disabled="notificationStore.items.length === 0"
          @click="notificationStore.clear()"
        >
          {{ t('notification.clear') }}
        </el-button>
      </div>
    </div>
    <el-empty
      v-if="notificationStore.items.length === 0"
      :description="t('notification.empty')"
      :image-size="80"
    />
    <ul
      v-else
      class="notice-list"
    >
      <li
        v-for="item in notificationStore.items"
        :key="item.id"
        :class="{ 'notice-item--unread': !item.read }"
        @click="notificationStore.markAsRead(item.id)"
      >
        <div class="notice-row">
          <span class="notice-type">{{ item.type }}</span>
          <span class="notice-time">{{ formatTime(item.timestamp) }}</span>
        </div>
        <div class="notice-payload">
          {{ describePayload(item.payload) }}
        </div>
        <div
          v-if="item.scope === 'TOPIC' && item.topic"
          class="notice-meta"
        >
          {{ `topic: ${item.topic}` }}
        </div>
      </li>
    </ul>
    <div class="notice-footer">
      <el-button
        size="small"
        link
        type="primary"
        @click="gotoFullPage"
      >
        {{ t('inbox.full.bell.viewAll') }}
      </el-button>
    </div>
  </el-popover>
</template>

<style scoped lang="scss">
.notice-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.notice-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 360px;
  overflow-y: auto;

  li {
    cursor: pointer;
    padding: 8px 4px;
    border-bottom: 1px dashed var(--el-border-color-lighter);
    font-size: 13px;

    &:last-child {
      border-bottom: 0;
    }
  }
}

.notice-item--unread {
  background: var(--el-color-primary-light-9);
}

.notice-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  color: var(--el-text-color-primary);
}

.notice-type {
  font-weight: 600;
}

.notice-time {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.notice-payload {
  color: var(--el-text-color-regular);
  word-break: break-all;
  line-height: 1.5;
}

.notice-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.notice-footer {
  display: flex;
  justify-content: center;
  padding: 8px 0 4px;
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: 4px;
}

:global(.topbar__action) {
  cursor: pointer;
  font-size: 18px;
  color: #4b5563;
}
:global(.topbar__action:hover) {
  color: #2563eb;
}
</style>
