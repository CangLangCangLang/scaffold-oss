import i18n from '@/locales'

/**
 * 后端 BizCode#errorKey 与前端 i18n message-key 的映射。
 * 与 backend/scaffold-admin/src/main/resources/i18n/messages*.properties 中的同名键保持一致。
 */
export const ERROR_KEY_I18N: Record<string, string> = {
  BIZ_SUCCESS: 'errors.biz.success',
  BIZ_PARAM_INVALID: 'errors.biz.paramInvalid',
  BIZ_PARAM_MISSING: 'errors.biz.paramMissing',
  BIZ_UNAUTHORIZED: 'errors.unauthorized',
  BIZ_FORBIDDEN: 'errors.forbidden',
  BIZ_DEMO_MODE: 'errors.biz.demoMode',
  BIZ_RESOURCE_NOT_FOUND: 'errors.biz.notFound',
  BIZ_METHOD_NOT_ALLOWED: 'errors.biz.methodNotAllowed',
  BIZ_UNSUPPORTED_MEDIA: 'errors.biz.unsupportedMedia',
  BIZ_CONFLICT: 'errors.biz.conflict',
  BIZ_DUPLICATE_SUBMIT: 'errors.biz.duplicateSubmit',
  BIZ_RATE_LIMITED: 'errors.biz.rateLimited',
  BIZ_INTERNAL_ERROR: 'errors.server',
  BIZ_DEPENDENCY_UNAVAILABLE: 'errors.biz.dependencyUnavailable',
  BIZ_BUSINESS_ERROR: 'errors.biz.business'
}

/** 历史 HTTP/业务码兜底映射（保持向下兼容） */
export const ERROR_CODE_MAP: Record<string, string> = {
  '401': '认证失败，无法访问系统资源',
  '403': '当前操作没有权限',
  '404': '访问资源不存在',
  '500': '系统内部错误',
  '601': '系统警告消息',
  default: '系统未知错误，请反馈给管理员'
}

function tryTranslate(key?: string): string | undefined {
  if (!key) return undefined
  const messageKey = ERROR_KEY_I18N[key]
  if (!messageKey) return undefined
  if (!i18n.global.te(messageKey)) return undefined
  return i18n.global.t(messageKey)
}

/**
 * 解析错误展示文案：优先 errorKey -> i18n，其次后端 msg，最后回退码表。
 */
export function resolveErrorMessage(
  code: number | string | undefined | null,
  fallback?: string,
  errorKey?: string
): string {
  return (
    tryTranslate(errorKey) ||
    fallback ||
    (code === undefined || code === null ? ERROR_CODE_MAP.default : ERROR_CODE_MAP[String(code)]) ||
    ERROR_CODE_MAP.default
  )
}
