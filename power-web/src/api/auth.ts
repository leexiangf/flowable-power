import type { ApiResult, CurrentUser, LoginResponse, MenuVO, PasswordChangeRequest, ProfileUpdateRequest } from '@/types/api'
import { request, unwrap } from './request'

export function loginWeb(username: string, password: string) {
  return unwrap(
    request.post<ApiResult<LoginResponse>>('/auth/login/web', { username, password }),
  )
}

export function logout(accessToken?: string, refreshToken?: string) {
  return unwrap(
    request.post<ApiResult<void>>('/auth/logout', {
      accessToken: accessToken ?? undefined,
      refreshToken: refreshToken ?? undefined,
    }),
  )
}

export function fetchMe() {
  return unwrap(request.get<ApiResult<CurrentUser>>('/auth/me'))
}

export function updateProfile(data: ProfileUpdateRequest) {
  return unwrap(request.put<ApiResult<CurrentUser>>('/auth/me/profile', data))
}

export function changePassword(data: PasswordChangeRequest) {
  return unwrap(request.put<ApiResult<void>>('/auth/me/password', data))
}

export function fetchMenuTree() {
  return unwrap(request.get<ApiResult<MenuVO[]>>('/auth/menus/tree'))
}