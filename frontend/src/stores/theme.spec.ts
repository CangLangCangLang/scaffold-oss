import { beforeEach, describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '@/stores/theme'

describe('useThemeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.documentElement.classList.remove('dark')
    document.documentElement.removeAttribute('data-theme')
    if (typeof localStorage !== 'undefined') localStorage.clear()
  })

  it('defaults to light mode', () => {
    const store = useThemeStore()
    expect(store.mode).toBe('light')
    expect(store.isDark).toBe(false)
  })

  it('toggles between light and dark', async () => {
    const store = useThemeStore()
    store.toggle()
    await nextTick()
    expect(store.mode).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    store.toggle()
    await nextTick()
    expect(store.mode).toBe('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('persists mode to localStorage', () => {
    const store = useThemeStore()
    store.setMode('dark')
    expect(localStorage.getItem('app.theme')).toBe('dark')
  })
})
