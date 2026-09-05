import type { ApiResult } from '@/types/api'
import type {
  PageResult,
  TaskAddSignRequest,
  TaskCompleteRequest,
  TaskDelegateRequest,
  TaskRejectRequest,
  TaskTransferRequest,
  TaskVO,
  CcVO,
  UserTaskNodeVO,
  WorkflowPageQuery,
} from '@/types/workflow'
import { request, unwrap } from '../request'

export function fetchTodoTasks(params: WorkflowPageQuery) {
  return unwrap(request.get<ApiResult<PageResult<TaskVO>>>('/workflow/tasks/todo', { params }))
}

export function fetchDoneTasks(params: WorkflowPageQuery) {
  return unwrap(request.get<ApiResult<PageResult<TaskVO>>>('/workflow/tasks/done', { params }))
}

export function claimTask(taskId: string) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/claim`))
}

export function unclaimTask(taskId: string) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/unclaim`))
}

export function completeTask(taskId: string, data?: TaskCompleteRequest) {
  return unwrap(
    request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/complete`, data ?? {}),
  )
}

export function rejectTask(taskId: string, data?: TaskRejectRequest) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/reject`, data ?? {}))
}

export function transferTask(taskId: string, data: TaskTransferRequest) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/transfer`, data))
}

export function assignTask(taskId: string, data: TaskTransferRequest) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/assign`, data))
}

export function addSignTask(taskId: string, data: TaskAddSignRequest) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/add-sign`, data))
}

export function reduceSignTask(taskId: string) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/reduce-sign`))
}

export function fetchRejectableNodes(taskId: string) {
  return unwrap(
    request.get<ApiResult<UserTaskNodeVO[]>>(`/workflow/tasks/${taskId}/rejectable-nodes`),
  )
}

export function delegateTask(taskId: string, data: TaskDelegateRequest) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/delegate`, data))
}

export function resolveDelegateTask(taskId: string) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/${taskId}/resolve`))
}

export function fetchCcTasks(params: WorkflowPageQuery) {
  return unwrap(request.get<ApiResult<PageResult<CcVO>>>('/workflow/tasks/cc', { params }))
}

export function markCcRead(ccId: string) {
  return unwrap(request.post<ApiResult<void>>(`/workflow/tasks/cc/${ccId}/read`))
}
