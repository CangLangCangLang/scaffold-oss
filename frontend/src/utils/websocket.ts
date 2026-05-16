import { Client, type IFrame, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { getToken } from '@/utils/auth'

export interface PushMessage<T = unknown> {
  scope: 'USER' | 'TOPIC'
  target: string
  type: string
  id: string
  payload: T
  timestamp: number
}

export type ConnectionStatus = 'idle' | 'connecting' | 'connected' | 'reconnecting' | 'closed' | 'error'

export interface WebSocketBusOptions {
  /** 后端 STOMP endpoint，默认 /ws */
  endpoint?: string
  /** 重连间隔，默认 5000ms */
  reconnectDelay?: number
  /** 心跳间隔（双向），默认 10000ms */
  heartbeat?: number
  onStatusChange?: (status: ConnectionStatus, frame?: IFrame) => void
  debug?: boolean
}

type Handler<T = unknown> = (message: PushMessage<T>) => void

interface SubscriptionRecord {
  destination: string
  handler: Handler
  stompSub: StompSubscription | null
}

/**
 * 浏览器侧 STOMP 客户端封装：
 * - 与后端 com.scaffold.framework.web.websocket 对齐：用户队列 /user/queue/notice，主题 /topic/<name>
 * - 自带断线重连，重连后自动重新订阅之前的 destination
 * - 在握手 URL 上携带 token 参数，与后端 JwtHandshakeInterceptor 协同
 */
export class WebSocketBus {
  private client: Client | null = null
  private status: ConnectionStatus = 'idle'
  private subscriptions = new Map<string, SubscriptionRecord[]>()
  private readonly options: Required<Omit<WebSocketBusOptions, 'onStatusChange' | 'debug'>> &
    Pick<WebSocketBusOptions, 'onStatusChange' | 'debug'>

  constructor(options: WebSocketBusOptions = {}) {
    this.options = {
      endpoint: options.endpoint ?? '/ws',
      reconnectDelay: options.reconnectDelay ?? 5000,
      heartbeat: options.heartbeat ?? 10000,
      onStatusChange: options.onStatusChange,
      debug: options.debug ?? false
    }
  }

  getStatus(): ConnectionStatus {
    return this.status
  }

  /** 连接（幂等）。多次调用只会建立一条会话；token 取自 cookie。 */
  connect(): void {
    if (this.client?.active) return
    const token = getToken()
    if (!token) {
      this.transitionTo('error')
      throw new Error('No token available, login first before subscribing.')
    }
    const baseUrl = this.resolveBaseUrl()
    const wsUrl = `${baseUrl}${this.options.endpoint}?token=${encodeURIComponent(token)}`

    this.client = new Client({
      brokerURL: wsUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: this.options.reconnectDelay,
      heartbeatIncoming: this.options.heartbeat,
      heartbeatOutgoing: this.options.heartbeat,
      debug: this.options.debug ? (msg) => console.debug('[stomp]', msg) : () => {}
    })

    this.client.onConnect = (frame) => {
      this.transitionTo('connected', frame)
      this.subscriptions.forEach((records, destination) => {
        records.forEach((record) => this.bindStompSubscription(destination, record))
      })
    }
    this.client.onWebSocketClose = () => {
      if (this.status !== 'closed') this.transitionTo('reconnecting')
    }
    this.client.onStompError = (frame) => this.transitionTo('error', frame)

    this.transitionTo('connecting')
    this.client.activate()
  }

  /** 显式断开（不会触发重连）。 */
  disconnect(): void {
    if (!this.client) return
    void this.client.deactivate()
    this.client = null
    this.subscriptions.clear()
    this.transitionTo('closed')
  }

  /** 订阅当前用户的私信通道。 */
  onUserMessage<T = unknown>(handler: Handler<T>): () => void {
    return this.subscribe('/user/queue/notice', handler)
  }

  /** 订阅指定主题。 */
  onTopic<T = unknown>(topic: string, handler: Handler<T>): () => void {
    return this.subscribe(`/topic/${topic}`, handler)
  }

  /** 通用订阅，返回取消订阅的函数。 */
  subscribe<T = unknown>(destination: string, handler: Handler<T>): () => void {
    const record: SubscriptionRecord = {
      destination,
      handler: handler as Handler,
      stompSub: null
    }
    const list = this.subscriptions.get(destination) ?? []
    list.push(record)
    this.subscriptions.set(destination, list)
    if (this.client?.connected) this.bindStompSubscription(destination, record)
    return () => {
      record.stompSub?.unsubscribe()
      const records = this.subscriptions.get(destination)
      if (!records) return
      const remaining = records.filter((r) => r !== record)
      if (remaining.length === 0) this.subscriptions.delete(destination)
      else this.subscriptions.set(destination, remaining)
    }
  }

  private bindStompSubscription(destination: string, record: SubscriptionRecord) {
    if (!this.client?.connected) return
    record.stompSub = this.client.subscribe(destination, (frame: IMessage) => {
      try {
        const data = JSON.parse(frame.body) as PushMessage<unknown>
        record.handler(data)
      } catch (error) {
        console.warn('[ws] failed to parse frame for', destination, error)
      }
    })
  }

  private transitionTo(next: ConnectionStatus, frame?: IFrame) {
    this.status = next
    this.options.onStatusChange?.(next, frame)
  }

  private resolveBaseUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}`
  }
}

let singleton: WebSocketBus | null = null

export function getWebSocketBus(options?: WebSocketBusOptions): WebSocketBus {
  if (!singleton) singleton = new WebSocketBus(options)
  return singleton
}

export function resetWebSocketBus(): void {
  singleton?.disconnect()
  singleton = null
}
