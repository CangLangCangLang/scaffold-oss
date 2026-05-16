import { createI18n, type I18nOptions } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

const STORAGE_KEY = 'app.locale'

function detectLocale(): SupportedLocale {
  const stored = (typeof localStorage !== 'undefined' && localStorage.getItem(STORAGE_KEY)) as
    | SupportedLocale
    | null
  if (stored && SUPPORTED_LOCALES.includes(stored)) return stored
  if (typeof navigator !== 'undefined') {
    const lang = navigator.language || (navigator as unknown as { userLanguage?: string }).userLanguage || 'zh-CN'
    return lang.toLowerCase().startsWith('en') ? 'en-US' : 'zh-CN'
  }
  return 'zh-CN'
}

const options: I18nOptions = {
  legacy: false,
  globalInjection: true,
  locale: detectLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
}

const i18n = createI18n(options)

export function setLocale(locale: SupportedLocale) {
  const ref = i18n.global.locale as unknown as { value: SupportedLocale }
  ref.value = locale
  if (typeof localStorage !== 'undefined') localStorage.setItem(STORAGE_KEY, locale)
  if (typeof document !== 'undefined') document.documentElement.lang = locale
}

export function getLocale(): SupportedLocale {
  return (i18n.global.locale as unknown as { value: SupportedLocale }).value
}

export default i18n
