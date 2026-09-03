import type { ApiResult } from '@/types/api'
import type { PageResult, RoleSaveRequest, RoleVO } from '@/types/system'
import { request, unwrap } from './request'

export interface RolePageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: number
}

export function fetchRolePage(params: RolePageQuery) {
  return unwrap(request.get<ApiResult<PageResult<RoleVO>>>('/auth/roles', { params }))
}

export function fetchEnabledRoles() {
  return unwrap(request.get<ApiResult<RoleVO[]>>('/auth/roles/enabled'))
}

export function fetchRoleDetail(roleId: string) {
  return unwrap(request.get<ApiResult<RoleVO>>(`/auth/roles/${roleId}`))
}

export function createRole(data: RoleSaveRequest) {
  return unwrap(request.post<ApiResult<RoleVO>>('/auth/roles', data))
}

export function updateRole(roleId: string, data: RoleSaveRequest) {
  return unwrap(request.put<ApiResult<RoleVO>>(`/auth/roles/${roleId}`, data))
}

export function deleteRole(roleId: string) {
  return unwrap(request.delete<ApiResult<void>>(`/auth/roles/${roleId}`))
}

export function assignRoleMenus(roleId: string, menuIds: string[]) {
  return unwrap(request.put<ApiResult<void>>(`/auth/roles/${roleId}/menus`, { menuIds }))
}
