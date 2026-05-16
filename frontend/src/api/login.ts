import request from '@/utils/request'
import type { ApiResult, CaptchaInfo, LoginInfo, RouterRecord, UserInfo } from '@/types/api'

export interface LoginPayload {
  username: string
  password: string
  code: string
  uuid: string
}

export function login(payload: LoginPayload) {
  return request.post<ApiResult<LoginInfo>, ApiResult<LoginInfo>>('/login', payload, {
    isToken: false,
    repeatSubmit: false
  })
}

export function logout() {
  return request.post<ApiResult>('/logout')
}

export function getInfo() {
  return request.get<ApiResult<UserInfo>, ApiResult<UserInfo>>('/getInfo')
}

export function getCaptcha() {
  return request.get<ApiResult<CaptchaInfo>, ApiResult<CaptchaInfo>>('/captchaImage', {
    isToken: false,
    timeout: 20_000
  })
}

export function getRouters() {
  return request.get<ApiResult<RouterRecord[]>, ApiResult<RouterRecord[]>>('/getRouters')
}

export interface SsoProvider {
  id: string
  label: string
  icon?: string
  authorizationUri: string
}

export function listSsoProviders() {
  return request.get<ApiResult<SsoProvider[]>, ApiResult<SsoProvider[]>>('/sso/providers', {
    isToken: false
  })
}
