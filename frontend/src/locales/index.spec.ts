import { describe, expect, it } from 'vitest'
import i18n, { setLocale, getLocale } from '@/locales'

describe('i18n', () => {
  it('contains zh-CN messages', () => {
    expect(i18n.global.t('common.add', undefined, { locale: 'zh-CN' })).toBe('新增')
  })

  it('contains en-US messages', () => {
    expect(i18n.global.t('common.add', undefined, { locale: 'en-US' })).toBe('Add')
  })

  it('switches locale via setLocale', () => {
    setLocale('en-US')
    expect(getLocale()).toBe('en-US')
    setLocale('zh-CN')
    expect(getLocale()).toBe('zh-CN')
  })
})
