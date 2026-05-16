<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Expand, Fold, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { listTopBarWidgets } from '@/layout/widgets'

const appStore = useAppStore()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const router = useRouter()

const collapsed = computed(() => appStore.sidebarCollapsed)
const displayName = computed(() => userStore.nickName || userStore.username || '匿名用户')
const widgets = computed(() => listTopBarWidgets())

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
    permissionStore.reset()
    router.replace('/login')
  } catch {
    // cancel
  }
}
</script>

<template>
  <header class="topbar">
    <el-icon
      class="topbar__action"
      :title="collapsed ? '展开菜单' : '收起菜单'"
      @click="appStore.toggleSidebar"
    >
      <component :is="collapsed ? Expand : Fold" />
    </el-icon>
    <div class="topbar__breadcrumb">
      <slot name="breadcrumb" />
    </div>
    <div class="topbar__actions">
      <component
        :is="widget.component"
        v-for="widget in widgets"
        :key="widget.key"
      />
      <el-icon
        class="topbar__action"
        title="设置"
        @click="appStore.openSettings"
      >
        <Setting />
      </el-icon>
      <el-dropdown trigger="click">
        <span class="topbar__user">
          <el-avatar :size="30">{{ displayName.charAt(0).toUpperCase() }}</el-avatar>
          <span class="topbar__user-name">{{ displayName }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped lang="scss">
.topbar {
  height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

  &__breadcrumb {
    flex: 1;
    margin: 0 12px;
    color: #6b7280;
    font-size: 14px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__action {
    cursor: pointer;
    font-size: 18px;
    color: #4b5563;

    &:hover {
      color: #2563eb;
    }
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
  }

  &__user-name {
    font-size: 14px;
    color: #1f2937;
  }
}

html.dark .topbar {
  background: var(--el-bg-color-overlay);
  border-bottom-color: var(--el-border-color-darker);
}
</style>
