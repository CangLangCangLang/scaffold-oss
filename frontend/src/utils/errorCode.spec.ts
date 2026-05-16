import { describe, expect, it } from 'vitest'
import i18n, { setLocale } from '@/locales'
import { resolveErrorMessage, ERROR_CODE_MAP } from '@/utils/errorCode'

describe('resolveErrorMessage', () => {
  it('returns mapped message for known code', () => {
    expect(resolveErrorMessage(401)).toBe(ERROR_CODE_MAP['401'])
    expect(resolveErrorMessage('403')).toBe(ERROR_CODE_MAP['403'])
  })

  it('falls back to default when code is unknown', () => {
    expect(resolveErrorMessage(9999)).toBe(ERROR_CODE_MAP.default)
  })

  it('uses provided fallback when code is null/undefined', () => {
    expect(resolveErrorMessage(undefined, 'oops')).toBe('oops')
    expect(resolveErrorMessage(null as unknown as number)).toBe(ERROR_CODE_MAP.default)
  })

  it('prefers errorKey -> i18n message', () => {
    setLocale('zh-CN')
    const zh = resolveErrorMessage(429, 'fallback', 'BIZ_RATE_LIMITED')
    expect(zh).toBe(i18n.global.t('errors.biz.rateLimited'))

    setLocale('en-US')
    const en = resolveErrorMessage(429, 'fallback', 'BIZ_RATE_LIMITED')
    expect(en).toBe(i18n.global.t('errors.biz.rateLimited'))

    setLocale('zh-CN')
  })

  it('falls back to msg when errorKey is unknown', () => {
    expect(resolveErrorMessage(500, 'backend says fail', 'BIZ_UNKNOWN_KEY')).toBe('backend says fail')
  })
})
