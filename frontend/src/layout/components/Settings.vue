<script setup lang="ts">
import { ElDrawer } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/stores/app'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
import { setLocale, type SupportedLocale } from '@/locales'

const appStore = useAppStore()
const themeStore = useThemeStore()
const { t, locale } = useI18n()

const themeOptions: Array<{ value: ThemeMode; key: string }> = [
  { value: 'light', key: 'layout.light' },
  { value: 'dark', key: 'layout.dark' },
  { value: 'auto', key: 'layout.auto' }
]

const localeOptions: Array<{ value: SupportedLocale; label: string }> = [
  { value: 'zh-CN', label: '简体中文' },
  { value: 'en-US', label: 'English' }
]

function changeLocale(value: SupportedLocale) {
  setLocale(value)
  locale.value = value
}
</script>

<template>
  <el-drawer
    v-model="appStore.settingsOpen"
    :title="t('layout.settings')"
    :size="320"
    direction="rtl"
  >
    <div class="settings">
      <div class="settings__section">
        <div class="settings__title">
          {{ t('layout.theme') }}
        </div>
        <el-radio-group
          :model-value="themeStore.mode"
          @update:model-value="(v: any) => themeStore.setMode(v as ThemeMode)"
        >
          <el-radio-button
            v-for="opt in themeOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ t(opt.key) }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="settings__section">
        <div class="settings__title">
          {{ t('layout.language') }}
        </div>
        <el-radio-group
          :model-value="(locale as string)"
          @update:model-value="(v: any) => changeLocale(v as SupportedLocale)"
        >
          <el-radio-button
            v-for="opt in localeOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="settings__section">
        <div class="settings__title">
          界面状态
        </div>
        <div class="settings__row">
          <span>侧边栏</span>
          <el-switch
            :model-value="!appStore.sidebarCollapsed"
            active-text="展开"
            inactive-text="收起"
            @update:model-value="(value: any) => appStore.setSidebar(!value)"
          />
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped lang="scss">
.settings {
  display: flex;
  flex-direction: column;
  gap: 18px;

  &__section {
    background: var(--el-fill-color-light);
    border-radius: 8px;
    padding: 14px 16px;
  }

  &__title {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin-bottom: 10px;
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    color: var(--el-text-color-primary);
  }
}
</style>
