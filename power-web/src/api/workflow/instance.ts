import type { ApiResult } from '@/types/api'
import type {
  ActivityTraceVO,
  PageResult,
  ProcessCancelRequest,
  ProcessHighlightVO,
  ProcessInstanceVO,
  ProcessStartRequest,
  ProcessTerminateRequest,
  ProcessUrgeRequest,
  TaskVO,
  WorkflowPageQuery,
} from '@/types/workflow'
import { request, unwrap } from '../request'

export interface InstanceMonitorQuery extends WorkflowPageQuery {
  processDefinitionKey?: string
  finished?: boolean
}

export function startProcessInstance(data: ProcessStartRequest) {
  return unwrap(request.post<ApiResult<ProcessInstanceVO>>('/workflow/instances/start', data))
}

export function fetchMyInstances(params: WorkflowPageQuery) {
  return unwrap(
    request.get<ApiResult<PageResult<ProcessInstanceVO>>>('/workflow/instances/mine', { params }),
  )
}

export function fetchInstanceMonitor(params: InstanceMonitorQuery) {
  return unwrap(
    request.get<ApiResult<PageResult<ProcessInstanceVO>>>('/workflow/instances', { params }),
  )
}

export function fetchInstanceDetail(processInstanceId: string) {
  return unwrap(
    request.get<ApiResult<ProcessInstanceVO>>(`/workflow/instances/${processInstanceId}`),
  )
}

export function fetchInstanceActiveTasks(processInstanceId: string) {
  return unwrap(
    request.get<ApiResult<TaskVO[]>>(`/workflow/instances/${processInstanceId}/tasks`),
  )
}

export function cancelProcessInstance(processInstanceId: string, data?: ProcessCancelRequest) {
  return unwrap(
    request.post<ApiResult<void>>(`/workflow/instances/${processInstanceId}/cancel`, data ?? {}),
  )
}

export function terminateProcessInstance(
  processInstanceId: string,
  data?: ProcessTerminateRequest,
) {
  return unwrap(
    request.post<ApiResult<void>>(
      `/workflow/instances/${processInstanceId}/terminate`,
      data ?? {},
    ),
  )
}

export function suspendProcessInstance(processInstanceId: string) {
  return unwrap(
    request.post<ApiResult<void>>(`/workflow/instances/${processInstanceId}/suspend`),
  )
}

export function activateProcessInstance(processInstanceId: string) {
  return unwrap(
    request.post<ApiResult<void>>(`/workflow/instances/${processInstanceId}/activate`),
  )
}

export function urgeProcessInstance(processInstanceId: string, data?: ProcessUrgeRequest) {
  return unwrap(
    request.post<ApiResult<void>>(`/workflow/instances/${processInstanceId}/urge`, data ?? {}),
  )
}

export function fetchInstanceTimeline(processInstanceId: string) {
  return unwrap(
    request.get<ApiResult<ActivityTraceVO[]>>(
      `/workflow/instances/${processInstanceId}/timeline`,
    ),
  )
}

export function fetchInstanceHighlight(processInstanceId: string) {
  return unwrap(
    request.get<ApiResult<ProcessHighlightVO>>(
      `/workflow/instances/${processInstanceId}/highlight`,
    ),
  )
}

/** 服务端生成的带高亮流程图 PNG；失败或非 PNG 时返回 null */
export async function fetchInstanceDiagram(processInstanceId: string): Promise<Blob | null> {
  try {
    const res = await request.get(`/workflow/instances/${processInstanceId}/diagram`, {
      responseType: 'blob',
    })
    const blob = res.data as Blob
    const contentType = String(res.headers['content-type'] || '')
    if (contentType.includes('image/png')) {
      return blob
    }
    return null
  } catch {
    return null
  }
}
