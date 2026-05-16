<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import {
  ElButton,
  ElEmpty,
  ElInput,
  ElMessage,
  ElMessageBox
} from 'element-plus'
import { Refresh, Delete } from '@element-plus/icons-vue'
import {
  listCacheName,
  listCacheKey,
  getCacheValue,
  clearCacheName,
  clearCacheKey,
  clearCacheAll,
  type CacheNameItem
} from '@/api/monitor/cache'

const cacheNames = ref<CacheNameItem[]>([])
const keys = ref<string[]>([])
const activeName = ref<CacheNameItem>()
const activeKey = ref('')
const detail = ref<CacheNameItem>({ cacheName: '' })
const search = ref('')

async function loadNames() {
  const res = (await listCacheName()) as { data?: CacheNameItem[] }
  cacheNames.value = res.data ?? []
}

async function loadKeys(item: CacheNameItem) {
  activeName.value = item
  const res = (await listCacheKey(item.cacheName)) as { data?: string[] }
  keys.value = res.data ?? []
  activeKey.value = ''
  detail.value = { cacheName: item.cacheName }
}

async function loadValue(key: string) {
  if (!activeName.value) return
  activeKey.value = key
  const res = (await getCacheValue(activeName.value.cacheName, key)) as { data?: CacheNameItem }
  detail.value = res.data ?? { cacheName: activeName.value.cacheName, cacheKey: key }
}

async function handleClearAll() {
  try {
    await ElMessageBox.confirm('确认清理所有缓存？', '提示', { type: 'warning' })
    await clearCacheAll()
    ElMessage.success('已清理')
    keys.value = []
    detail.value = { cacheName: activeName.value?.cacheName ?? '' }
  } catch {
    // cancel
  }
}

async function handleClearName() {
  if (!activeName.value) return
  await clearCacheName(activeName.value.cacheName)
  ElMessage.success('已清理')
  await loadKeys(activeName.value)
}

async function handleClearKey() {
  if (!activeKey.value) return
  await clearCacheKey(activeKey.value)
  ElMessage.success('已清理')
  if (activeName.value) await loadKeys(activeName.value)
}

const filteredKeys = ref<string[]>([])

watch([keys, search], () => {
  if (!search.value) {
    filteredKeys.value = keys.value
  } else {
    filteredKeys.value = keys.value.filter((k) => k.includes(search.value))
  }
})

onMounted(loadNames)
</script>

<template>
  <div class="scaffold-page cache-list">
    <aside class="scaffold-card cache-list__pane">
      <div class="cache-list__pane-header">
        缓存列表
        <ElButton
          :icon="Refresh"
          link
          @click="loadNames"
        />
      </div>
      <ul class="cache-list__items">
        <li
          v-for="item in cacheNames"
          :key="item.cacheName"
          class="cache-list__item"
          :class="{ 'is-active': activeName?.cacheName === item.cacheName }"
          @click="loadKeys(item)"
        >
          <div>{{ item.cacheName }}</div>
          <small>{{ item.remark }}</small>
        </li>
      </ul>
    </aside>

    <aside class="scaffold-card cache-list__pane">
      <div class="cache-list__pane-header">
        键名列表
        <div>
          <ElButton
            :icon="Delete"
            link
            type="danger"
            @click="handleClearName"
          >
            清空
          </ElButton>
        </div>
      </div>
      <ElInput
        v-model="search"
        placeholder="搜索键名"
        clearable
        size="small"
        class="cache-list__search"
      />
      <ul class="cache-list__items">
        <li
          v-for="key in filteredKeys"
          :key="key"
          class="cache-list__item"
          :class="{ 'is-active': activeKey === key }"
          @click="loadValue(key)"
        >
          {{ key }}
        </li>
        <ElEmpty
          v-if="!filteredKeys.length"
          description="无键值"
          :image-size="80"
        />
      </ul>
    </aside>

    <section class="scaffold-card cache-list__detail">
      <div class="cache-list__pane-header">
        缓存内容
        <ElButton
          type="danger"
          :icon="Delete"
          :disabled="!activeKey"
          @click="handleClearKey"
        >
          删除当前键
        </ElButton>
      </div>
      <div class="cache-list__detail-grid">
        <div><label>缓存名</label><span>{{ detail.cacheName }}</span></div>
        <div><label>键名</label><span>{{ detail.cacheKey || '-' }}</span></div>
        <div class="cache-list__detail-value">
          <label>值</label><pre>{{ detail.cacheValue || '请选择左侧键名' }}</pre>
        </div>
      </div>
      <div class="cache-list__footer">
        <ElButton
          type="warning"
          plain
          @click="handleClearAll"
        >
          清空所有缓存
        </ElButton>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.cache-list {
  display: grid;
  grid-template-columns: 280px 280px 1fr;
  gap: 16px;

  &__pane {
    display: flex;
    flex-direction: column;
    height: calc(100vh - 56px - 38px - 32px);
    overflow: hidden;
  }

  &__pane-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
    color: #1f2937;
    padding-bottom: 8px;
    border-bottom: 1px solid #e5e7eb;
    margin-bottom: 8px;
  }

  &__search {
    margin-bottom: 8px;
  }

  &__items {
    list-style: none;
    padding: 0;
    margin: 0;
    overflow-y: auto;
    flex: 1;
  }

  &__item {
    padding: 8px 10px;
    border-radius: 6px;
    cursor: pointer;
    color: #374151;
    transition: background 0.15s;

    &:hover {
      background: #f3f4f6;
    }

    &.is-active {
      background: #dbeafe;
      color: #1d4ed8;
    }

    small {
      display: block;
      color: #9ca3af;
      font-size: 12px;
    }
  }

  &__detail {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__detail-grid {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;

    label {
      display: inline-block;
      width: 60px;
      color: #6b7280;
      font-size: 13px;
    }
  }

  &__detail-value {
    flex: 1;

    pre {
      margin: 8px 0 0;
      padding: 12px;
      background: #f3f4f6;
      border-radius: 6px;
      max-height: 380px;
      overflow: auto;
      white-space: pre-wrap;
      word-break: break-all;
      font-family: 'JetBrains Mono', Consolas, monospace;
      font-size: 12px;
    }
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
