import type { ApiResult } from '@/types/api'
import type { PageResult, ProcessDefinitionVO, WorkflowPageQuery } from '@/types/workflow'
import { request, unwrap } from '../request'

export interface DefinitionPageQuery extends WorkflowPageQuery {
  suspended?: boolean
  category?: string
}

export function fetchDefinitionPage(params: DefinitionPageQuery) {
  return unwrap(
    request.get<ApiResult<PageResult<ProcessDefinitionVO>>>('/workflow/definitions', { params }),
  )
}

export function fetchStartableDefinitions(params: WorkflowPageQuery) {
  return unwrap(
    request.get<ApiResult<PageResult<ProcessDefinitionVO>>>('/workflow/definitions/startable', {
      params,
    }),
  )
}

export function fetchDefinitionXml(processDefinitionId: string) {
  return unwrap(
    request.get<ApiResult<string>>(`/workflow/definitions/${processDefinitionId}/xml`),
  )
}

export function deployDefinition(file: File) {
  const form = new FormData()
  form.append('file', file)
  // 不手动设置 Content-Type，让浏览器自动附带 multipart boundary
  return unwrap(request.post<ApiResult<ProcessDefinitionVO>>('/workflow/definitions/deploy', form))
}

export function suspendDefinition(processDefinitionId: string) {
  return unwrap(
    request.post<ApiResult<void>>(`/workflow/definitions/${processDefinitionId}/suspend`),
  )
}

export function activateDefinition(processDefinitionId: string) {
  return unwrap(
    request.post<ApiResult<void>>(`/workflow/definitions/${processDefinitionId}/activate`),
  )
}
