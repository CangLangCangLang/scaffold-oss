<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import Sidebar from './components/Sidebar.vue'
import TopBar from './components/TopBar.vue'
import Breadcrumb from './components/Breadcrumb.vue'
import TagsView from './components/TagsView.vue'
import Settings from './components/Settings.vue'

const appStore = useAppStore()
const sidebarWidth = computed(() => (appStore.sidebarCollapsed ? '64px' : '220px'))
</script>

<template>
  <div class="layout">
    <aside
      class="layout__aside"
      :style="{ width: sidebarWidth }"
    >
      <div class="layout__brand">
        <div class="layout__logo" />
        <span
          v-if="!appStore.sidebarCollapsed"
          class="layout__title"
        >Scaffold</span>
      </div>
      <Sidebar />
    </aside>
    <section class="layout__main">
      <TopBar>
        <template #breadcrumb>
          <Breadcrumb />
        </template>
      </TopBar>
      <TagsView />
      <main class="layout__content">
        <router-view v-slot="{ Component, route }">
          <transition
            name="fade-slide"
            mode="out-in"
          >
            <component
              :is="Component"
              :key="route.fullPath"
            />
          </transition>
        </router-view>
      </main>
    </section>
    <Settings />
  </div>
</template>

<style scoped lang="scss">
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;

  &__aside {
    flex-shrink: 0;
    background: #1f2937;
    color: #d1d5db;
    transition: width 0.2s ease;
    display: flex;
    flex-direction: column;
  }

  &__brand {
    display: flex;
    align-items: center;
    gap: 10px;
    height: 56px;
    padding: 0 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }

  &__logo {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: linear-gradient(135deg, #3b82f6, #0ea5e9);
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: #fff;
    letter-spacing: 0.05em;
  }

  &__main {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  &__content {
    flex: 1;
    overflow: auto;
    background: #f5f7fa;
  }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
