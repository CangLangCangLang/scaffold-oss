/**
 * 后端统一返回结构（com.scaffold.common.core.domain.AjaxResult / R<T>）。
 * code 为 200 表示成功；后端会附带 traceId 便于排查。
 */
export interface ApiResult<T = unknown> {
  code: number
  msg?: string
  data?: T
  traceId?: string
  /** 后端 BizCode#errorKey，例如 BIZ_PARAM_INVALID；前端基于此做稳定路由/提示 */
  errorKey?: string
  /** AjaxResult 模式下，data 字段会被打散到根对象上，因此保留索引签名兼容 */
  [key: string]: unknown
}

export interface PageResult<T = unknown> {
  total: number
  rows: T[]
  code: number
  msg?: string
}

export interface LoginInfo {
  token: string
}

export interface CaptchaInfo {
  captchaEnabled?: boolean
  uuid: string
  img: string
}

export interface UserInfo {
  user: {
    userId: number
    userName: string
    nickName: string
    avatar?: string
    [key: string]: unknown
  }
  roles: string[]
  permissions: string[]
  isDefaultModifyPwd?: boolean
  isPasswordExpired?: boolean
}

export interface RouterMeta {
  title?: string
  icon?: string
  noCache?: boolean
  link?: string | null
  affix?: boolean
}

export interface RouterRecord {
  name?: string
  path: string
  hidden?: boolean
  redirect?: string
  alwaysShow?: boolean
  component?: string
  meta?: RouterMeta
  children?: RouterRecord[]
}
