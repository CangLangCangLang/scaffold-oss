import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import type { PushMessage } from '@/utils/websocket'

vi.mock('./api', () => ({
  listUnread: vi.fn(async () => ({ code: 200, msg: 'ok', data: [] })),
  countUnread: vi.fn(async () => ({ code: 200, msg: 'ok', data: { count: 0 } })),
  ackInbox: vi.fn(async () => ({ code: 200, msg: 'ok' })),
  ackAllInbox: vi.fn(async () => ({ code: 200, msg: 'ok', data: { count: 0 } }))
}))

import { useNotificationStore } from './store'

function buildMessage(partial: Partial<PushMessage> = {}): PushMessage {
  return {
    scope: 'USER',
    target: 'alice',
    type: 'notice',
    id: 'msg-' + Math.random(),
    payload: { hello: 'world' },
    timestamp: Date.now(),
    ...partial
  }
}

describe('useNotificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('appends new notifications to the top with default unread state', () => {
    const store = useNotificationStore()
    store.items.unshift({
      id: 'a',
      type: 'notice',
      payload: { ok: 1 },
      timestamp: Date.now(),
      read: false,
      scope: 'USER'
    })
    expect(store.unreadCount).toBe(1)
    expect(store.items[0].id).toBe('a')
  })

  it('markAllRead resets unread count', async () => {
    const store = useNotificationStore()
    store.items.push(
      { id: '1', type: 't', payload: 0, timestamp: 0, read: false, scope: 'USER' },
      { id: '2', type: 't', payload: 0, timestamp: 0, read: false, scope: 'USER' }
    )
    expect(store.unreadCount).toBe(2)
    await store.markAllRead()
    expect(store.unreadCount).toBe(0)
  })

  it('markAsRead updates only the matching record', async () => {
    const store = useNotificationStore()
    store.items.push(
      { id: '1', type: 't', payload: 0, timestamp: 0, read: false, scope: 'USER' },
      { id: '2', type: 't', payload: 0, timestamp: 0, read: false, scope: 'USER' }
    )
    await store.markAsRead('1')
    expect(store.items.find((it) => it.id === '1')?.read).toBe(true)
    expect(store.items.find((it) => it.id === '2')?.read).toBe(false)
  })

  it('clear empties the notification list', () => {
    const store = useNotificationStore()
    store.items.push({ id: '1', type: 't', payload: buildMessage(), timestamp: 0, read: false, scope: 'USER' })
    store.clear()
    expect(store.items).toHaveLength(0)
  })
})
