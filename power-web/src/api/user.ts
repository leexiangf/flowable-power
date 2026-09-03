import type { ApiResult } from '@/types/api'
import type { PageResult, UserSaveRequest, UserVO } from '@/types/system'
import { request, unwrap } from './request'

export interface UserPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: number
}

export function fetchUserPage(params: UserPageQuery) {
  return unwrap(request.get<ApiResult<PageResult<UserVO>>>('/auth/users', { params }))
}

export function fetchUserDetail(userId: string) {
  return unwrap(request.get<ApiResult<UserVO>>(`/auth/users/${userId}`))
}

export function createUser(data: UserSaveRequest) {
  return unwrap(request.post<ApiResult<UserVO>>('/auth/users', data))
}

export function updateUser(userId: string, data: UserSaveRequest) {
  return unwrap(request.put<ApiResult<UserVO>>(`/auth/users/${userId}`, data))
}

export function deleteUser(userId: string) {
  return unwrap(request.delete<ApiResult<void>>(`/auth/users/${userId}`))
}

export function disableUser(userId: string) {
  return unwrap(request.post<ApiResult<void>>(`/auth/users/${userId}/disable`))
}

export function enableUser(userId: string) {
  return unwrap(request.post<ApiResult<void>>(`/auth/users/${userId}/enable`))
}
