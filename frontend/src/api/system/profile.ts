import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

export const getUserProfile = () =>
  request.get<ApiResult, ApiResult>('/system/user/profile')

export const updateUserProfile = (data: Record<string, unknown>) =>
  request.put<ApiResult, ApiResult>('/system/user/profile', data)

export const updateUserPwd = (oldPassword: string, newPassword: string) =>
  request.put<ApiResult, ApiResult>('/system/user/profile/updatePwd', null, {
    params: { oldPassword, newPassword }
  })

export const uploadAvatar = (data: FormData) =>
  request.post<ApiResult, ApiResult>('/system/user/profile/avatar', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
