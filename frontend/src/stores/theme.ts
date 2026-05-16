import { defineStore } from 'pinia'
import { computed, ref, watchEffect } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'auto'

const STORAGE_KEY = 'app.theme'

function readStored(): ThemeMode {
  if (typeof localStorage === 'undefined') return 'light'
  const value = localStorage.getItem(STORAGE_KEY) as ThemeMode | null
  if (value === 'light' || value === 'dark' || value === 'auto') return value
  return 'light'
}

function prefersDark(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches
}

function applyHtmlClass(dark: boolean) {
  if (typeof document === 'undefined') return
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readStored())

  const isDark = computed(() => (mode.value === 'auto' ? prefersDark() : mode.value === 'dark'))

  function setMode(next: ThemeMode) {
    mode.value = next
    if (typeof localStorage !== 'undefined') localStorage.setItem(STORAGE_KEY, next)
  }

  function toggle() {
    setMode(isDark.value ? 'light' : 'dark')
  }

  watchEffect(() => {
    applyHtmlClass(isDark.value)
  })

  if (typeof window !== 'undefined') {
    window
      .matchMedia('(prefers-color-scheme: dark)')
      .addEventListener('change', () => {
        if (mode.value === 'auto') applyHtmlClass(prefersDark())
      })
  }

  return { mode, isDark, setMode, toggle }
})
