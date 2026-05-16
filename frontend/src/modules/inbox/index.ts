/**
 * inbox 模块（前端）：通知中心 + 与后端 sys_message_inbox 表的拉取/ACK 协议。
 * <p>
 * 删除整个目录即下线：
 * <ul>
 *   <li>main.ts 通过 import.meta.glob 自动发现/移除模块</li>
 *   <li>TopBar 上的铃铛通过 widget 注入机制集成，模块缺失时不渲染</li>
 *   <li>路由 /system/message/inbox 由模块自注入；模块下线时路由也跟着消失</li>
 *   <li>{@code useNotificationStore} 也跟着消失，不会有死代码引用</li>
 * </ul>
 */
import { markRaw } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import type { ScaffoldFrontendModule } from '../loader'
import { registerTopBarWidget } from '@/layout/widgets'
import NotificationBell from './components/NotificationBell.vue'
import { useNotificationStore } from './store'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: 'system/message/inbox',
    name: 'InboxList',
    component: () => import('./views/InboxList.vue'),
    meta: { title: '我的消息' }
  }
]

const inboxModule: ScaffoldFrontendModule = {
  name: 'inbox',
  routes,
  locales: {
    'zh-CN': {
      inbox: {
        title: '我的消息',
        empty: '暂无消息',
        full: {
          search: {
            type: '消息类型',
            typePlaceholder: '如 cms.article',
            status: '状态',
            range: '时间范围'
          },
          status: {
            unread: '未读',
            read: '已读',
            expired: '已过期'
          },
          column: {
            id: '编号',
            type: '消息类型',
            content: '内容摘要',
            time: '时间'
          },
          batch: {
            ackBtn: '批量已读',
            confirmDelete: '确定删除选中的 {count} 条消息？删除后无法恢复。',
            confirmDeleteOne: '确定删除这条消息？',
            ackedCount: '已标记 {count} 条为已读',
            deletedCount: '已删除 {count} 条',
            ackOneOk: '已标记为已读',
            deletedOne: '已删除',
            deleteTitle: '批量删除',
            deleteOneTitle: '删除',
            deleteAction: '删除'
          },
          row: {
            ackBtn: '已读'
          },
          detail: {
            title: '消息详情',
            messageId: '消息 ID',
            scope: '作用域',
            target: '对象',
            type: '消息类型',
            createdAt: '创建时间',
            readAt: '读取时间',
            expireAt: '过期时间',
            payload: 'Payload'
          },
          bell: {
            viewAll: '查看全部消息'
          }
        }
      }
    },
    'en-US': {
      inbox: {
        title: 'My Messages',
        empty: 'No messages',
        full: {
          search: {
            type: 'Message type',
            typePlaceholder: 'e.g. cms.article',
            status: 'Status',
            range: 'Time range'
          },
          status: {
            unread: 'Unread',
            read: 'Read',
            expired: 'Expired'
          },
          column: {
            id: 'ID',
            type: 'Type',
            content: 'Content',
            time: 'Time'
          },
          batch: {
            ackBtn: 'Mark as read',
            confirmDelete: 'Delete the selected {count} messages? This cannot be undone.',
            confirmDeleteOne: 'Delete this message?',
            ackedCount: 'Marked {count} message(s) as read',
            deletedCount: 'Deleted {count} message(s)',
            ackOneOk: 'Marked as read',
            deletedOne: 'Deleted',
            deleteTitle: 'Batch delete',
            deleteOneTitle: 'Delete',
            deleteAction: 'Delete'
          },
          row: {
            ackBtn: 'Read'
          },
          detail: {
            title: 'Message detail',
            messageId: 'Message ID',
            scope: 'Scope',
            target: 'Target',
            type: 'Type',
            createdAt: 'Created at',
            readAt: 'Read at',
            expireAt: 'Expires at',
            payload: 'Payload'
          },
          bell: {
            viewAll: 'View all messages'
          }
        }
      }
    }
  },
  install(ctx) {
    // 1. 顶部栏注入铃铛
    registerTopBarWidget({
      key: 'inbox.bell',
      component: markRaw(NotificationBell),
      order: 10
    })

    // 2. 监听全局退出事件：用户登出后清理 store
    //    通过 router.beforeEach 在导航到 /login 时主动断开。
    ctx.router.beforeEach((to, _from, next) => {
      if (to.path === '/login') {
        try {
          const store = useNotificationStore()
          store.disconnect()
        } catch {
          // pinia 未就绪时忽略
        }
      }
      next()
    })

    // 3. 防止应用启动时立即报错——不在 install 阶段访问 store（pinia 还没装好）
    void useUserStore
  }
}

export default inboxModule
export { useNotificationStore } from './store'
export type { NotificationItem } from './store'
