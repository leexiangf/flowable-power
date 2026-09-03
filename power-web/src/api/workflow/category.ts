import type { ApiResult } from '@/types/api'
import type {
  CategorySaveRequest,
  CategoryVO,
  PageResult,
  WorkflowPageQuery,
} from '@/types/workflow'
import { request, unwrap } from '../request'

export interface CategoryPageQuery extends WorkflowPageQuery {
  keyword?: string
}

export function fetchCategoryPage(params: CategoryPageQuery) {
  return unwrap(request.get<ApiResult<PageResult<CategoryVO>>>('/workflow/categories', { params }))
}

export function fetchEnabledCategories() {
  return unwrap(request.get<ApiResult<CategoryVO[]>>('/workflow/categories/enabled'))
}

export function fetchCategoryDetail(id: string) {
  return unwrap(request.get<ApiResult<CategoryVO>>(`/workflow/categories/${id}`))
}

export function createCategory(data: CategorySaveRequest) {
  return unwrap(request.post<ApiResult<CategoryVO>>('/workflow/categories', data))
}

export function updateCategory(id: string, data: CategorySaveRequest) {
  return unwrap(request.put<ApiResult<CategoryVO>>(`/workflow/categories/${id}`, data))
}

export function deleteCategory(id: string) {
  return unwrap(request.delete<ApiResult<void>>(`/workflow/categories/${id}`))
}
