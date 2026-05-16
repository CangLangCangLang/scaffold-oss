import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getWebSocketBus, type ConnectionStatus, type PushMessage } from '@/utils/websocket'
import { ackAllInbox, ackInbox, listUnread, type InboxEntry } from './api'

export interface NotificationItem {
  /** 业务消息 ID（来自 PushMessage.id 或 inbox.messageId） */
  id: string
  /** inbox 主键，用于 ack；纯实时推送的消息没有这个字段 */
  inboxId?: number
  type: string
  payload: unknown
  timestamp: number
  read: boolean
  scope: 'USER' | 'TOPIC'
  topic?: string
}

const MAX_KEPT = 100

/**
 * 通知中心 store（属于 inbox 模块）。
 * <p>
 * 删除整个 modules/inbox 目录后这个 store 也随之消失，TopBar 上的铃铛不会渲染（因为 widget 也由模块注入）。
 */
export const useNotificationStore = defineStore('inboxNotification', () => {
  const status = ref<ConnectionStatus>('idle')
  const items = ref<NotificationItem[]>([])
  const subscribedTopics = ref<Set<string>>(new Set())
  const unreadCount = computed(() => items.value.filter((item) => !item.read).length)

  let unsubscribeUser: (() => void) | null = null
  const unsubscribeTopics = new Map<string, () => void>()

  function appendMessage(message: PushMessage, topic?: string) {
    if (items.value.find((item) => item.id === message.id)) return
    items.value.unshift({
      id: message.id,
      type: message.type,
      payload: message.payload,
      timestamp: message.timestamp,
      scope: message.scope,
      topic,
      read: false
    })
    if (items.value.length > MAX_KEPT) {
      items.value.length = MAX_KEPT
    }
  }

  function appendInboxEntry(entry: InboxEntry) {
    if (items.value.find((item) => item.id === entry.messageId)) return
    let payload: unknown = entry.payload
    if (typeof payload === 'string') {
      try { payload = JSON.parse(payload) } catch { /* keep as string */ }
    }
    items.value.unshift({
      id: entry.messageId,
      inboxId: entry.id,
      type: entry.type,
      payload,
      timestamp: new Date(entry.createdAt).getTime(),
      scope: entry.scope,
      read: false
    })
    if (items.value.length > MAX_KEPT) {
      items.value.length = MAX_KEPT
    }
  }

  async function loadUnreadFromInbox() {
    try {
      const res = await listUnread(MAX_KEPT)
      const list = res.data || []
      // 倒序追加，保证时间靠前的在底部、新消息在顶
      for (let i = list.length - 1; i >= 0; i--) appendInboxEntry(list[i])
    } catch (err) {
      console.warn('inbox 拉取未读失败', err)
    }
  }

  function connect() {
    const bus = getWebSocketBus({ onStatusChange: (next) => (status.value = next) })
    bus.connect()
    unsubscribeUser = bus.onUserMessage((message) => appendMessage(message))
    void loadUnreadFromInbox()
  }

  function disconnect() {
    unsubscribeUser?.()
    unsubscribeTopics.forEach((unsub) => unsub())
    unsubscribeTopics.clear()
    subscribedTopics.value.clear()
    unsubscribeUser = null
    items.value = []
    status.value = 'closed'
  }

  function subscribeTopic(topic: string) {
    if (subscribedTopics.value.has(topic)) return
    const bus = getWebSocketBus()
    const unsub = bus.onTopic(topic, (message) => appendMessage(message, topic))
    unsubscribeTopics.set(topic, unsub)
    subscribedTopics.value.add(topic)
  }

  function unsubscribeTopic(topic: string) {
    unsubscribeTopics.get(topic)?.()
    unsubscribeTopics.delete(topic)
    subscribedTopics.value.delete(topic)
  }

  async function markAllRead() {
    items.value.forEach((item) => (item.read = true))
    try {
      await ackAllInbox()
    } catch (err) {
      console.warn('inbox ack-all 失败', err)
    }
  }

  async function markAsRead(id: string) {
    const item = items.value.find((it) => it.id === id)
    if (!item) return
    item.read = true
    if (item.inboxId != null) {
      try {
        await ackInbox(item.inboxId)
      } catch (err) {
        console.warn('inbox ack 失败 id=', id, err)
      }
    }
  }

  function clear() {
    items.value = []
  }

  return {
    status,
    items,
    unreadCount,
    subscribedTopics,
    connect,
    disconnect,
    subscribeTopic,
    unsubscribeTopic,
    markAllRead,
    markAsRead,
    clear,
    loadUnreadFromInbox
  }
})
