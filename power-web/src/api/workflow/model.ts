import type { ApiResult } from '@/types/api'
import type {
  ModelSaveRequest,
  ModelVO,
  PageResult,
  ProcessDefinitionVO,
  WorkflowPageQuery,
} from '@/types/workflow'
import { request, unwrap } from '../request'

export interface ModelPageQuery extends WorkflowPageQuery {
  keyword?: string
}

export function fetchModelPage(params: ModelPageQuery) {
  return unwrap(request.get<ApiResult<PageResult<ModelVO>>>('/workflow/models', { params }))
}

export function fetchModelDetail(id: string) {
  return unwrap(request.get<ApiResult<ModelVO>>(`/workflow/models/${id}`))
}

export function saveModel(data: ModelSaveRequest) {
  return unwrap(request.post<ApiResult<ModelVO>>('/workflow/models', data))
}

export function deleteModel(id: string) {
  return unwrap(request.delete<ApiResult<void>>(`/workflow/models/${id}`))
}

export function deployModel(id: string) {
  return unwrap(request.post<ApiResult<ProcessDefinitionVO>>(`/workflow/models/${id}/deploy`))
}
