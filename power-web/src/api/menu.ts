import type { ApiResult } from '@/types/api'
import type { MenuDetailVO, MenuSaveRequest } from '@/types/system'
import { request, unwrap } from './request'

export interface MenuListQuery {
  keyword?: string
  status?: number
}

export function fetchAdminMenuTree() {
  return unwrap(request.get<ApiResult<MenuDetailVO[]>>('/auth/menus/tree/all'))
}

export function fetchMenuList(params?: MenuListQuery) {
  return unwrap(request.get<ApiResult<MenuDetailVO[]>>('/auth/menus', { params }))
}

export function fetchMenuDetail(menuId: string) {
  return unwrap(request.get<ApiResult<MenuDetailVO>>(`/auth/menus/${menuId}`))
}

export function createMenu(data: MenuSaveRequest) {
  return unwrap(request.post<ApiResult<MenuDetailVO>>('/auth/menus', data))
}

export function updateMenu(menuId: string, data: MenuSaveRequest) {
  return unwrap(request.put<ApiResult<MenuDetailVO>>(`/auth/menus/${menuId}`, data))
}

export function deleteMenu(menuId: string) {
  return unwrap(request.delete<ApiResult<void>>(`/auth/menus/${menuId}`))
}
